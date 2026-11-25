package com.accommodation.platform.payment.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.*;
import com.accommodation.platform.common.exception.*;
import com.accommodation.platform.common.util.ReferenceGenerator;
import com.accommodation.platform.config.AppProperties;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.payment.dto.PaymentDtos.*;
import com.accommodation.platform.payment.entity.Payment;
import com.accommodation.platform.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final Map<PaymentProvider, PaymentGateway> gateways;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final AppProperties appProperties;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            List<PaymentGateway> gatewayList,
            NotificationService notificationService,
            AuditLogService auditLogService,
            AppProperties appProperties
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.gateways = gatewayList.stream().collect(Collectors.toMap(PaymentGateway::getProvider, Function.identity()));
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.appProperties = appProperties;
    }

    @Transactional
    public PaymentResponse initiatePayment(UUID userId, InitiatePaymentRequest request, String headerIdempotencyKey) {
        String idempotencyKey = (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank())
                ? headerIdempotencyKey.trim()
                : (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()
                    ? request.idempotencyKey().trim()
                    : UUID.randomUUID().toString());

        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Booking booking = bookingRepository.findById(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only pay for your own booking");
        }
        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Payment can only be initiated for pending bookings", "INVALID_BOOKING_STATE");
        }
        if (booking.isApprovalRequired()) {
            throw new BadRequestException("Booking is awaiting owner approval before payment", "OWNER_APPROVAL_REQUIRED");
        }
        if (booking.getExpiresAt() != null && booking.getExpiresAt().isBefore(Instant.now())) {
            booking.setBookingStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            throw new BadRequestException("Booking hold has expired", "BOOKING_EXPIRED");
        }

        BigDecimal remaining = booking.getTotalAmount().subtract(booking.getAmountPaid());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Booking is already fully paid", "ALREADY_PAID");
        }

        PaymentProvider provider = PaymentProvider.valueOf(appProperties.payment().provider().toUpperCase());
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new IllegalStateException("Configured payment provider not available: " + provider);
        }

        String payRef = ReferenceGenerator.paymentReference();
        String orderId = gateway.createOrder(payRef, remaining, "INR");

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentReference(payRef);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setProvider(provider);
        payment.setProviderOrderId(orderId);
        payment.setAmount(remaining);
        payment.setRefundAmount(BigDecimal.ZERO);
        payment.setCurrency("INR");
        payment.setPaymentStatus(PaymentStatus.INITIATED);
        payment.setPaymentMethod(request.paymentMethod());
        payment = paymentRepository.save(payment);

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse verifyPayment(UUID userId, UUID paymentId, VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found", "PAYMENT_NOT_FOUND"));

        if (!payment.getBooking().getUser().getId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        // Idempotent: if already succeeded, return current state without double-processing
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }
        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED || payment.getPaymentStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Cannot verify a refunded payment", "INVALID_PAYMENT_STATE");
        }

        PaymentGateway gateway = gateways.get(payment.getProvider());
        boolean verified = gateway.verifyPayment(
                payment.getProviderOrderId(),
                request.providerTransactionId(),
                request.signature(),
                request.simulateFailure()
        );

        payment.setProviderTransactionId(request.providerTransactionId());
        if (verified) {
            markPaymentSucceeded(payment);
        } else {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(request.failureReason() != null ? request.failureReason() : "Payment verification failed");
            paymentRepository.save(payment);
            notificationService.notify(
                    payment.getBooking().getUser(),
                    "Payment Failed",
                    "Payment " + payment.getPaymentReference() + " failed. Please retry before your hold expires.",
                    NotificationType.PAYMENT_FAILED
            );
        }

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse processWebhook(WebhookPayload payload, String signatureHeader) {
        if (signatureHeader != null && !signatureHeader.isBlank()) {
            String expected = RazorpayPaymentGateway.hmacSha256(
                    payload.paymentReference() + "|" + payload.providerTransactionId() + "|" + payload.status().name(),
                    appProperties.payment().webhookSecret()
            );
            if (!expected.equals(signatureHeader)) {
                throw new UnauthorizedException("Invalid webhook signature", "INVALID_WEBHOOK_SIGNATURE");
            }
        }

        Payment payment = paymentRepository.findByPaymentReference(payload.paymentReference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found", "PAYMENT_NOT_FOUND"));

        // Idempotency guard for duplicate webhook deliveries
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        payment.setProviderTransactionId(payload.providerTransactionId());
        if (payload.status() == PaymentStatus.SUCCESS) {
            markPaymentSucceeded(payment);
        } else if (payload.status() == PaymentStatus.FAILED) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(payload.failureReason());
            paymentRepository.save(payment);
        }
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID actorId, UserRole actorRole, UUID paymentId, RefundRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found", "PAYMENT_NOT_FOUND"));

        Booking booking = payment.getBooking();
        if (actorRole != UserRole.ADMIN && !booking.getUser().getId().equals(actorId)) {
            throw new ForbiddenException("You do not have permission to refund this payment");
        }
        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS && payment.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Only successful payments can be refunded", "INVALID_PAYMENT_STATE");
        }

        BigDecimal maxRefundable = payment.getAmount().subtract(payment.getRefundAmount());
        BigDecimal requestedRefund = (request != null && request.amount() != null) ? request.amount() : maxRefundable;
        if (requestedRefund.compareTo(BigDecimal.ZERO) <= 0 || requestedRefund.compareTo(maxRefundable) > 0) {
            throw new BadRequestException("Refund amount exceeds refundable balance", "INVALID_REFUND_AMOUNT");
        }

        PaymentGateway gateway = gateways.get(payment.getProvider());
        gateway.processRefund(payment.getProviderTransactionId(), requestedRefund);

        BigDecimal newTotalRefund = payment.getRefundAmount().add(requestedRefund);
        payment.setRefundAmount(newTotalRefund);
        payment.setPaymentStatus(
                newTotalRefund.compareTo(payment.getAmount()) == 0
                        ? PaymentStatus.REFUNDED
                        : PaymentStatus.PARTIALLY_REFUNDED
        );
        paymentRepository.save(payment);

        booking.setRefundAmount(booking.getRefundAmount().add(requestedRefund));
        bookingRepository.save(booking);

        notificationService.notify(
                booking.getUser(),
                "Refund Processed",
                "Refund of INR " + requestedRefund + " processed for payment " + payment.getPaymentReference(),
                NotificationType.REFUND_PROCESSED
        );
        auditLogService.record(
                actorId, null, "PAYMENT_REFUNDED", "Payment", payment.getId().toString(),
                "RefundAmount=" + requestedRefund
        );
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID actorId, UserRole actorRole, UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found", "PAYMENT_NOT_FOUND"));
        Booking booking = payment.getBooking();
        if (actorRole != UserRole.ADMIN
                && !booking.getUser().getId().equals(actorId)
                && !(actorRole == UserRole.PROPERTY_OWNER && booking.getProperty().getOwner().getId().equals(actorId))) {
            throw new ForbiddenException("Access denied");
        }
        return toResponse(payment);
    }

    private void markPaymentSucceeded(Payment payment) {
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        Booking booking = payment.getBooking();
        booking.setAmountPaid(booking.getAmountPaid().add(payment.getAmount()));
        if (booking.getAmountPaid().compareTo(booking.getTotalAmount()) >= 0
                && booking.getBookingStatus() == BookingStatus.PENDING
                && !booking.isApprovalRequired()) {
            booking.setBookingStatus(BookingStatus.CONFIRMED);
            booking.setExpiresAt(null);
        }
        bookingRepository.save(booking);

        notificationService.notify(
                booking.getUser(),
                "Payment Successful",
                "Payment " + payment.getPaymentReference() + " of INR " + payment.getAmount() + " succeeded.",
                NotificationType.PAYMENT_SUCCESS
        );
        auditLogService.record(
                booking.getUser().getId(), booking.getUser().getEmail(), "PAYMENT_SUCCESS",
                "Payment", payment.getId().toString(), "Amount=" + payment.getAmount()
        );
    }

    public PaymentResponse toResponse(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getBooking().getId(),
                p.getBooking().getBookingReference(),
                p.getPaymentReference(),
                p.getProvider(),
                p.getProviderOrderId(),
                p.getProviderTransactionId(),
                p.getAmount(),
                p.getRefundAmount(),
                p.getCurrency(),
                p.getPaymentStatus(),
                p.getPaymentMethod(),
                p.getFailureReason(),
                p.getCreatedAt()
        );
    }
}
