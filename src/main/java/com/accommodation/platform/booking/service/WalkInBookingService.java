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
import com.accommodation.platform.common.enums.BedStatus;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.RoomStatus;
import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.util.ReferenceGenerator;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.room.entity.Room;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class WalkInBookingService {

    private final BookingRepository bookingRepository;
    private final BedRepository bedRepository;
    private final AvailabilityBlockRepository blockRepository;
    private final AvailabilityService availabilityService;
    private final PricingCalculationService pricingService;
    private final BookingService bookingService;
    private final BookingStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;

    public WalkInBookingService(
            BookingRepository bookingRepository,
            BedRepository bedRepository,
            AvailabilityBlockRepository blockRepository,
            AvailabilityService availabilityService,
            PricingCalculationService pricingService,
            BookingService bookingService,
            BookingStatusHistoryRepository historyRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.bedRepository = bedRepository;
        this.blockRepository = blockRepository;
        this.availabilityService = availabilityService;
        this.pricingService = pricingService;
        this.bookingService = bookingService;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BookingResponse createWalkInBooking(UUID ownerId, WalkInBookingRequest req) {
        availabilityService.validateTimeWindow(req.checkIn(), req.checkOut());

        Bed bed = bedRepository.findByIdForUpdate(req.bedId())
                .orElseThrow(() -> new ResourceNotFoundException("Bed not found", "BED_NOT_FOUND"));
        Room room = bed.getRoom();
        Property property = room.getProperty();

        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }
        if (room.getStatus() != RoomStatus.AVAILABLE || bed.getStatus() != BedStatus.AVAILABLE) {
            throw new ConflictException("Bed is not available", "BED_UNAVAILABLE");
        }
        if (bookingRepository.existsOverlappingBooking(bed.getId(), req.checkIn(), req.checkOut(), Instant.now())
                || blockRepository.existsOverlappingBlock(bed.getId(), req.checkIn(), req.checkOut())) {
            throw new ConflictException("Bed is already booked or blocked for the requested period", "BED_UNAVAILABLE");
        }

        PriceBreakdown breakdown = pricingService.calculatePrice(property, room, req.stayType(), req.checkIn(), req.checkOut());
        User owner = userRepository.getReferenceById(ownerId);

        Booking booking = new Booking();
        booking.setBookingReference(ReferenceGenerator.bookingReference());
        booking.setUser(owner);
        booking.setProperty(property);
        booking.setBed(bed);
        booking.setCheckIn(req.checkIn());
        booking.setCheckOut(req.checkOut());
        booking.setStayType(req.stayType());
        booking.setBookingStatus(req.paidAtDesk() ? BookingStatus.CONFIRMED : BookingStatus.PENDING);
        booking.setApprovalRequired(false);
        booking.setWalkIn(true);
        booking.setGuestName(req.guestName().trim());
        booking.setGuestPhone(req.guestPhone().trim());
        booking.setBaseAmount(breakdown.baseAmount());
        booking.setDepositAmount(breakdown.depositAmount());
        booking.setDiscountAmount(breakdown.discountAmount());
        booking.setExtraCharges(breakdown.extraCharges());
        booking.setTotalAmount(breakdown.totalAmount());
        booking.setAmountPaid(req.paidAtDesk() ? breakdown.totalAmount() : BigDecimal.ZERO);
        booking = bookingRepository.save(booking);

        historyRepository.save(new BookingStatusHistory(
                booking.getId(), null, booking.getBookingStatus(), ownerId, "Walk-in booking created by owner"
        ));
        return bookingService.toResponse(booking);
    }

    public record WalkInBookingRequest(
            @NotNull UUID bedId,
            @NotNull Instant checkIn,
            @NotNull Instant checkOut,
            @NotNull StayType stayType,
            @NotBlank String guestName,
            @NotBlank @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$") String guestPhone,
            boolean paidAtDesk
    ) {}
}
