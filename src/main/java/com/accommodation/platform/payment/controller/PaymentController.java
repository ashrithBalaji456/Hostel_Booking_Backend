package com.accommodation.platform.payment.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.payment.dto.PaymentDtos.*;
import com.accommodation.platform.payment.service.PaymentService;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments", description = "Idempotent payment initiation, verification, webhooks, and refunds")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a payment for a pending booking")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.initiatePayment(
                SecurityUtils.getCurrentUserId(), request, idempotencyKey
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment initiated", response));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment details")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable UUID paymentId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Payment fetched",
                paymentService.getPayment(principal.getId(), principal.getRole(), paymentId)
        ));
    }

    @PostMapping("/{paymentId}/verify")
    @Operation(summary = "Verify payment completion with provider")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        PaymentResponse response = paymentService.verifyPayment(
                SecurityUtils.getCurrentUserId(), paymentId, request
        );
        return ResponseEntity.ok(ApiResponse.ok("Payment processed", response));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Process payment provider webhook idempotently")
    public ResponseEntity<ApiResponse<PaymentResponse>> webhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @Valid @RequestBody WebhookPayload payload
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Webhook processed",
                paymentService.processWebhook(payload, signature)
        ));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Request a full or partial refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody(required = false) RefundRequest request
    ) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Refund processed",
                paymentService.refundPayment(principal.getId(), principal.getRole(), paymentId, request)
        ));
    }
}
