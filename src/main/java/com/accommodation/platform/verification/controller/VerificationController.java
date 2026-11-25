package com.accommodation.platform.verification.controller;

import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.verification.service.VerificationService;
import com.accommodation.platform.verification.service.VerificationService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Verification", description = "Owner KYC and property verification document submission and admin review")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/owner/verifications")
    @Operation(summary = "Submit owner KYC verification documents")
    public ResponseEntity<ApiResponse<OwnerVerificationResponse>> submitOwnerVerification(
            @Valid @RequestBody SubmitOwnerVerificationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Owner verification submitted",
                        verificationService.submitOwnerVerification(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/owner/verifications")
    @Operation(summary = "View my owner verification submissions")
    public ResponseEntity<ApiResponse<List<OwnerVerificationResponse>>> getMyVerifications() {
        return ResponseEntity.ok(ApiResponse.ok("Owner verifications fetched",
                verificationService.getOwnerVerifications(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/owner/properties/{propertyId}/verification")
    @Operation(summary = "Submit or resubmit property verification documents")
    public ResponseEntity<ApiResponse<PropertyVerificationResponse>> submitPropertyVerification(
            @PathVariable UUID propertyId,
            @Valid @RequestBody SubmitPropertyVerificationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Property verification submitted",
                        verificationService.submitPropertyVerification(SecurityUtils.getCurrentUserId(), propertyId, request)));
    }

    @GetMapping("/admin/owner-verifications")
    @Operation(summary = "List owner verifications for admin review")
    public ResponseEntity<ApiResponse<PagedResponse<OwnerVerificationResponse>>> listOwnerVerifications(
            @RequestParam(required = false) VerificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Owner verifications fetched",
                verificationService.listByStatus(status, page, size)));
    }

    @PostMapping("/admin/owner-verifications/{verificationId}/review")
    @Operation(summary = "Approve or reject an owner verification")
    public ResponseEntity<ApiResponse<OwnerVerificationResponse>> reviewOwnerVerification(
            @PathVariable UUID verificationId,
            @RequestBody ReviewVerificationRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Owner verification reviewed",
                verificationService.reviewOwnerVerification(
                        SecurityUtils.getCurrentUserId(), verificationId, request.approved(), request.notes())));
    }
}
