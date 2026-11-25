package com.accommodation.platform.auth.repository;

import com.accommodation.platform.auth.entity.VerificationToken;
import com.accommodation.platform.common.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByTokenAndTokenType(String token, TokenType tokenType);
}
