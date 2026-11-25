package com.accommodation.platform.checkin.repository;

import com.accommodation.platform.checkin.entity.CheckInRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckInRecordRepository extends JpaRepository<CheckInRecord, UUID> {
    Optional<CheckInRecord> findByBookingId(UUID bookingId);
}
