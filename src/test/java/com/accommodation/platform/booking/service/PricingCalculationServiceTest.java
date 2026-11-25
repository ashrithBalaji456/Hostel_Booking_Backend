package com.accommodation.platform.booking.service;

import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.config.AppProperties;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.entity.PropertyPricing;
import com.accommodation.platform.room.entity.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingCalculationServiceTest {

    private PricingCalculationService pricingService;
    private Property property;
    private Room room;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                "Asia/Kolkata",
                new AppProperties.Cors(List.of("http://localhost:3000")),
                new AppProperties.Jwt("test-secret-key-at-least-32-bytes-long", 900000L, 604800000L),
                new AppProperties.Booking(15, 24, 24, new BigDecimal("20")),
                new AppProperties.Payment("MOCK", "secret", "", ""),
                new AppProperties.RateLimit(20)
        );
        pricingService = new PricingCalculationService(props);

        property = new Property();
        property.setMinShortStayHours(3);
        property.setMaxShortStayDays(14);
        property.setMinLongStayDays(7);
        property.setMaxLongStayMonths(12);

        PropertyPricing pricing = new PropertyPricing();
        pricing.setHourlyRate(new BigDecimal("100.00"));
        pricing.setNightlyRate(new BigDecimal("500.00"));
        pricing.setWeeklyRate(new BigDecimal("3000.00"));
        pricing.setMonthlyRate(new BigDecimal("9000.00"));
        pricing.setSecurityDeposit(new BigDecimal("2000.00"));
        pricing.setExtraCharges(new BigDecimal("50.00"));
        pricing.setDiscountPercentage(new BigDecimal("10.00"));
        property.setPricing(pricing);

        room = new Room();
        room.setProperty(property);
    }

    @Test
    void calculatesHourlyShortStayWithDiscountAndExtras() {
        Instant checkIn = Instant.parse("2026-12-01T10:00:00Z");
        Instant checkOut = checkIn.plus(5, ChronoUnit.HOURS);

        var breakdown = pricingService.calculatePrice(property, room, StayType.SHORT_STAY, checkIn, checkOut);

        assertEquals(new BigDecimal("500.00"), breakdown.baseAmount());
        assertEquals(new BigDecimal("50.00"), breakdown.discountAmount());
        assertEquals(new BigDecimal("0.00"), breakdown.depositAmount());
        assertEquals(new BigDecimal("50.00"), breakdown.extraCharges());
        assertEquals(new BigDecimal("500.00"), breakdown.totalAmount());
    }

    @Test
    void calculatesMonthlyLongStayWithSecurityDeposit() {
        Instant checkIn = Instant.parse("2026-12-01T12:00:00Z");
        Instant checkOut = checkIn.plus(30, ChronoUnit.DAYS);

        var breakdown = pricingService.calculatePrice(property, room, StayType.LONG_STAY, checkIn, checkOut);

        assertEquals(new BigDecimal("9000.00"), breakdown.baseAmount());
        assertEquals(new BigDecimal("900.00"), breakdown.discountAmount());
        assertEquals(new BigDecimal("2000.00"), breakdown.depositAmount());
        assertEquals(new BigDecimal("50.00"), breakdown.extraCharges());
        assertEquals(new BigDecimal("10150.00"), breakdown.totalAmount());
    }

    @Test
    void rejectsShortStayBelowMinimumHours() {
        Instant checkIn = Instant.parse("2026-12-01T10:00:00Z");
        Instant checkOut = checkIn.plus(2, ChronoUnit.HOURS);

        assertThrows(BadRequestException.class,
                () -> pricingService.calculatePrice(property, room, StayType.SHORT_STAY, checkIn, checkOut));
    }

    @Test
    void calculatesFullRefundWhenCancelledBeforeFreeWindow() {
        Instant checkIn = Instant.parse("2026-12-10T12:00:00Z");
        Instant cancelledAt = checkIn.minus(30, ChronoUnit.HOURS);

        BigDecimal refund = pricingService.calculateRefundAmount(new BigDecimal("1000.00"), checkIn, cancelledAt);
        assertEquals(new BigDecimal("1000.00"), refund);
    }

    @Test
    void appliesCancellationFeeInsideWindow() {
        Instant checkIn = Instant.parse("2026-12-10T12:00:00Z");
        Instant cancelledAt = checkIn.minus(6, ChronoUnit.HOURS);

        BigDecimal refund = pricingService.calculateRefundAmount(new BigDecimal("1000.00"), checkIn, cancelledAt);
        assertEquals(new BigDecimal("800.00"), refund);
    }
}
