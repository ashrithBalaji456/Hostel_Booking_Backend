package com.accommodation.platform.booking.service;

import com.accommodation.platform.availability.service.AvailabilityService;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.entity.BookingFailure;
import com.accommodation.platform.booking.repository.BookingFailureRepository;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.NotificationType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.property.dto.PropertyDtos.PropertySearchItemResponse;
import com.accommodation.platform.property.service.PropertySearchService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BookingFailureService {

    private final BookingFailureRepository failureRepository;
    private final BookingRepository bookingRepository;
    private final PropertySearchService searchService;
    private final AvailabilityService availabilityService;
    private final NotificationService notificationService;

    public BookingFailureService(
            BookingFailureRepository failureRepository,
            BookingRepository bookingRepository,
            PropertySearchService searchService,
            AvailabilityService availabilityService,
            NotificationService notificationService
    ) {
        this.failureRepository = failureRepository;
        this.bookingRepository = bookingRepository;
        this.searchService = searchService;
        this.availabilityService = availabilityService;
        this.notificationService = notificationService;
    }

    @Transactional
    public FailureWithAlternativesResponse reportFailure(UUID actorId, UUID bookingId, ReportFailureRequest req) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (!booking.getUser().getId().equals(actorId) && !booking.getProperty().getOwner().getId().equals(actorId)) {
            throw new ForbiddenException("Access denied");
        }

        BookingFailure failure = new BookingFailure();
        failure.setBookingId(bookingId);
        failure.setFailureReason(req.failureReason().trim().toUpperCase());
        failure.setDescription(req.description());
        failure.setResolutionStatus("OPEN");
        failure = failureRepository.save(failure);

        var alternatives = searchService.search(
                booking.getProperty().getCity(), null, null,
                booking.getProperty().getLatitude(), booking.getProperty().getLongitude(), 15.0,
                booking.getCheckIn(), booking.getCheckOut(), booking.getStayType(),
                null, null, null, null, booking.getProperty().getGenderPolicy(),
                null, null, true, true, "distance", 0, 5
        ).content().stream()
                .filter(p -> !p.id().equals(booking.getProperty().getId()))
                .toList();

        notificationService.notify(
                booking.getUser(),
                "Booking Fulfillment Issue Reported",
                "We recorded an issue for booking " + booking.getBookingReference() + " and found " + alternatives.size() + " alternatives.",
                NotificationType.BOOKING_CANCELLED
        );

        return new FailureWithAlternativesResponse(failure, alternatives);
    }

    @Transactional
    public BookingFailure respondToAlternative(UUID customerId, UUID failureId, boolean accepted, UUID alternativePropertyId, UUID alternativeBedId) {
        BookingFailure failure = failureRepository.findById(failureId)
                .orElseThrow(() -> new ResourceNotFoundException("Failure record not found", "FAILURE_NOT_FOUND"));
        Booking booking = bookingRepository.findById(failure.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("Access denied");
        }

        failure.setCustomerAcceptedAlternative(accepted);
        if (accepted) {
            if (alternativePropertyId == null || alternativeBedId == null) {
                throw new BadRequestException("Alternative property and bed must be specified", "INVALID_ALTERNATIVE");
            }
            if (!availabilityService.isBedAvailable(alternativeBedId, booking.getCheckIn(), booking.getCheckOut())) {
                throw new BadRequestException("Selected alternative bed is no longer available", "BED_UNAVAILABLE");
            }
            failure.setAlternativePropertyId(alternativePropertyId);
            failure.setAlternativeBedId(alternativeBedId);
            failure.setResolutionStatus("RELOCATED");
        } else {
            failure.setResolutionStatus("REFUND_INITIATED");
            booking.setBookingStatus(BookingStatus.CANCELLED);
            booking.setRefundAmount(booking.getAmountPaid());
            booking.setCancellationReason("Booking fulfillment failure: " + failure.getFailureReason());
            bookingRepository.save(booking);
        }

        return failureRepository.save(failure);
    }

    public record ReportFailureRequest(
            @NotBlank String failureReason,
            String description
    ) {}

    public record RespondAlternativeRequest(
            boolean accepted,
            UUID alternativePropertyId,
            UUID alternativeBedId
    ) {}

    public record FailureWithAlternativesResponse(
            BookingFailure failure,
            List<PropertySearchItemResponse> alternatives
    ) {}
}
