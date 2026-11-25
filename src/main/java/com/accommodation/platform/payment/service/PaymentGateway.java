package com.accommodation.platform.payment.service;

import com.accommodation.platform.common.enums.PaymentProvider;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentProvider getProvider();
    String createOrder(String paymentReference, BigDecimal amount, String currency);
    boolean verifyPayment(String providerOrderId, String providerTransactionId, String signature, boolean simulateFailure);
    String processRefund(String providerTransactionId, BigDecimal refundAmount);
}
