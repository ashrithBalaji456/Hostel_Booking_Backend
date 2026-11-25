package com.accommodation.platform.support.repository;

import com.accommodation.platform.support.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
