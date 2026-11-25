package com.accommodation.platform.coupon.service;

import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.coupon.entity.Coupon;
import com.accommodation.platform.coupon.entity.CouponUsage;
import com.accommodation.platform.coupon.repository.CouponRepository;
import com.accommodation.platform.coupon.repository.CouponUsageRepository;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class CouponService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    public CouponService(CouponRepository couponRepository, CouponUsageRepository couponUsageRepository) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
    }

    @Transactional
    public CouponResponse createCoupon(CreateCouponRequest req) {
        String code = req.code().trim().toUpperCase();
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Coupon code already exists", "DUPLICATE_COUPON");
        }
        if (!req.validUntil().isAfter(req.validFrom())) {
            throw new BadRequestException("validUntil must be after validFrom", "INVALID_COUPON_DATES");
        }
        Coupon c = new Coupon();
        c.setCode(code);
        c.setDiscountType(req.discountType().toUpperCase());
        c.setDiscountValue(req.discountValue());
        c.setMinBookingAmount(req.minBookingAmount() != null ? req.minBookingAmount() : BigDecimal.ZERO);
        c.setMaxDiscountAmount(req.maxDiscountAmount());
        c.setValidFrom(req.validFrom());
        c.setValidUntil(req.validUntil());
        c.setUsageLimit(req.usageLimit());
        c.setPerCustomerLimit(req.perCustomerLimit() > 0 ? req.perCustomerLimit() : 1);
        c.setPropertyId(req.propertyId());
        c.setStayType(req.stayType());
        c.setActive(true);
        return toResponse(couponRepository.save(c));
    }

    @Transactional
    public CouponResponse toggleCoupon(UUID couponId, boolean active) {
        Coupon c = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found", "COUPON_NOT_FOUND"));
        c.setActive(active);
        return toResponse(couponRepository.save(c));
    }

    @Transactional(readOnly = true)
    public PagedResponse<CouponResponse> listCoupons(int page, int size) {
        return PagedResponse.from(
                couponRepository.findAll(PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50)))
                        .map(this::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal validateAndCalculateDiscount(
            String code, UUID userId, UUID propertyId, StayType stayType, BigDecimal baseAmount
    ) {
        Coupon c = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code", "INVALID_COUPON"));

        Instant now = Instant.now();
        if (!c.isActive() || now.isBefore(c.getValidFrom()) || now.isAfter(c.getValidUntil())) {
            throw new BadRequestException("Coupon is inactive or expired", "COUPON_EXPIRED");
        }
        if (c.getUsageLimit() != null && c.getUsedCount() >= c.getUsageLimit()) {
            throw new BadRequestException("Coupon usage limit reached", "COUPON_LIMIT_REACHED");
        }
        if (couponUsageRepository.countByCouponIdAndUserId(c.getId(), userId) >= c.getPerCustomerLimit()) {
            throw new BadRequestException("You have already used this coupon", "COUPON_CUSTOMER_LIMIT_REACHED");
        }
        if (c.getPropertyId() != null && !c.getPropertyId().equals(propertyId)) {
            throw new BadRequestException("Coupon is not valid for this property", "COUPON_PROPERTY_MISMATCH");
        }
        if (c.getStayType() != null && c.getStayType() != stayType) {
            throw new BadRequestException("Coupon is not valid for this stay type", "COUPON_STAY_TYPE_MISMATCH");
        }
        if (baseAmount.compareTo(c.getMinBookingAmount()) < 0) {
            throw new BadRequestException("Booking amount does not meet minimum for coupon", "COUPON_MIN_AMOUNT");
        }

        BigDecimal discount = "PERCENTAGE".equalsIgnoreCase(c.getDiscountType())
                ? baseAmount.multiply(c.getDiscountValue()).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : c.getDiscountValue().setScale(2, RoundingMode.HALF_UP);

        if (c.getMaxDiscountAmount() != null && discount.compareTo(c.getMaxDiscountAmount()) > 0) {
            discount = c.getMaxDiscountAmount().setScale(2, RoundingMode.HALF_UP);
        }
        return discount.min(baseAmount);
    }

    @Transactional
    public void recordCouponUsage(String code, UUID userId, UUID bookingId, BigDecimal discountApplied) {
        Coupon c = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found", "COUPON_NOT_FOUND"));
        if (couponUsageRepository.existsByCouponIdAndBookingId(c.getId(), bookingId)) {
            return;
        }
        c.setUsedCount(c.getUsedCount() + 1);
        couponRepository.save(c);

        CouponUsage usage = new CouponUsage();
        usage.setCoupon(c);
        usage.setUserId(userId);
        usage.setBookingId(bookingId);
        usage.setDiscountApplied(discountApplied);
        couponUsageRepository.save(usage);
    }

    private CouponResponse toResponse(Coupon c) {
        return new CouponResponse(
                c.getId(), c.getCode(), c.getDiscountType(), c.getDiscountValue(),
                c.getMinBookingAmount(), c.getMaxDiscountAmount(), c.getValidFrom(), c.getValidUntil(),
                c.getUsageLimit(), c.getUsedCount(), c.getPerCustomerLimit(),
                c.getPropertyId(), c.getStayType(), c.isActive()
        );
    }

    public record CreateCouponRequest(
            @NotBlank String code,
            @NotBlank String discountType,
            @NotNull @DecimalMin("0.01") BigDecimal discountValue,
            BigDecimal minBookingAmount,
            BigDecimal maxDiscountAmount,
            @NotNull Instant validFrom,
            @NotNull Instant validUntil,
            Integer usageLimit,
            @Min(1) int perCustomerLimit,
            UUID propertyId,
            StayType stayType
    ) {}

    public record CouponResponse(
            UUID id,
            String code,
            String discountType,
            BigDecimal discountValue,
            BigDecimal minBookingAmount,
            BigDecimal maxDiscountAmount,
            Instant validFrom,
            Instant validUntil,
            Integer usageLimit,
            int usedCount,
            int perCustomerLimit,
            UUID propertyId,
            StayType stayType,
            boolean active
    ) {}
}
