package com.accommodation.platform.payout.repository;

import com.accommodation.platform.payout.entity.OwnerCommission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OwnerCommissionRepository extends JpaRepository<OwnerCommission, UUID> {
    Optional<OwnerCommission> findByOwnerId(UUID ownerId);
}
