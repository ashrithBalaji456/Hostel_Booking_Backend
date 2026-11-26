package com.accommodation.platform.booking.service;

import com.accommodation.platform.availability.repository.AvailabilityBlockRepository;
import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.entity.BookingStatusHistory;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.booking.repository.BookingStatusHistoryRepository;
import com.accommodation.platform.booking.service.PricingCalculationService.PriceBreakdown;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.NotificationType;
import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.rent.service.RentService;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BookingModificationService {

    private final BookingRepository bookingRepository;
    private final BedRepository bedRepository;
    private final AvailabilityBlockRepository blockRepository;
    private final AvailabilityService availabilityService;
    private final PricingCalculationService pricingService;
    private final BookingService bookingService;
    private final BookingStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final RentService rentService;

    public BookingModificationService(
            BookingRepository bookingRepository,
            BedRepository bedRepository,
            AvailabilityBlockRepository blockRepository,
            AvailabilityService availabilityService,
            PricingCalculationService pricingService,
            BookingService bookingService,
            BookingStatusHistoryRepository historyRepository,
            NotificationService notificationService,
            RentService rentService
    ) {
        this.bookingRepository = bookingRepository;
        this.bedRepository = bedRepository;
        this.blockRepository = blockRepository;
        this.availabilityService = availabilityService;
        this.pricingService = pricingService;
        this.bookingService = bookingService;
        this.historyRepository = historyRepository;
        this.notificationService = notificationService;
        this.rentService = rentService;
    }

    @Transactional
    public BookingResponse extendBooking(UUID userId, UUID bookingId, ExtendBookingRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only extend your own booking");
        }
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED && booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only active bookings can be extended", "INVALID_BOOKING_STATE");
        }
        if (!req.newCheckOut().isAfter(booking.getCheckOut())) {
            throw new BadRequestException("New check-out must be after current check-out", "INVALID_DATES");
        }

        // Lock the bed and check [currentCheckOut, newCheckOut)
        Bed bed = bedRepository.findByIdForUpdate(booking.getBed().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));

        if (bookingRepository.existsOverlappingBooking(bed.getId(), booking.getCheckOut(), req.newCheckOut(), Instant.now())
                || blockRepository.existsOverlappingBlock(bed.getId(), booking.getCheckOut(), req.newCheckOut())) {
            throw new ConflictException("Bed is not available for the requested extension period", "BED_UNAVAILABLE");
        }

        PriceBreakdown newBreakdown = pricingService.calculatePrice(
                booking.getProperty(), bed.getRoom(), booking.getStayType(), booking.getCheckIn(), req.newCheckOut()
        );

        booking.setCheckOut(req.newCheckOut());
        booking.setBaseAmount(newBreakdown.baseAmount());
        booking.setDiscountAmount(newBreakdown.discountAmount());
        booking.setExtraCharges(newBreakdown.extraCharges());
        booking.setTotalAmount(newBreakdown.totalAmount());
        booking = bookingRepository.save(booking);
        rentService.syncScheduleForExtension(booking);

        historyRepository.save(new BookingStatusHistory(
                booking.getId(), booking.getBookingStatus(), booking.getBookingStatus(), userId,
                "Extended check-out to " + req.newCheckOut()
        ));
        notificationService.notify(
                booking.getProperty().getOwner(),
                "Booking Extended",
                "Booking " + booking.getBookingReference() + " was extended to " + req.newCheckOut(),
                NotificationType.BOOKING_CONFIRMED
        );
        return bookingService.toResponse(booking);
    }

    @Transactional
    public BookingResponse modifyBooking(UUID actorId, UserRole role, UUID bookingId, ModifyBookingRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (role != UserRole.ADMIN && !booking.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("Access denied");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking cannot be modified in current status", "INVALID_BOOKING_STATE");
        }

        availabilityService.validateTimeWindow(req.newCheckIn(), req.newCheckOut());
        UUID targetBedId = req.newBedId() != null ? req.newBedId() : booking.getBed().getId();
        Bed targetBed = bedRepository.findByIdForUpdate(targetBedId)
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));

        if (!targetBed.getRoom().getProperty().getId().equals(booking.getProperty().getId())) {
            throw new BadRequestException("Target bed must belong to the same property", "INVALID_BED_PROPERTY");
        }

        // Temporarily mark current booking CANCELLED in memory? No — check overlapping excluding self by checking if bed changed or dates expanded
        if (!targetBedId.equals(booking.getBed().getId())) {
            if (bookingRepository.existsOverlappingBooking(targetBedId, req.newCheckIn(), req.newCheckOut(), Instant.now())
                    || blockRepository.existsOverlappingBlock(targetBedId, req.newCheckIn(), req.newCheckOut())) {
                throw new ConflictException("Selected bed is unavailable for the new dates", "BED_UNAVAILABLE");
            }
        } else {
            if (blockRepository.existsOverlappingBlock(targetBedId, req.newCheckIn(), req.newCheckOut())) {
                throw new ConflictException("Selected bed is blocked for the new dates", "BED_UNAVAILABLE");
            }
        }

        PriceBreakdown newBreakdown = pricingService.calculatePrice(
                booking.getProperty(), targetBed.getRoom(), booking.getStayType(), req.newCheckIn(), req.newCheckOut()
        );

        booking.setBed(targetBed);
        booking.setCheckIn(req.newCheckIn());
        booking.setCheckOut(req.newCheckOut());
        booking.setBaseAmount(newBreakdown.baseAmount());
        booking.setDiscountAmount(newBreakdown.discountAmount());
        booking.setExtraCharges(newBreakdown.extraCharges());
        booking.setTotalAmount(newBreakdown.totalAmount());
        booking = bookingRepository.save(booking);
        rentService.syncScheduleForExtension(booking);

        historyRepository.save(new BookingStatusHistory(
                booking.getId(), booking.getBookingStatus(), booking.getBookingStatus(), actorId,
                "Modified dates/bed"
        ));
        return bookingService.toResponse(booking);
    }

    @Transactional
    public BookingResponse requestTransitionToLongStay(UUID userId, UUID bookingId, Instant newCheckOut) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        if (booking.getStayType() == StayType.LONG_STAY) {
            throw new BadRequestException("Booking is already a long stay", "ALREADY_LONG_STAY");
        }
        if (!newCheckOut.isAfter(booking.getCheckOut())) {
            throw new BadRequestException("New check-out must be after current check-out", "INVALID_DATES");
        }

        Bed bed = bedRepository.findByIdForUpdate(booking.getBed().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));
        if (bookingRepository.existsOverlappingBooking(bed.getId(), booking.getCheckOut(), newCheckOut, Instant.now())) {
            throw new ConflictException("Bed is not available for long-stay transition", "BED_UNAVAILABLE");
        }

        PriceBreakdown breakdown = pricingService.calculatePrice(
                booking.getProperty(), bed.getRoom(), StayType.LONG_STAY, booking.getCheckIn(), newCheckOut
        );

        BookingStatus prevStatus = booking.getBookingStatus();
        booking.setStayType(StayType.LONG_STAY);
        booking.setCheckOut(newCheckOut);
        booking.setApprovalRequired(true);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setBaseAmount(breakdown.baseAmount());
        booking.setDepositAmount(breakdown.depositAmount());
        booking.setDiscountAmount(breakdown.discountAmount());
        booking.setTotalAmount(breakdown.totalAmount());
        booking = bookingRepository.save(booking);

        historyRepository.save(new BookingStatusHistory(
                booking.getId(), prevStatus, BookingStatus.PENDING, userId,
                "Requested transition to LONG_STAY (awaiting owner confirmation)"
        ));
        notificationService.notify(
                booking.getProperty().getOwner(),
                "Long-Stay Transition Request",
                "Customer requested converting booking " + booking.getBookingReference() + " to LONG_STAY.",
                NotificationType.BOOKING_CREATED
        );
        return bookingService.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingStatusHistory> getHistory(UUID bookingId) {
        return historyRepository.findByBookingIdOrderByCreatedAtAsc(bookingId);
    }

    public record ExtendBookingRequest(@NotNull Instant newCheckOut) {}

    public record ModifyBookingRequest(
            @NotNull Instant newCheckIn,
            @NotNull Instant newCheckOut,
            UUID newBedId
    ) {}
}
