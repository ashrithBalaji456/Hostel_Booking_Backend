package com.accommodation.platform.payment.service;

import com.accommodation.platform.common.enums.PaymentProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.MOCK;
    }

    @Override
    public String createOrder(String paymentReference, BigDecimal amount, String currency) {
        return "mock_order_" + paymentReference;
    }

    @Override
    public boolean verifyPayment(String providerOrderId, String providerTransactionId, String signature, boolean simulateFailure) {
        if (simulateFailure) {
            return false;
        }
        return providerTransactionId != null && !providerTransactionId.isBlank()
                && !providerTransactionId.toUpperCase().contains("FAIL");
    }

    @Override
    public String processRefund(String providerTransactionId, BigDecimal refundAmount) {
        return "mock_refund_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
