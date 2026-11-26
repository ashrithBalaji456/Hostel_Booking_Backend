package com.accommodation.platform.coupon.controller;

import com.accommodation.platform.common.enums.StayType;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.coupon.service.CouponService;
import com.accommodation.platform.coupon.service.CouponService.CouponPreviewResponse;
import com.accommodation.platform.coupon.service.CouponService.CouponResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coupons")
@Tag(name = "Coupons (Public)", description = "List active coupons and preview coupon discounts")
public class PublicCouponController {

    private final CouponService couponService;

    public PublicCouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/active")
    @Operation(summary = "List active coupons applicable to a property")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> listActive(
            @RequestParam(required = false) UUID propertyId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Active coupons fetched", couponService.listActiveCoupons(propertyId)));
    }

    @GetMapping("/preview")
    @Operation(summary = "Preview coupon discount for a stay")
    public ResponseEntity<ApiResponse<CouponPreviewResponse>> preview(
            @RequestParam String code,
            @RequestParam UUID propertyId,
            @RequestParam StayType stayType,
            @RequestParam BigDecimal baseAmount
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Coupon applied",
                couponService.previewCouponDiscount(code, propertyId, stayType, baseAmount)
        ));
    }
}
