package com.accommodation.platform.verification.repository;

import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.verification.entity.OwnerVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OwnerVerificationRepository extends JpaRepository<OwnerVerification, UUID> {
    List<OwnerVerification> findByOwnerIdOrderBySubmittedAtDesc(UUID ownerId);
    Page<OwnerVerification> findByVerificationStatusOrderBySubmittedAtDesc(VerificationStatus status, Pageable pageable);
}
