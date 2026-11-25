package com.accommodation.platform.dashboard.controller;

import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.booking.dto.BookingDtos.RejectBookingRequest;
import com.accommodation.platform.booking.service.BookingService;
import com.accommodation.platform.booking.service.WalkInBookingService;
import com.accommodation.platform.booking.service.WalkInBookingService.WalkInBookingRequest;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.dashboard.dto.DashboardDtos.OwnerDashboardResponse;
import com.accommodation.platform.dashboard.service.OwnerDashboardService;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owner")
@Tag(name = "Owner Dashboard", description = "Booking approvals, rejections, earnings, and statistics for property owners")
public class OwnerDashboardController {

    private final OwnerDashboardService ownerDashboardService;
    private final BookingService bookingService;
    private final WalkInBookingService walkInBookingService;

    public OwnerDashboardController(
            OwnerDashboardService ownerDashboardService,
            BookingService bookingService,
            WalkInBookingService walkInBookingService
    ) {
        this.ownerDashboardService = ownerDashboardService;
        this.bookingService = bookingService;
        this.walkInBookingService = walkInBookingService;
    }

    @GetMapping("/bookings")
    @Operation(summary = "List bookings across properties owned by the authenticated owner")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getOwnerBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Owner bookings fetched",
                ownerDashboardService.getOwnerBookings(SecurityUtils.getCurrentUserId(), status, page, size)
        ));
    }

    @PostMapping("/bookings/{bookingId}/approve")
    @Operation(summary = "Approve a pending booking request")
    public ResponseEntity<ApiResponse<BookingResponse>> approveBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking approved",
                bookingService.approveBookingByOwner(SecurityUtils.getCurrentUserId(), bookingId)
        ));
    }

    @PostMapping("/bookings/{bookingId}/reject")
    @Operation(summary = "Reject a pending booking request")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody RejectBookingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking rejected",
                bookingService.rejectBookingByOwner(SecurityUtils.getCurrentUserId(), bookingId, request)
        ));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get owner dashboard summary and upcoming check-ins")
    public ResponseEntity<ApiResponse<OwnerDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Owner dashboard fetched",
                ownerDashboardService.getDashboard(SecurityUtils.getCurrentUserId())
        ));
    }

    @PostMapping("/bookings/walk-in")
    @Operation(summary = "Record a walk-in booking using concurrency-safe bed inventory")
    public ResponseEntity<ApiResponse<BookingResponse>> createWalkInBooking(
            @Valid @RequestBody WalkInBookingRequest request
    ) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.ok("Walk-in booking created",
                        walkInBookingService.createWalkInBooking(SecurityUtils.getCurrentUserId(), request)));
    }
}
