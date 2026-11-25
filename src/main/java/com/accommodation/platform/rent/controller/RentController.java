package com.accommodation.platform.rent.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.rent.entity.RentPayment;
import com.accommodation.platform.rent.entity.RentSchedule;
import com.accommodation.platform.rent.service.RentService;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rent")
@Tag(name = "Monthly Rent Collection", description = "Long-term rent schedules and installment payments")
public class RentController {

    private final RentService rentService;

    public RentController(RentService rentService) {
        this.rentService = rentService;
    }

    @PostMapping("/bookings/{bookingId}/schedule")
    @Operation(summary = "Generate or retrieve monthly rent schedule for a long-stay booking")
    public ResponseEntity<ApiResponse<List<RentSchedule>>> generateSchedule(@PathVariable UUID bookingId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rent schedule ready", rentService.generateMonthlySchedule(bookingId)));
    }

    @GetMapping("/bookings/{bookingId}/schedule")
    @Operation(summary = "Get monthly rent schedule for a booking")
    public ResponseEntity<ApiResponse<List<RentSchedule>>> getSchedule(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Rent schedule fetched", rentService.getSchedule(bookingId)));
    }

    @PostMapping("/schedules/{scheduleId}/pay")
    @Operation(summary = "Record a full or partial rent installment payment")
    public ResponseEntity<ApiResponse<RentPayment>> payRent(
            @PathVariable UUID scheduleId,
            @RequestParam BigDecimal amount
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Rent payment recorded",
                        rentService.recordRentPayment(SecurityUtils.getCurrentUserId(), scheduleId, amount)));
    }
}
