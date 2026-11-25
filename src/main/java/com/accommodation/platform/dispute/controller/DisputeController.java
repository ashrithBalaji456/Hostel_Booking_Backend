package com.accommodation.platform.dispute.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.dispute.entity.BookingDispute;
import com.accommodation.platform.dispute.service.DisputeService;
import com.accommodation.platform.dispute.service.DisputeService.CreateDisputeRequest;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Booking Disputes", description = "Raise and resolve booking disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping("/disputes")
    @Operation(summary = "Raise a booking dispute")
    public ResponseEntity<ApiResponse<BookingDispute>> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Dispute raised",
                        disputeService.createDispute(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/disputes")
    @Operation(summary = "List my booking disputes")
    public ResponseEntity<ApiResponse<PagedResponse<BookingDispute>>> myDisputes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Disputes fetched",
                disputeService.getMyDisputes(SecurityUtils.getCurrentUserId(), page, size)));
    }

    @GetMapping("/admin/disputes")
    @Operation(summary = "List all booking disputes (admin)")
    public ResponseEntity<ApiResponse<PagedResponse<BookingDispute>>> adminListDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Disputes fetched",
                disputeService.listAll(status, page, size)));
    }

    @PostMapping("/admin/disputes/{disputeId}/resolve")
    @Operation(summary = "Resolve or update a booking dispute (admin)")
    public ResponseEntity<ApiResponse<BookingDispute>> resolveDispute(
            @PathVariable UUID disputeId,
            @RequestParam String status,
            @RequestParam(required = false) String resolution,
            @RequestParam(required = false) String adminNotes
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Dispute updated",
                disputeService.resolveDispute(SecurityUtils.getCurrentUserId(), disputeId, status, resolution, adminNotes)));
    }
}
