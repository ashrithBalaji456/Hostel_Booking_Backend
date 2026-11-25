package com.accommodation.platform.payout.repository;

import com.accommodation.platform.payout.entity.OwnerPayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OwnerPayoutRepository extends JpaRepository<OwnerPayout, UUID> {
    Page<OwnerPayout> findByOwnerIdOrderByRequestedAtDesc(UUID ownerId, Pageable pageable);
}
