package com.accommodation.platform.booking.service;

import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.config.AppProperties;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.room.entity.Room;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

@Service
public class PricingCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final AppProperties appProperties;

    public PricingCalculationService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public PriceBreakdown calculatePrice(Property property, Room room, StayType stayType, Instant checkIn, Instant checkOut) {
        PropertyPricing propPricing = property.getPricing();
        if (propPricing == null) {
            throw new BadRequestException("Property pricing is not configured", "PRICING_NOT_CONFIGURED");
        }

        long totalMinutes = Duration.between(checkIn, checkOut).toMinutes();
        if (totalMinutes <= 0) {
            throw new BadRequestException("Check-out must be after check-in", "INVALID_DATES");
        }

        long hours = (long) Math.ceil(totalMinutes / 60.0);
        long days = (long) Math.ceil(totalMinutes / (60.0 * 24.0));

        BigDecimal hourlyRate = room.getHourlyRate() != null ? room.getHourlyRate() : propPricing.getHourlyRate();
        BigDecimal nightlyRate = room.getNightlyRate() != null ? room.getNightlyRate() : propPricing.getNightlyRate();
        BigDecimal weeklyRate = room.getWeeklyRate() != null ? room.getWeeklyRate() : propPricing.getWeeklyRate();
        BigDecimal monthlyRate = room.getMonthlyRate() != null ? room.getMonthlyRate() : propPricing.getMonthlyRate();

        BigDecimal baseAmount;
        BigDecimal depositAmount = BigDecimal.ZERO;

        if (stayType == StayType.SHORT_STAY) {
            if (hours < property.getMinShortStayHours()) {
                throw new BadRequestException(
                        "Short stay must be at least " + property.getMinShortStayHours() + " hours",
                        "INVALID_STAY_DURATION"
                );
            }
            if (days > property.getMaxShortStayDays()) {
                throw new BadRequestException(
                        "Short stay cannot exceed " + property.getMaxShortStayDays() + " days",
                        "INVALID_STAY_DURATION"
                );
            }

            if (hours < 24 && hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0) {
                baseAmount = hourlyRate.multiply(BigDecimal.valueOf(hours));
            } else {
                long nights = Math.max(1, days);
                baseAmount = nightlyRate.multiply(BigDecimal.valueOf(nights));
            }
        } else {
            if (days < property.getMinLongStayDays()) {
                throw new BadRequestException(
                        "Long stay must be at least " + property.getMinLongStayDays() + " days",
                        "INVALID_STAY_DURATION"
                );
            }
            long maxDays = property.getMaxLongStayMonths() * 31L;
            if (days > maxDays) {
                throw new BadRequestException(
                        "Long stay cannot exceed " + property.getMaxLongStayMonths() + " months",
                        "INVALID_STAY_DURATION"
                );
            }

            if (days >= 28 && monthlyRate != null && monthlyRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal months = BigDecimal.valueOf(days)
                        .divide(BigDecimal.valueOf(30), 4, RoundingMode.HALF_UP);
                baseAmount = monthlyRate.multiply(months);
            } else if (days >= 7 && weeklyRate != null && weeklyRate.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal weeks = BigDecimal.valueOf(days)
                        .divide(BigDecimal.valueOf(7), 4, RoundingMode.HALF_UP);
                baseAmount = weeklyRate.multiply(weeks);
            } else {
                baseAmount = nightlyRate.multiply(BigDecimal.valueOf(days));
            }
            depositAmount = propPricing.getSecurityDeposit() != null ? propPricing.getSecurityDeposit() : BigDecimal.ZERO;
        }

        baseAmount = baseAmount.setScale(2, RoundingMode.HALF_UP);
        depositAmount = depositAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountPct = propPricing.getDiscountPercentage() != null
                ? propPricing.getDiscountPercentage() : BigDecimal.ZERO;
        BigDecimal discountAmount = baseAmount.multiply(discountPct)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal extraCharges = propPricing.getExtraCharges() != null
                ? propPricing.getExtraCharges().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        BigDecimal totalAmount = baseAmount
                .subtract(discountAmount)
                .add(depositAmount)
                .add(extraCharges)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new PriceBreakdown(baseAmount, depositAmount, discountAmount, extraCharges, totalAmount);
    }

    public BigDecimal calculateRefundAmount(BigDecimal amountPaid, Instant checkIn, Instant cancelledAt) {
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long hoursUntilCheckIn = Duration.between(cancelledAt, checkIn).toHours();
        if (hoursUntilCheckIn >= appProperties.booking().cancellationFreeHoursBeforeCheckin()) {
            return amountPaid.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal feePct = appProperties.booking().cancellationFeePercentage();
        BigDecimal fee = amountPaid.multiply(feePct).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return amountPaid.subtract(fee).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public record PriceBreakdown(
            BigDecimal baseAmount,
            BigDecimal depositAmount,
            BigDecimal discountAmount,
            BigDecimal extraCharges,
            BigDecimal totalAmount
    ) {}
}
