package com.accommodation.platform.safety.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.safety.entity.SafetyComplaint;
import com.accommodation.platform.safety.service.SafetyComplaintService;
import com.accommodation.platform.safety.service.SafetyComplaintService.CreateComplaintRequest;
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
@Tag(name = "Safety & Complaints", description = "Private safety complaint reporting and admin investigation")
public class SafetyComplaintController {

    private final SafetyComplaintService complaintService;

    public SafetyComplaintController(SafetyComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping("/safety/complaints")
    @Operation(summary = "Submit a private safety or property complaint")
    public ResponseEntity<ApiResponse<SafetyComplaint>> submitComplaint(@Valid @RequestBody CreateComplaintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Safety complaint submitted",
                        complaintService.submitComplaint(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/admin/safety/complaints")
    @Operation(summary = "List safety complaints (admin)")
    public ResponseEntity<ApiResponse<PagedResponse<SafetyComplaint>>> listComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Safety complaints fetched",
                complaintService.listAll(status, page, size)));
    }

    @PostMapping("/admin/safety/complaints/{complaintId}/resolve")
    @Operation(summary = "Resolve a safety complaint and optionally suspend the property (admin)")
    public ResponseEntity<ApiResponse<SafetyComplaint>> resolveComplaint(
            @PathVariable UUID complaintId,
            @RequestParam String status,
            @RequestParam(required = false) String resolution,
            @RequestParam(required = false) String adminNotes,
            @RequestParam(defaultValue = "false") boolean suspendProperty
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Safety complaint updated",
                complaintService.resolveComplaint(
                        SecurityUtils.getCurrentUserId(), complaintId, status, resolution, adminNotes, suspendProperty)));
    }
}
