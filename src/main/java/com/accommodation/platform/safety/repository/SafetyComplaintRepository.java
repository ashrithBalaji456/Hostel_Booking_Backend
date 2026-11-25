package com.accommodation.platform.safety.repository;

import com.accommodation.platform.safety.entity.SafetyComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SafetyComplaintRepository extends JpaRepository<SafetyComplaint, UUID> {
    Page<SafetyComplaint> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    long countByStatus(String status);
}
