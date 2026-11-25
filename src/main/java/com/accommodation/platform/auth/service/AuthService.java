package com.accommodation.platform.auth.service;

import com.accommodation.platform.auth.dto.AuthDtos.*;
import com.accommodation.platform.auth.entity.RefreshToken;
import com.accommodation.platform.auth.entity.VerificationToken;
import com.accommodation.platform.auth.repository.RefreshTokenRepository;
import com.accommodation.platform.auth.repository.VerificationTokenRepository;
import com.accommodation.platform.common.enums.AccountStatus;
import com.accommodation.platform.common.enums.TokenType;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ConflictException;
import com.accommodation.platform.common.exception.UnauthorizedException;
import com.accommodation.platform.config.AppProperties;
import com.accommodation.platform.security.JwtService;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == UserRole.ADMIN) {
            throw new BadRequestException("Self-registration as ADMIN is not permitted", "INVALID_ROLE");
        }
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists", "DUPLICATE_EMAIL");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhoneNumber(request.phoneNumber().trim());
        user.setRole(request.role());
        user.setGender(request.gender());
        user.setEmailVerified(false);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userRepository.save(user);

        String verificationToken = createVerificationToken(user, TokenType.EMAIL_VERIFICATION, 24);
        return issueTokens(user, verificationToken);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password", "INVALID_CREDENTIALS");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Account is " + user.getAccountStatus().name().toLowerCase(), "ACCOUNT_INACTIVE");
        }

        return issueTokens(user, null);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String tokenHash = sha256(request.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token", "INVALID_REFRESH_TOKEN"));

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked", "INVALID_REFRESH_TOKEN");
        }

        User user = existing.getUser();
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active", "ACCOUNT_INACTIVE");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return issueTokens(user, null);
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public TokenMessageResponse forgotPassword(ForgotPasswordRequest request) {
        return userRepository.findByEmailIgnoreCase(request.email().trim())
                .map(user -> {
                    String resetToken = createVerificationToken(user, TokenType.PASSWORD_RESET, 1);
                    return new TokenMessageResponse("Password reset instructions generated", resetToken);
                })
                .orElse(new TokenMessageResponse("If the email exists, password reset instructions have been sent", null));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken vt = verificationTokenRepository
                .findByTokenAndTokenType(request.token(), TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid password reset token", "INVALID_TOKEN"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token is expired or already used", "TOKEN_EXPIRED");
        }

        User user = vt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        vt.setUsed(true);
        verificationTokenRepository.save(vt);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        VerificationToken vt = verificationTokenRepository
                .findByTokenAndTokenType(request.token(), TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new BadRequestException("Invalid verification token", "INVALID_TOKEN"));

        if (vt.isUsed() || vt.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Verification token is expired or already used", "TOKEN_EXPIRED");
        }

        User user = vt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        vt.setUsed(true);
        verificationTokenRepository.save(vt);
    }

    private AuthResponse issueTokens(User user, String verificationToken) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String rawRefreshToken = generateOpaqueToken();

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(sha256(rawRefreshToken));
        rt.setExpiresAt(Instant.now().plusMillis(appProperties.jwt().refreshTokenExpirationMs()));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                appProperties.jwt().accessTokenExpirationMs(),
                new UserSummary(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getPhoneNumber(),
                        user.getRole(),
                        user.getGender(),
                        user.isEmailVerified()
                ),
                verificationToken
        );
    }

    private String createVerificationToken(User user, TokenType type, int hoursValid) {
        String token = generateOpaqueToken();
        VerificationToken vt = new VerificationToken();
        vt.setUser(user);
        vt.setToken(token);
        vt.setTokenType(type);
        vt.setExpiresAt(Instant.now().plus(hoursValid, ChronoUnit.HOURS));
        vt.setUsed(false);
        verificationTokenRepository.save(vt);
        return token;
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
