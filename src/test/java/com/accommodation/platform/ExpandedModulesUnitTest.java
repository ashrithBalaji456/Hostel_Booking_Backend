package com.accommodation.platform;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.coupon.entity.Coupon;
import com.accommodation.platform.coupon.repository.CouponRepository;
import com.accommodation.platform.coupon.repository.CouponUsageRepository;
import com.accommodation.platform.coupon.service.CouponService;
import com.accommodation.platform.deposit.entity.SecurityDeposit;
import com.accommodation.platform.deposit.repository.SecurityDepositRepository;
import com.accommodation.platform.deposit.service.SecurityDepositService;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.payment.repository.PaymentRepository;
import com.accommodation.platform.payout.entity.OwnerCommission;
import com.accommodation.platform.payout.entity.OwnerPayout;
import com.accommodation.platform.payout.repository.OwnerCommissionRepository;
import com.accommodation.platform.payout.repository.OwnerPayoutRepository;
import com.accommodation.platform.payout.service.PayoutService;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpandedModulesUnitTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponUsageRepository couponUsageRepository;
    @Mock private OwnerCommissionRepository commissionRepository;
    @Mock private OwnerPayoutRepository payoutRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private SecurityDepositRepository depositRepository;
    @Mock private BookingRepository bookingRepository;

    @Test
    void couponPercentageRespectsMaxCap() {
        CouponService service = new CouponService(couponRepository, couponUsageRepository);
        UUID userId = UUID.randomUUID();

        Coupon c = new Coupon();
        c.setCode("SAVE20");
        c.setDiscountType("PERCENTAGE");
        c.setDiscountValue(new BigDecimal("20.00"));
        c.setMinBookingAmount(new BigDecimal("500.00"));
        c.setMaxDiscountAmount(new BigDecimal("300.00"));
        c.setValidFrom(Instant.now().minus(1, ChronoUnit.DAYS));
        c.setValidUntil(Instant.now().plus(5, ChronoUnit.DAYS));
        c.setPerCustomerLimit(1);
        c.setActive(true);

        when(couponRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(c));
        when(couponUsageRepository.countByCouponIdAndUserId(any(), any())).thenReturn(0L);

        BigDecimal discount = service.validateAndCalculateDiscount(
                "SAVE20", userId, UUID.randomUUID(), StayType.SHORT_STAY, new BigDecimal("2000.00")
        );
        assertEquals(new BigDecimal("300.00"), discount);
    }

    @Test
    void payoutDeductsConfiguredCommission() {
        PayoutService service = new PayoutService(commissionRepository, payoutRepository, paymentRepository, auditLogService);
        UUID ownerId = UUID.randomUUID();

        when(paymentRepository.calculateOwnerNetEarnings(ownerId)).thenReturn(new BigDecimal("10000.00"));
        OwnerCommission commission = new OwnerCommission();
        commission.setOwnerId(ownerId);
        commission.setCommissionPercentage(new BigDecimal("12.00"));
        when(commissionRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(commission));
        when(payoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OwnerPayout payout = service.requestPayout(ownerId, new BigDecimal("5000.00"));
        assertEquals(new BigDecimal("600.00"), payout.getCommissionAmount());
        assertEquals(new BigDecimal("4400.00"), payout.getNetPayable());
    }

    @Test
    void depositDeductionRequiresReason() {
        SecurityDepositService service = new SecurityDepositService(depositRepository, bookingRepository);
        UUID ownerId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        User owner = new User();
        owner.setId(ownerId);
        Property property = new Property();
        property.setOwner(owner);
        Booking booking = new Booking();
        booking.setProperty(property);
        booking.setBookingStatus(com.accommodation.platform.common.enums.BookingStatus.COMPLETED);

        SecurityDeposit deposit = new SecurityDeposit();
        deposit.setBookingId(bookingId);
        deposit.setDepositAmount(new BigDecimal("2000.00"));

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(depositRepository.findByBookingId(bookingId)).thenReturn(Optional.of(deposit));

        assertThrows(BadRequestException.class,
                () -> service.settleDepositByOwner(ownerId, bookingId, new BigDecimal("500.00"), " "));
    }
}
