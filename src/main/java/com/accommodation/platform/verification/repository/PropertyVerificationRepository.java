package com.accommodation.platform.verification.repository;

import com.accommodation.platform.verification.entity.PropertyVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PropertyVerificationRepository extends JpaRepository<PropertyVerification, UUID> {
    List<PropertyVerification> findByPropertyIdOrderBySubmittedAtDesc(UUID propertyId);
}
