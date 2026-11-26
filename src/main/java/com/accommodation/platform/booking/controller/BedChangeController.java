package com.accommodation.platform.booking.controller;

import com.accommodation.platform.booking.service.BedChangeService;
import com.accommodation.platform.booking.service.BedChangeService.BedChangeRequestResponse;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bed Change Requests", description = "Internal room and bed change requests for active bookings")
public class BedChangeController {

    public record CreateBedChangeRequest(@NotNull UUID requestedBedId, String reason) {}
    public record ResolveBedChangeRequest(String ownerNote) {}

    private final BedChangeService bedChangeService;

    public BedChangeController(BedChangeService bedChangeService) {
        this.bedChangeService = bedChangeService;
    }

    @PostMapping("/bookings/{bookingId}/bed-change-requests")
    @Operation(summary = "Customer requests an internal room or bed change")
    public ResponseEntity<ApiResponse<BedChangeRequestResponse>> createRequest(
            @PathVariable UUID bookingId,
            @Valid @RequestBody CreateBedChangeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Bed change request submitted",
                bedChangeService.requestBedChange(SecurityUtils.getCurrentUserId(), bookingId, request.requestedBedId(), request.reason())));
    }

    @GetMapping("/bookings/{bookingId}/bed-change-requests")
    @Operation(summary = "List bed change requests for a customer booking")
    public ResponseEntity<ApiResponse<List<BedChangeRequestResponse>>> getCustomerRequests(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ApiResponse.ok("Bed change requests fetched",
                bedChangeService.getCustomerRequests(SecurityUtils.getCurrentUserId(), bookingId)));
    }

    @GetMapping("/owner/bed-change-requests")
    @Operation(summary = "List bed change requests for owner's properties")
    public ResponseEntity<ApiResponse<List<BedChangeRequestResponse>>> getOwnerRequests() {
        return ResponseEntity.ok(ApiResponse.ok("Owner bed change requests fetched",
                bedChangeService.getOwnerRequests(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/owner/bed-change-requests/{requestId}/approve")
    @Operation(summary = "Owner approves bed change request and swaps guest to new bed")
    public ResponseEntity<ApiResponse<BedChangeRequestResponse>> approveRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ResolveBedChangeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Bed change approved",
                bedChangeService.approveRequest(SecurityUtils.getCurrentUserId(), requestId, request != null ? request.ownerNote() : null)));
    }

    @PostMapping("/owner/bed-change-requests/{requestId}/reject")
    @Operation(summary = "Owner rejects bed change request")
    public ResponseEntity<ApiResponse<BedChangeRequestResponse>> rejectRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ResolveBedChangeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Bed change rejected",
                bedChangeService.rejectRequest(SecurityUtils.getCurrentUserId(), requestId, request != null ? request.ownerNote() : null)));
    }
}
