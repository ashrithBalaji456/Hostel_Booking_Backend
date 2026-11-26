package com.accommodation.platform.user.dto;

import com.accommodation.platform.common.enums.AccountStatus;
import com.accommodation.platform.common.enums.Gender;
import com.accommodation.platform.common.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {}

    public record UserProfileResponse(
            UUID id,
            String fullName,
            String email,
            String phoneNumber,
            UserRole role,
            Gender gender,
            String profileImage,
            String occupationType,
            String organizationName,
            String courseOrRole,
            boolean emailVerified,
            AccountStatus accountStatus,
            Instant createdAt
    ) {}

    public record UpdateProfileRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120)
            String fullName,

            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid Indian phone number format")
            String phoneNumber,

            Gender gender,

            String profileImage,
            @Size(max = 40) String occupationType,
            @Size(max = 200) String organizationName,
            @Size(max = 150) String courseOrRole
    ) {}
}
