package com.accommodation.platform.safety.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.common.enums.ListingStatus;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.safety.entity.SafetyComplaint;
import com.accommodation.platform.safety.repository.SafetyComplaintRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SafetyComplaintService {

    private final SafetyComplaintRepository complaintRepo;
    private final PropertyRepository propertyRepository;
    private final AuditLogService auditLogService;

    public SafetyComplaintService(
            SafetyComplaintRepository complaintRepo,
            PropertyRepository propertyRepository,
            AuditLogService auditLogService
    ) {
        this.complaintRepo = complaintRepo;
        this.propertyRepository = propertyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public SafetyComplaint submitComplaint(UUID reporterId, CreateComplaintRequest req) {
        if (!propertyRepository.existsById(req.propertyId())) {
            throw new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND");
        }
        SafetyComplaint c = new SafetyComplaint();
        c.setComplaintReference("CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        c.setReporterId(reporterId);
        c.setPropertyId(req.propertyId());
        c.setBookingId(req.bookingId());
        c.setCategory(req.category().trim().toUpperCase());
        c.setDescription(req.description().trim());
        c.setEvidenceReferences(req.evidenceReferences());
        c.setStatus("OPEN");
        return complaintRepo.save(c);
    }

    @Transactional
    public SafetyComplaint resolveComplaint(
            UUID adminId, UUID complaintId, String status, String resolution, String adminNotes, boolean suspendProperty
    ) {
        SafetyComplaint c = complaintRepo.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Complaint not found", "COMPLAINT_NOT_FOUND"));
        c.setStatus(status.trim().toUpperCase());
        c.setResolution(resolution);
        c.setAdminNotes(adminNotes);
        c = complaintRepo.save(c);

        if (suspendProperty) {
            Property p = propertyRepository.findById(c.getPropertyId()).orElse(null);
            if (p != null) {
                p.setListingStatus(ListingStatus.SUSPENDED);
                propertyRepository.save(p);
            }
        }

        auditLogService.record(adminId, null, "SAFETY_COMPLAINT_REVIEWED", "SafetyComplaint", complaintId.toString(),
                "Status=" + c.getStatus() + ", SuspendedProperty=" + suspendProperty);
        return c;
    }

    @Transactional(readOnly = true)
    public PagedResponse<SafetyComplaint> listAll(String status, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return PagedResponse.from(status != null
                ? complaintRepo.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                : complaintRepo.findAll(pageable));
    }

    public record CreateComplaintRequest(
            @NotNull UUID propertyId,
            UUID bookingId,
            @NotBlank String category,
            @NotBlank String description,
            String evidenceReferences
    ) {}
}
