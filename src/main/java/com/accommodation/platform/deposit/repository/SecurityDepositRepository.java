package com.accommodation.platform.deposit.repository;

import com.accommodation.platform.deposit.entity.SecurityDeposit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecurityDepositRepository extends JpaRepository<SecurityDeposit, UUID> {
    Optional<SecurityDeposit> findByBookingId(UUID bookingId);
}
