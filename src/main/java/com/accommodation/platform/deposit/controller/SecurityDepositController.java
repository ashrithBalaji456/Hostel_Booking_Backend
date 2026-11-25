package com.accommodation.platform.deposit.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.deposit.entity.SecurityDeposit;
import com.accommodation.platform.deposit.service.SecurityDepositService;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Security Deposits", description = "Security deposit tracking, deductions, refunds, and disputes")
public class SecurityDepositController {

    private final SecurityDepositService depositService;

    public SecurityDepositController(SecurityDepositService depositService) {
        this.depositService = depositService;
    }

    @GetMapping("/deposits/bookings/{bookingId}")
    @Operation(summary = "Get security deposit status for a booking")
    public ResponseEntity<ApiResponse<SecurityDeposit>> getDeposit(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit fetched", depositService.createOrGetDeposit(bookingId)));
    }

    @PostMapping("/deposits/bookings/{bookingId}/request-refund")
    @Operation(summary = "Customer requests security deposit refund")
    public ResponseEntity<ApiResponse<SecurityDeposit>> requestRefund(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit refund requested",
                depositService.requestRefund(SecurityUtils.getCurrentUserId(), bookingId)));
    }

    @PostMapping("/owner/deposits/bookings/{bookingId}/settle")
    @Operation(summary = "Owner settles deposit with optional documented deduction")
    public ResponseEntity<ApiResponse<SecurityDeposit>> settleDeposit(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) BigDecimal deduction,
            @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit settled",
                depositService.settleDepositByOwner(SecurityUtils.getCurrentUserId(), bookingId, deduction, reason)));
    }

    @PostMapping("/deposits/bookings/{bookingId}/dispute")
    @Operation(summary = "Customer disputes a security deposit deduction")
    public ResponseEntity<ApiResponse<SecurityDeposit>> disputeDeposit(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Deposit deduction disputed",
                depositService.disputeDeduction(SecurityUtils.getCurrentUserId(), bookingId)));
    }
}
