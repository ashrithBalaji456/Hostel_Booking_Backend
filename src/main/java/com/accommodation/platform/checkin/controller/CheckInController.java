package com.accommodation.platform.checkin.controller;

import com.accommodation.platform.checkin.entity.CheckInRecord;
import com.accommodation.platform.checkin.entity.CheckOutRecord;
import com.accommodation.platform.checkin.service.CheckInService;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Check-In & Check-Out", description = "Owner and customer check-in/out verification and no-show handling")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping("/owner/bookings/{bookingId}/checkin")
    @Operation(summary = "Record guest check-in (owner)")
    public ResponseEntity<ApiResponse<CheckInRecord>> ownerCheckIn(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String notes
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Check-in recorded",
                checkInService.checkInByOwner(SecurityUtils.getCurrentUserId(), bookingId, notes)));
    }

    @PostMapping("/bookings/{bookingId}/checkin/confirm")
    @Operation(summary = "Confirm check-in (customer)")
    public ResponseEntity<ApiResponse<CheckInRecord>> customerConfirmCheckIn(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Check-in confirmed",
                checkInService.confirmCheckInByCustomer(SecurityUtils.getCurrentUserId(), bookingId)));
    }

    @PostMapping("/owner/bookings/{bookingId}/checkout")
    @Operation(summary = "Record guest check-out and mark booking completed (owner)")
    public ResponseEntity<ApiResponse<CheckOutRecord>> ownerCheckOut(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) BigDecimal lateFee,
            @RequestParam(required = false) String notes
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Check-out recorded",
                checkInService.checkOutByOwner(SecurityUtils.getCurrentUserId(), bookingId, lateFee, notes)));
    }

    @PostMapping("/owner/bookings/{bookingId}/no-show")
    @Operation(summary = "Mark a confirmed booking as no-show (owner)")
    public ResponseEntity<ApiResponse<Void>> markNoShow(
            @PathVariable UUID bookingId,
            @RequestParam(required = false) String notes
    ) {
        checkInService.markNoShow(SecurityUtils.getCurrentUserId(), bookingId, notes);
        return ResponseEntity.ok(ApiResponse.ok("Booking marked as no-show"));
    }
}
