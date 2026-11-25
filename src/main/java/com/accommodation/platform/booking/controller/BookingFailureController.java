package com.accommodation.platform.booking.controller;

import com.accommodation.platform.booking.entity.BookingFailure;
import com.accommodation.platform.booking.service.BookingFailureService;
import com.accommodation.platform.booking.service.BookingFailureService.*;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Booking Failures & Alternatives", description = "Handle arrival bed unavailability, alternative recommendations, or full refund")
public class BookingFailureController {

    private final BookingFailureService failureService;

    public BookingFailureController(BookingFailureService failureService) {
        this.failureService = failureService;
    }

    @PostMapping("/{bookingId}/failure")
    @Operation(summary = "Report booking fulfillment failure and fetch real available alternatives")
    public ResponseEntity<ApiResponse<FailureWithAlternativesResponse>> reportFailure(
            @PathVariable UUID bookingId,
            @Valid @RequestBody ReportFailureRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Booking failure recorded",
                        failureService.reportFailure(SecurityUtils.getCurrentUserId(), bookingId, request)));
    }

    @PostMapping("/failures/{failureId}/respond")
    @Operation(summary = "Accept an alternative property or reject for a full refund")
    public ResponseEntity<ApiResponse<BookingFailure>> respond(
            @PathVariable UUID failureId,
            @RequestBody RespondAlternativeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Response recorded",
                failureService.respondToAlternative(
                        SecurityUtils.getCurrentUserId(), failureId, request.accepted(),
                        request.alternativePropertyId(), request.alternativeBedId())));
    }
}
