package com.accommodation.platform.chat.repository;

import com.accommodation.platform.chat.entity.HostelTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HostelTicketRepository extends JpaRepository<HostelTicket, UUID> {
    List<HostelTicket> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
    List<HostelTicket> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
    long countByOwnerId(UUID ownerId);
    long countByOwnerIdAndStatusIn(UUID ownerId, List<String> statuses);
    long countByStatusIn(List<String> statuses);
}
