package com.accommodation.platform.booking.dto;

import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.StayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BookingDtos {

    private BookingDtos() {}

    public record CreateBookingRequest(
            @NotNull(message = "Property ID is required")
            UUID propertyId,

            @NotNull(message = "Bed ID is required")
            UUID bedId,

            @NotNull(message = "Check-in time is required")
            Instant checkIn,

            @NotNull(message = "Check-out time is required")
            Instant checkOut,

            @NotNull(message = "Stay type is required")
            StayType stayType,

            @NotBlank(message = "Guest name is required")
            String guestName,

            @NotBlank(message = "Guest phone is required")
            @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid Indian phone number format")
            String guestPhone,

            boolean requestOwnerApproval
    ) {}

    public record CancelBookingRequest(
            String reason
    ) {}

    public record RejectBookingRequest(
            @NotBlank(message = "Rejection reason is required")
            String reason
    ) {}

    public record BookingResponse(
            UUID id,
            String bookingReference,
            UUID userId,
            UUID propertyId,
            String propertyName,
            UUID bedId,
            String bedNumber,
            String roomNumber,
            Instant checkIn,
            Instant checkOut,
            StayType stayType,
            BookingStatus bookingStatus,
            boolean approvalRequired,
            String guestName,
            String guestPhone,
            BigDecimal baseAmount,
            BigDecimal depositAmount,
            BigDecimal discountAmount,
            BigDecimal extraCharges,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            BigDecimal refundAmount,
            String cancellationReason,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public record BookingStatusResponse(
            UUID bookingId,
            String bookingReference,
            BookingStatus bookingStatus,
            boolean approvalRequired,
            BigDecimal totalAmount,
            BigDecimal amountPaid,
            Instant expiresAt
    ) {}
}
