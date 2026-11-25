package com.accommodation.platform.coupon.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.coupon.service.CouponService;
import com.accommodation.platform.coupon.service.CouponService.CouponResponse;
import com.accommodation.platform.coupon.service.CouponService.CreateCouponRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@Tag(name = "Coupons (Admin)", description = "Create, list, activate, and deactivate promotional coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @Operation(summary = "Create a new promotional coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Coupon created", couponService.createCoupon(request)));
    }

    @GetMapping
    @Operation(summary = "List all coupons")
    public ResponseEntity<ApiResponse<PagedResponse<CouponResponse>>> listCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Coupons fetched", couponService.listCoupons(page, size)));
    }

    @PostMapping("/{couponId}/status")
    @Operation(summary = "Activate or deactivate a coupon")
    public ResponseEntity<ApiResponse<CouponResponse>> toggleCoupon(
            @PathVariable UUID couponId,
            @RequestParam boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Coupon updated", couponService.toggleCoupon(couponId, active)));
    }
}
