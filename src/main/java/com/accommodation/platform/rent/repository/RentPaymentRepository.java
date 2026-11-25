package com.accommodation.platform.rent.repository;

import com.accommodation.platform.rent.entity.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RentPaymentRepository extends JpaRepository<RentPayment, UUID> {
    List<RentPayment> findByRentScheduleIdOrderByPaidAtDesc(UUID rentScheduleId);
}
