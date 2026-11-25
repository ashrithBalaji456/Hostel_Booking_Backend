package com.accommodation.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        Cors cors,
        Jwt jwt,
        Booking booking,
        Payment payment,
        RateLimit rateLimit
) {
    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(
            String secret,
            long accessTokenExpirationMs,
            long refreshTokenExpirationMs
    ) {}

    public record Booking(
            int holdExpirationMinutes,
            int ownerApprovalExpirationHours,
            int cancellationFreeHoursBeforeCheckin,
            BigDecimal cancellationFeePercentage
    ) {}

    public record Payment(
            String provider,
            String webhookSecret,
            String razorpayKeyId,
            String razorpayKeySecret
    ) {}

    public record RateLimit(int authRequestsPerMinute) {}
}
