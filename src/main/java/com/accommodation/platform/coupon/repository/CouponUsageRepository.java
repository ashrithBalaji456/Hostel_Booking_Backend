package com.accommodation.platform.coupon.repository;

import com.accommodation.platform.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, UUID> {
    long countByCouponIdAndUserId(UUID couponId, UUID userId);
    boolean existsByCouponIdAndBookingId(UUID couponId, UUID bookingId);
}
