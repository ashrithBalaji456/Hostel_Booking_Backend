package com.accommodation.platform.common.util;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class ReferenceGenerator {

    private static final String ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ReferenceGenerator() {}

    public static String bookingReference() {
        return "BK-" + LocalDate.now(ZoneOffset.UTC).format(DATE_FMT) + "-" + randomSuffix(6);
    }

    public static String paymentReference() {
        return "PAY-" + LocalDate.now(ZoneOffset.UTC).format(DATE_FMT) + "-" + randomSuffix(8);
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
