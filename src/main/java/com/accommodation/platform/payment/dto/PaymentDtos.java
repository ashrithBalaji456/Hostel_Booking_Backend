package com.accommodation.platform.payment.dto;

import com.accommodation.platform.common.enums.PaymentMethod;
import com.accommodation.platform.common.enums.PaymentProvider;
import com.accommodation.platform.common.enums.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {}

    public record InitiatePaymentRequest(
            @NotNull(message = "Booking ID is required")
            UUID bookingId,

            @NotNull(message = "Payment method is required")
            PaymentMethod paymentMethod,

            String idempotencyKey
    ) {}

    public record VerifyPaymentRequest(
            @NotBlank(message = "Provider transaction ID is required")
            String providerTransactionId,

            String signature,

            boolean simulateFailure,

            String failureReason
    ) {}

    public record WebhookPayload(
            @NotBlank(message = "Payment reference is required")
            String paymentReference,

            @NotBlank(message = "Provider transaction ID is required")
            String providerTransactionId,

            @NotNull(message = "Status is required")
            PaymentStatus status,

            String failureReason
    ) {}

    public record RefundRequest(
            @DecimalMin(value = "0.01", message = "Refund amount must be positive")
            BigDecimal amount,

            String reason
    ) {}

    public record PaymentResponse(
            UUID id,
            UUID bookingId,
            String bookingReference,
            String paymentReference,
            PaymentProvider provider,
            String providerOrderId,
            String providerTransactionId,
            BigDecimal amount,
            BigDecimal refundAmount,
            String currency,
            PaymentStatus paymentStatus,
            PaymentMethod paymentMethod,
            String failureReason,
            Instant createdAt
    ) {}
}
