package com.accommodation.platform.dashboard.controller;

import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.common.enums.AccountStatus;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.PaymentStatus;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.dashboard.dto.DashboardDtos.*;
import com.accommodation.platform.dashboard.service.AdminDashboardService;
import com.accommodation.platform.payment.dto.PaymentDtos.PaymentResponse;
import com.accommodation.platform.property.dto.PropertyDtos.PropertyDetailResponse;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.user.dto.UserDtos.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Dashboard", description = "Platform administration, property verification, moderation, and analytics")
public class AdminDashboardController {

    private final AdminDashboardService adminService;

    public AdminDashboardController(AdminDashboardService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    @Operation(summary = "List and filter platform users")
    public ResponseEntity<ApiResponse<PagedResponse<UserProfileResponse>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Users fetched",
                adminService.listUsers(role, status, page, size)
        ));
    }

    @PostMapping("/users/{userId}/status")
    @Operation(summary = "Update user account status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "User status updated",
                adminService.updateUserStatus(SecurityUtils.getCurrentUserId(), userId, request.status())
        ));
    }

    @GetMapping("/properties")
    @Operation(summary = "List properties with optional verification status filter")
    public ResponseEntity<ApiResponse<PagedResponse<PropertyDetailResponse>>> listProperties(
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Properties fetched",
                adminService.listProperties(verificationStatus, page, size)
        ));
    }

    @PostMapping("/properties/{propertyId}/approve")
    @Operation(summary = "Approve a property listing")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> approveProperty(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Property approved",
                adminService.approveProperty(SecurityUtils.getCurrentUserId(), propertyId)
        ));
    }

    @PostMapping("/properties/{propertyId}/reject")
    @Operation(summary = "Reject a property listing")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> rejectProperty(
            @PathVariable UUID propertyId,
            @Valid @RequestBody RejectPropertyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Property rejected",
                adminService.rejectProperty(SecurityUtils.getCurrentUserId(), propertyId, request.reason())
        ));
    }

    @PostMapping("/properties/{propertyId}/suspend")
    @Operation(summary = "Suspend an active property listing")
    public ResponseEntity<ApiResponse<PropertyDetailResponse>> suspendProperty(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Property suspended",
                adminService.suspendProperty(SecurityUtils.getCurrentUserId(), propertyId)
        ));
    }

    @GetMapping("/bookings")
    @Operation(summary = "List all bookings on the platform")
    public ResponseEntity<ApiResponse<PagedResponse<BookingResponse>>> listBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Bookings fetched",
                adminService.listBookings(status, page, size)
        ));
    }

    @GetMapping("/payments")
    @Operation(summary = "List all payments on the platform")
    public ResponseEntity<ApiResponse<PagedResponse<PaymentResponse>>> listPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Payments fetched",
                adminService.listPayments(status, page, size)
        ));
    }

    @PostMapping("/reviews/{reviewId}/moderate")
    @Operation(summary = "Hide or unhide a review")
    public ResponseEntity<ApiResponse<Void>> moderateReview(
            @PathVariable UUID reviewId,
            @RequestBody ModerateReviewRequest request
    ) {
        adminService.moderateReview(SecurityUtils.getCurrentUserId(), reviewId, request.hidden());
        return ResponseEntity.ok(ApiResponse.ok("Review moderation updated"));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get platform-wide admin analytics")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok("Admin dashboard fetched", adminService.getDashboard()));
    }
}
