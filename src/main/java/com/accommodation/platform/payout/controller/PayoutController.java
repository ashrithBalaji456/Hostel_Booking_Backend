package com.accommodation.platform.payout.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.payout.entity.OwnerCommission;
import com.accommodation.platform.payout.entity.OwnerPayout;
import com.accommodation.platform.payout.service.PayoutService;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Commissions & Payouts", description = "Owner payout requests, history, and admin commission/settlement management")
public class PayoutController {

    private final PayoutService payoutService;

    public PayoutController(PayoutService payoutService) {
        this.payoutService = payoutService;
    }

    @PostMapping("/owner/payouts")
    @Operation(summary = "Request a payout for eligible earnings")
    public ResponseEntity<ApiResponse<OwnerPayout>> requestPayout(@RequestParam BigDecimal amount) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payout requested",
                        payoutService.requestPayout(SecurityUtils.getCurrentUserId(), amount)));
    }

    @GetMapping("/owner/payouts")
    @Operation(summary = "View owner payout history")
    public ResponseEntity<ApiResponse<PagedResponse<OwnerPayout>>> getMyPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Payouts fetched",
                payoutService.getOwnerPayouts(SecurityUtils.getCurrentUserId(), page, size)));
    }

    @PostMapping("/admin/commissions/{ownerId}")
    @Operation(summary = "Configure platform commission percentage for an owner")
    public ResponseEntity<ApiResponse<OwnerCommission>> setCommission(
            @PathVariable UUID ownerId,
            @RequestParam BigDecimal percentage
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Commission updated",
                payoutService.setCommission(SecurityUtils.getCurrentUserId(), ownerId, percentage)));
    }

    @PostMapping("/admin/payouts/{payoutId}/settle")
    @Operation(summary = "Settle an owner payout request")
    public ResponseEntity<ApiResponse<OwnerPayout>> settlePayout(
            @PathVariable UUID payoutId,
            @RequestParam String settlementReference
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Payout settled",
                payoutService.settlePayout(SecurityUtils.getCurrentUserId(), payoutId, settlementReference)));
    }
}
