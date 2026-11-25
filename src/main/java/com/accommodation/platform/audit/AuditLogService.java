package com.accommodation.platform.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID actorId, String actorEmail, String action, String entityType, String entityId, String details) {
        auditLogRepository.save(new AuditLog(actorId, actorEmail, action, entityType, entityId, details));
    }
}
