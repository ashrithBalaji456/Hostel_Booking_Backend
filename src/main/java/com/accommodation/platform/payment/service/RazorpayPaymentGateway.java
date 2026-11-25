package com.accommodation.platform.payment.service;

import com.accommodation.platform.common.enums.PaymentProvider;
import com.accommodation.platform.config.AppProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class RazorpayPaymentGateway implements PaymentGateway {

    private final AppProperties appProperties;

    public RazorpayPaymentGateway(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.RAZORPAY;
    }

    @Override
    public String createOrder(String paymentReference, BigDecimal amount, String currency) {
        // Server-side order identifier; integrate Razorpay Orders HTTP API here with razorpayKeyId/razorpayKeySecret
        return "order_rzp_" + paymentReference;
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String providerTransactionId, String signature, boolean simulateFailure) {
        if (simulateFailure || signature == null || signature.isBlank()) {
            return false;
        }
        String secret = appProperties.payment().razorpayKeySecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        String payload = providerOrderId + "|" + providerTransactionId;
        String expected = hmacSha256(payload, secret);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public String processRefund(String providerTransactionId, BigDecimal refundAmount) {
        return "rfnd_rzp_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 computation failed", e);
        }
    }
}
