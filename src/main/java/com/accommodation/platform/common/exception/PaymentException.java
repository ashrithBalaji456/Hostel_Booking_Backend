package com.accommodation.platform.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends ApiException {
    public PaymentException(String message, String errorCode) {
        super(message, HttpStatus.PAYMENT_REQUIRED, errorCode);
    }
}
