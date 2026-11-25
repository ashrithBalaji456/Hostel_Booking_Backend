package com.accommodation.platform.dashboard.dto;

import com.accommodation.platform.booking.dto.BookingDtos.BookingResponse;
import com.accommodation.platform.common.enums.AccountStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class DashboardDtos {

    private DashboardDtos() {}

    public record OwnerDashboardResponse(
            long totalProperties,
            long pendingBookings,
            long confirmedBookings,
            BigDecimal netEarnings,
            List<BookingResponse> upcomingCheckIns
    ) {}

    public record AdminDashboardResponse(
            long totalUsers,
            long totalCustomers,
            long totalOwners,
            long totalProperties,
            long pendingPropertyVerifications,
            long totalBookings,
            long confirmedBookings,
            BigDecimal netPlatformRevenue
    ) {}

    public record RejectPropertyRequest(
            @NotBlank(message = "Rejection reason is required")
            String reason
    ) {}

    public record UpdateAccountStatusRequest(
            @NotNull(message = "Account status is required")
            AccountStatus status
    ) {}

    public record ModerateReviewRequest(
            boolean hidden
    ) {}
}
