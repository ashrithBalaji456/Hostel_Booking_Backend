package com.accommodation.platform.auth.dto;

import com.accommodation.platform.common.enums.Gender;
import com.accommodation.platform.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank(message = "Full name is required")
            @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
            String fullName,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-]).{8,72}$",
                    message = "Password must be 8-72 characters with uppercase, lowercase, digit, and special character"
            )
            String password,

            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "^(\\+91)?[6-9]\\d{9}$", message = "Invalid Indian phone number format")
            String phoneNumber,

            @NotNull(message = "Role is required")
            UserRole role,

            Gender gender,
            @Size(max = 40) String occupationType,
            @Size(max = 200) String organizationName,
            @Size(max = 150) String courseOrRole
    ) {
        public RegisterRequest(String fullName, String email, String password, String phoneNumber, UserRole role, Gender gender) {
            this(fullName, email, password, phoneNumber, role, gender, null, null, null);
        }
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {}

    public record RefreshRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record LogoutRequest(
            @NotBlank(message = "Refresh token is required")
            String refreshToken
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Reset token is required")
            String token,

            @NotBlank(message = "New password is required")
            @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=-]).{8,72}$",
                    message = "Password must be 8-72 characters with uppercase, lowercase, digit, and special character"
            )
            String newPassword
    ) {}

    public record VerifyEmailRequest(
            @NotBlank(message = "Verification token is required")
            String token
    ) {}

    public record UserSummary(
            UUID id,
            String fullName,
            String email,
            String phoneNumber,
            UserRole role,
            Gender gender,
            boolean emailVerified
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresInMs,
            UserSummary user,
            String verificationToken
    ) {}

    public record TokenMessageResponse(
            String message,
            String token
    ) {}
}
