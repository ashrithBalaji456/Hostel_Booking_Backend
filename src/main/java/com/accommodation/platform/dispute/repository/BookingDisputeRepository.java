package com.accommodation.platform.dispute.repository;

import com.accommodation.platform.dispute.entity.BookingDispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingDisputeRepository extends JpaRepository<BookingDispute, UUID> {
    Page<BookingDispute> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
    Page<BookingDispute> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    long countByStatus(String status);
}
