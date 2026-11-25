package com.accommodation.platform.booking.controller;

import com.accommodation.platform.booking.dto.BookingDtos.*;
import com.accommodation.platform.booking.entity.BookingStatusHistory;
import com.accommodation.platform.booking.service.BookingModificationService;
import com.accommodation.platform.booking.service.BookingModificationService.ExtendBookingRequest;
import com.accommodation.platform.booking.service.BookingModificationService.ModifyBookingRequest;
import com.accommodation.platform.booking.service.BookingService;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Short-term and long-term bed booking management")
public class BookingController {

    private final BookingService bookingService;
    private final BookingModificationService modificationService;

    public BookingController(BookingService bookingService, BookingModificationService modificationService) {
        this.bookingService = bookingService;
        this.modificationService = modificationService;
    }

    @PostMapping
    @Operation(summary = "Create a new short-term or long-term bed booking")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(@Valid @RequestBody CreateBookingRequest request) {
        BookingResponse response = bookingService.createBooking(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking created successfully", response));
    }

    @GetMapping
    @Operation(summary = "List bookings for the authenticated user")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Bookings fetched successfully",
                bookingService.getMyBookings(SecurityUtils.getCurrentUserId(), page, size)
        ));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(@PathVariable UUID bookingId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking fetched successfully",
                bookingService.getBooking(principal.getId(), principal.getRole(), bookingId)
        ));
    }

    @GetMapping("/{bookingId}/status")
    @Operation(summary = "Get current booking status")
    public ResponseEntity<ApiResponse<BookingStatusResponse>> getBookingStatus(@PathVariable UUID bookingId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking status fetched",
                bookingService.getBookingStatus(principal.getId(), principal.getRole(), bookingId)
        ));
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "Confirm a paid pending booking")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(@PathVariable UUID bookingId) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking confirmed",
                bookingService.confirmBooking(principal.getId(), principal.getRole(), bookingId)
        ));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Cancel a booking and calculate eligible refund")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable UUID bookingId,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking cancelled",
                bookingService.cancelBooking(principal.getId(), principal.getRole(), bookingId, request)
        ));
    }

    @PutMapping("/{bookingId}")
    @Operation(summary = "Modify booking dates or bed")
    public ResponseEntity<ApiResponse<BookingResponse>> modifyBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ModifyBookingRequest request
    ) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking modified",
                modificationService.modifyBooking(principal.getId(), principal.getRole(), bookingId, request)
        ));
    }

    @PostMapping("/{bookingId}/extend")
    @Operation(summary = "Extend an active booking check-out date")
    public ResponseEntity<ApiResponse<BookingResponse>> extendBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ExtendBookingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking extended",
                modificationService.extendBooking(SecurityUtils.getCurrentUserId(), bookingId, request)
        ));
    }

    @PostMapping("/{bookingId}/transition-long-stay")
    @Operation(summary = "Request converting a short stay into a long stay (requires owner approval)")
    public ResponseEntity<ApiResponse<BookingResponse>> transitionToLongStay(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ExtendBookingRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Long-stay transition requested",
                modificationService.requestTransitionToLongStay(SecurityUtils.getCurrentUserId(), bookingId, request.newCheckOut())
        ));
    }

    @GetMapping("/{bookingId}/history")
    @Operation(summary = "View status transition history of a booking")
    public ResponseEntity<ApiResponse<java.util.List<BookingStatusHistory>>> getBookingHistory(
            @PathVariable UUID bookingId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Booking history fetched",
                modificationService.getHistory(bookingId)
        ));
    }
}
