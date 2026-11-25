package com.accommodation.platform.checkin.repository;

import com.accommodation.platform.checkin.entity.CheckOutRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CheckOutRecordRepository extends JpaRepository<CheckOutRecord, UUID> {
    Optional<CheckOutRecord> findByBookingId(UUID bookingId);
}
