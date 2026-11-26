package com.accommodation.platform.booking.service;

import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.entity.BedChangeRequest;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.entity.BookingStatusHistory;
import com.accommodation.platform.booking.repository.BedChangeRequestRepository;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.booking.repository.BookingStatusHistoryRepository;
import com.accommodation.platform.common.enums.BedStatus;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.NotificationType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BedChangeService {

    public record BedChangeRequestResponse(
            UUID id,
            UUID bookingId,
            String bookingReference,
            UUID propertyId,
            String propertyName,
            UUID customerId,
            String customerName,
            UUID currentBedId,
            String currentRoomNumber,
            String currentBedNumber,
            UUID requestedBedId,
            String requestedRoomNumber,
            String requestedBedNumber,
            String reason,
            String status,
            String ownerNote,
            Instant createdAt,
            Instant resolvedAt
    ) {}

    private final BedChangeRequestRepository requestRepository;
    private final BookingRepository bookingRepository;
    private final BedRepository bedRepository;
    private final AvailabilityService availabilityService;
    private final BookingStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;

    public BedChangeService(
            BedChangeRequestRepository requestRepository,
            BookingRepository bookingRepository,
            BedRepository bedRepository,
            AvailabilityService availabilityService,
            BookingStatusHistoryRepository historyRepository,
            NotificationService notificationService
    ) {
        this.requestRepository = requestRepository;
        this.bookingRepository = bookingRepository;
        this.bedRepository = bedRepository;
        this.availabilityService = availabilityService;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BedChangeRequestResponse requestBedChange(UUID customerId, UUID bookingId, UUID requestedBedId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can only request a bed change for your own booking");
        }
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Bed change is only available for confirmed bookings", "INVALID_BOOKING_STATE");
        }
        if (booking.getBed().getId().equals(requestedBedId)) {
            throw new BadRequestException("You are already assigned to this bed", "SAME_BED");
        }
        if (requestRepository.existsByBookingIdAndStatus(bookingId, "PENDING")) {
            throw new ConflictException("You already have a pending bed change request for this booking", "PENDING_REQUEST_EXISTS");
        }

        Bed requestedBed = bedRepository.findById(requestedBedId)
                .orElseThrow(() -> new ResourceNotFoundException("Requested bed not found", "BED_NOT_FOUND"));
        if (!requestedBed.getRoom().getProperty().getId().equals(booking.getProperty().getId())) {
            throw new BadRequestException("Requested bed must belong to the same property", "DIFFERENT_PROPERTY");
        }
        if (requestedBed.getStatus() != BedStatus.AVAILABLE) {
            throw new ConflictException("Requested bed is not available", "BED_UNAVAILABLE");
        }

        Instant now = Instant.now();
        Instant windowStart = now.isAfter(booking.getCheckIn()) ? now : booking.getCheckIn();
        if (!availabilityService.isBedAvailable(requestedBedId, windowStart, booking.getCheckOut())) {
            throw new ConflictException("Requested bed is already occupied for your stay window", "BED_OCCUPIED");
        }

        BedChangeRequest req = new BedChangeRequest();
        req.setBooking(booking);
        req.setProperty(booking.getProperty());
        req.setCustomer(booking.getUser());
        req.setCurrentBed(booking.getBed());
        req.setRequestedBed(requestedBed);
        req.setReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        req.setStatus("PENDING");
        req = requestRepository.save(req);

        notificationService.notify(
                booking.getProperty().getOwner(),
                "Room / Bed Change Request",
                booking.getGuestName() + " requested to move from Room "
                        + booking.getBed().getRoom().getRoomNumber() + " Bed " + booking.getBed().getBedNumber()
                        + " to Room " + requestedBed.getRoom().getRoomNumber() + " Bed " + requestedBed.getBedNumber(),
                NotificationType.BOOKING_CONFIRMED
        );

        return toResponse(req);
    }

    @Transactional(readOnly = true)
    public List<BedChangeRequestResponse> getCustomerRequests(UUID customerId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("Access denied");
        }
        return requestRepository.findDetailedByBookingId(bookingId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BedChangeRequestResponse> getOwnerRequests(UUID ownerId) {
        return requestRepository.findDetailedByOwnerId(ownerId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BedChangeRequestResponse approveRequest(UUID ownerId, UUID requestId, String ownerNote) {
        BedChangeRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed change request not found", "REQUEST_NOT_FOUND"));
        if (!req.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new BadRequestException("Request has already been resolved", "ALREADY_RESOLVED");
        }

        Booking booking = req.getBooking();
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can swap beds", "INVALID_BOOKING_STATE");
        }

        Bed requestedBed = bedRepository.findByIdForUpdate(req.getRequestedBed().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Requested bed not found", "BED_NOT_FOUND"));
        if (requestedBed.getStatus() != BedStatus.AVAILABLE) {
            throw new ConflictException("Requested bed is no longer available", "BED_UNAVAILABLE");
        }

        Instant now = Instant.now();
        Instant windowStart = now.isAfter(booking.getCheckIn()) ? now : booking.getCheckIn();
        if (!availabilityService.isBedAvailable(requestedBed.getId(), windowStart, booking.getCheckOut())) {
            throw new ConflictException("Requested bed is already occupied for this stay window", "BED_OCCUPIED");
        }

        Bed oldBed = booking.getBed();
        booking.setBed(requestedBed);
        bookingRepository.save(booking);

        historyRepository.save(new BookingStatusHistory(
                booking.getId(),
                booking.getBookingStatus(),
                booking.getBookingStatus(),
                ownerId,
                "Approved bed change from Room " + oldBed.getRoom().getRoomNumber() + " Bed " + oldBed.getBedNumber()
                        + " to Room " + requestedBed.getRoom().getRoomNumber() + " Bed " + requestedBed.getBedNumber()
        ));

        req.setStatus("APPROVED");
        req.setOwnerNote(ownerNote != null && !ownerNote.isBlank() ? ownerNote.trim() : "Approved");
        req.setResolvedAt(Instant.now());
        req = requestRepository.save(req);

        notificationService.notify(
                booking.getUser(),
                "Room / Bed Change Approved",
                "Your bed change to Room " + requestedBed.getRoom().getRoomNumber()
                        + " · Bed " + requestedBed.getBedNumber() + " has been approved.",
                NotificationType.BOOKING_CONFIRMED
        );

        return toResponse(req);
    }

    @Transactional
    public BedChangeRequestResponse rejectRequest(UUID ownerId, UUID requestId, String ownerNote) {
        BedChangeRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed change request not found", "REQUEST_NOT_FOUND"));
        if (!req.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (!"PENDING".equals(req.getStatus())) {
            throw new BadRequestException("Request has already been resolved", "ALREADY_RESOLVED");
        }

        req.setStatus("REJECTED");
        req.setOwnerNote(ownerNote != null && !ownerNote.isBlank() ? ownerNote.trim() : "Declined by owner");
        req.setResolvedAt(Instant.now());
        req = requestRepository.save(req);

        notificationService.notify(
                req.getBooking().getUser(),
                "Room / Bed Change Declined",
                "Your bed change request was declined" + (req.getOwnerNote() != null ? ": " + req.getOwnerNote() : "."),
                NotificationType.BOOKING_CONFIRMED
        );

        return toResponse(req);
    }

    private BedChangeRequestResponse toResponse(BedChangeRequest r) {
        return new BedChangeRequestResponse(
                r.getId(),
                r.getBooking().getId(),
                r.getBooking().getBookingReference(),
                r.getProperty().getId(),
                r.getProperty().getName(),
                r.getCustomer().getId(),
                r.getCustomer().getFullName(),
                r.getCurrentBed().getId(),
                r.getCurrentBed().getRoom().getRoomNumber(),
                r.getCurrentBed().getBedNumber(),
                r.getRequestedBed().getId(),
                r.getRequestedBed().getRoom().getRoomNumber(),
                r.getRequestedBed().getBedNumber(),
                r.getReason(),
                r.getStatus(),
                r.getOwnerNote(),
                r.getCreatedAt(),
                r.getResolvedAt()
        );
    }
}
