package com.accommodation.platform.booking.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.bed.entity.Bed;
import com.accommodation.platform.bed.repository.BedRepository;
import com.accommodation.platform.booking.dto.BookingDtos.*;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.booking.service.PricingCalculationService.PriceBreakdown;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.common.util.ReferenceGenerator;
import com.accommodation.platform.config.AppProperties;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BedRepository bedRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;
    private final PricingCalculationService pricingService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final AppProperties appProperties;

    public BookingService(
            BookingRepository bookingRepository,
            BedRepository bedRepository,
            UserRepository userRepository,
            AvailabilityService availabilityService,
            PricingCalculationService pricingService,
            NotificationService notificationService,
            AuditLogService auditLogService,
            AppProperties appProperties
    ) {
        this.bookingRepository = bookingRepository;
        this.bedRepository = bedRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
        this.pricingService = pricingService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.appProperties = appProperties;
    }

    @Transactional
    public BookingResponse createBooking(UUID userId, CreateBookingRequest request) {
        availabilityService.validateTimeWindow(request.checkIn(), request.checkOut());
        if (request.checkIn().isBefore(Instant.now().minus(5, ChronoUnit.MINUTES))) {
            throw new BadRequestException("Check-in time cannot be in the past", "INVALID_DATES");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));

        // Acquire PESSIMISTIC_WRITE lock on the Bed row to serialize concurrent booking attempts
        Bed bed = bedRepository.findByIdForUpdate(request.bedId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));

        Room room = bed.getRoom();
        Property property = room.getProperty();

        if (!property.getId().equals(request.propertyId())) {
            throw new BadRequestException("Bed does not belong to the specified property", "INVALID_BED_PROPERTY");
        }
        if (property.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Property is not verified for bookings", "PROPERTY_NOT_VERIFIED");
        }
        if (property.getListingStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Property is currently not active", "PROPERTY_INACTIVE");
        }
        if (room.getStatus() != RoomStatus.AVAILABLE || bed.getStatus() != BedStatus.AVAILABLE) {
            throw new ConflictException("The selected bed is not available", "BED_UNAVAILABLE");
        }

        validateGenderPolicy(property, user);

        if (bookingRepository.existsOverlappingBooking(bed.getId(), request.checkIn(), request.checkOut(), Instant.now())) {
            throw new ConflictException("The selected bed is not available for the requested time period", "BED_UNAVAILABLE");
        }

        PriceBreakdown breakdown = pricingService.calculatePrice(
                property, room, request.stayType(), request.checkIn(), request.checkOut()
        );

        boolean approvalRequired = !property.isInstantBookingEnabled() || request.requestOwnerApproval();
        Instant expiresAt = approvalRequired
                ? Instant.now().plus(appProperties.booking().ownerApprovalExpirationHours(), ChronoUnit.HOURS)
                : Instant.now().plus(appProperties.booking().holdExpirationMinutes(), ChronoUnit.MINUTES);

        Booking booking = new Booking();
        booking.setBookingReference(ReferenceGenerator.bookingReference());
        booking.setUser(user);
        booking.setProperty(property);
        booking.setBed(bed);
        booking.setCheckIn(request.checkIn());
        booking.setCheckOut(request.checkOut());
        booking.setStayType(request.stayType());
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setApprovalRequired(approvalRequired);
        booking.setGuestName(request.guestName().trim());
        booking.setGuestPhone(request.guestPhone().trim());
        booking.setBaseAmount(breakdown.baseAmount());
        booking.setDepositAmount(breakdown.depositAmount());
        booking.setDiscountAmount(breakdown.discountAmount());
        booking.setExtraCharges(breakdown.extraCharges());
        booking.setTotalAmount(breakdown.totalAmount());
        booking.setAmountPaid(BigDecimal.ZERO);
        booking.setRefundAmount(BigDecimal.ZERO);
        booking.setExpiresAt(expiresAt);

        booking = bookingRepository.save(booking);

        notificationService.notify(
                property.getOwner(),
                approvalRequired ? "New Booking Approval Request" : "New Pending Booking",
                "Booking " + booking.getBookingReference() + " for bed " + bed.getBedNumber() + " at " + property.getName(),
                NotificationType.BOOKING_CREATED
        );
        auditLogService.record(
                userId, user.getEmail(), "BOOKING_CREATED", "Booking", booking.getId().toString(),
                "Reference=" + booking.getBookingReference() + ", Total=" + booking.getTotalAmount()
        );

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(UUID actorId, UserRole actorRole, UUID bookingId) {
        Booking booking = findBookingById(bookingId);
        ensureCanAccessBooking(actorId, actorRole, booking);
        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingStatusResponse getBookingStatus(UUID actorId, UserRole actorRole, UUID bookingId) {
        Booking booking = findBookingById(bookingId);
        ensureCanAccessBooking(actorId, actorRole, booking);
        return new BookingStatusResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getBookingStatus(),
                booking.isApprovalRequired(),
                booking.getTotalAmount(),
                booking.getAmountPaid(),
                booking.getExpiresAt()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getMyBookings(UUID userId, int page, int size) {
        return PagedResponse.from(
                bookingRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))
                ).map(this::toResponse)
        );
    }

    @Transactional
    public BookingResponse confirmBooking(UUID actorId, UserRole actorRole, UUID bookingId) {
        Booking booking = findBookingById(bookingId);
        ensureCanAccessBooking(actorId, actorRole, booking);

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be confirmed", "INVALID_BOOKING_STATE");
        }
        if (booking.isApprovalRequired()) {
            throw new BadRequestException("Booking requires property owner approval before confirmation", "OWNER_APPROVAL_REQUIRED");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BadRequestException("Booking hold has expired", "BOOKING_EXPIRED");
        }
        if (booking.getAmountPaid().compareTo(booking.getTotalAmount()) < 0) {
            throw new BadRequestException("Full payment is required before confirming booking", "PAYMENT_REQUIRED");
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);
        booking = bookingRepository.save(booking);

        notificationService.notify(
                booking.getUser(),
                "Booking Confirmed",
                "Your booking " + booking.getBookingReference() + " at " + booking.getProperty().getName() + " is confirmed.",
                NotificationType.BOOKING_CONFIRMED
        );
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID actorId, UserRole actorRole, UUID bookingId, CancelBookingRequest request) {
        Booking booking = findBookingById(bookingId);
        if (actorRole != UserRole.ADMIN && !booking.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("Only the booking customer or an admin can cancel this booking");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING && booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state: " + booking.getBookingStatus(), "INVALID_BOOKING_STATE");
        }

        BigDecimal refund = pricingService.calculateRefundAmount(
                booking.getAmountPaid(), booking.getCheckIn(), Instant.now()
        );
        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setRefundAmount(refund);
        booking.setCancellationReason(request != null ? request.reason() : null);
        booking.setExpiresAt(null);
        booking = bookingRepository.save(booking);

        notificationService.notify(
                booking.getProperty().getOwner(),
                "Booking Cancelled",
                "Booking " + booking.getBookingReference() + " was cancelled.",
                NotificationType.BOOKING_CANCELLED
        );
        auditLogService.record(
                actorId, null, "BOOKING_CANCELLED", "Booking", booking.getId().toString(),
                "RefundAmount=" + refund
        );
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse approveBookingByOwner(UUID ownerId, UUID bookingId) {
        Booking booking = findBookingById(bookingId);
        if (!booking.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be approved", "INVALID_BOOKING_STATE");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BadRequestException("Booking request has expired", "BOOKING_EXPIRED");
        }

        booking.setApprovalRequired(false);
        if (booking.getAmountPaid().compareTo(booking.getTotalAmount()) >= 0) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setExpiresAt(null);
        } else {
            booking.setExpiresAt(Instant.now().plus(appProperties.booking().holdExpirationMinutes(), ChronoUnit.MINUTES));
        }
        booking = bookingRepository.save(booking);

        notificationService.notify(
                booking.getUser(),
                "Booking Request Approved",
                "Owner approved booking " + booking.getBookingReference() + ". Please complete payment if pending.",
                NotificationType.BOOKING_CONFIRMED
        );
        auditLogService.record(
                ownerId, null, "BOOKING_APPROVED", "Booking", booking.getId().toString(),
                "Reference=" + booking.getBookingReference()
        );
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse rejectBookingByOwner(UUID ownerId, UUID bookingId, RejectBookingRequest request) {
        Booking booking = findBookingById(bookingId);
        if (!booking.getProperty().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be rejected", "INVALID_BOOKING_STATE");
        }

        booking.setBookingStatus(BookingStatus.REJECTED);
        booking.setCancellationReason(request.reason());
        booking.setRefundAmount(booking.getAmountPaid());
        booking.setExpiresAt(null);
        booking = bookingRepository.save(booking);

        notificationService.notify(
                booking.getUser(),
                "Booking Request Rejected",
                "Booking " + booking.getBookingReference() + " was rejected: " + request.reason(),
                NotificationType.BOOKING_REJECTED
        );
        auditLogService.record(
                ownerId, null, "BOOKING_REJECTED", "Booking", booking.getId().toString(),
                "Reason=" + request.reason()
        );
        return toResponse(booking);
    }

    public BookingResponse toResponse(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getBookingReference(),
                b.getUser().getId(),
                b.getProperty().getId(),
                b.getProperty().getName(),
                b.getBed().getId(),
                b.getBed().getBedNumber(),
                b.getBed().getRoom().getRoomNumber(),
                b.getCheckIn(),
                b.getCheckOut(),
                b.getStayType(),
                b.getBookingStatus(),
                b.isApprovalRequired(),
                b.getGuestName(),
                b.getGuestPhone(),
                b.getBaseAmount(),
                b.getDepositAmount(),
                b.getDiscountAmount(),
                b.getExtraCharges(),
                b.getTotalAmount(),
                b.getAmountPaid(),
                b.getRefundAmount(),
                b.getCancellationReason(),
                b.getExpiresAt(),
                b.getCreatedAt()
        );
    }

    private Booking findBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
    }

    private void ensureCanAccessBooking(UUID actorId, UserRole role, Booking booking) {
        if (role == UserRole.ADMIN) return;
        if (booking.getUser().getId().equals(actorId)) return;
        if (role == UserRole.PROPERTY_OWNER && booking.getProperty().getOwner().getId().equals(actorId)) return;
        throw new ForbiddenException("You do not have access to this booking");
    }

    private void validateGenderPolicy(Property property, User user) {
        if (user.getGender() == null || property.getGenderPolicy() == GenderPolicy.CO_ED) {
            return;
        }
        if (property.getGenderPolicy() == GenderPolicy.MALE_ONLY && user.getGender() != Gender.MALE) {
            throw new BadRequestException("This property is restricted to male guests only", "GENDER_POLICY_MISMATCH");
        }
        if (property.getGenderPolicy() == GenderPolicy.FEMALE_ONLY && user.getGender() != Gender.FEMALE) {
            throw new BadRequestException("This property is restricted to female guests only", "GENDER_POLICY_MISMATCH");
        }
    }
}
