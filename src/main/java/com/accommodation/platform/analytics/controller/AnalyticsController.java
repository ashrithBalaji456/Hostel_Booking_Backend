package com.accommodation.platform.analytics.controller;

import com.accommodation.platform.analytics.service.AnalyticsService;
import com.accommodation.platform.analytics.service.AnalyticsService.AdminAnalyticsResponse;
import com.accommodation.platform.analytics.service.AnalyticsService.OwnerAnalyticsResponse;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Analytics & Reports", description = "Owner and admin financial and operational analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/owner/analytics")
    @Operation(summary = "Get owner booking and revenue analytics")
    public ResponseEntity<ApiResponse<OwnerAnalyticsResponse>> getOwnerAnalytics() {
        return ResponseEntity.ok(ApiResponse.ok("Owner analytics fetched",
                analyticsService.getOwnerAnalytics(SecurityUtils.getCurrentUserId())));
    }

    @GetMapping("/admin/analytics")
    @Operation(summary = "Get platform-wide admin analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> getAdminAnalytics() {
        return ResponseEntity.ok(ApiResponse.ok("Admin analytics fetched",
                analyticsService.getAdminAnalytics()));
    }
}
