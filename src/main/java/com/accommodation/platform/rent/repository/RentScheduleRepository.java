package com.accommodation.platform.rent.repository;

import com.accommodation.platform.rent.entity.RentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RentScheduleRepository extends JpaRepository<RentSchedule, UUID> {
    List<RentSchedule> findByBookingIdOrderByInstallmentNumberAsc(UUID bookingId);
}
