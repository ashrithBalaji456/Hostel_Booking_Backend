package com.accommodation.platform.verification.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.common.enums.VerificationStatus;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import com.accommodation.platform.verification.entity.OwnerVerification;
import com.accommodation.platform.verification.entity.PropertyVerification;
import com.accommodation.platform.verification.repository.OwnerVerificationRepository;
import com.accommodation.platform.verification.repository.PropertyVerificationRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VerificationService {

    private final OwnerVerificationRepository ownerVerificationRepo;
    private final PropertyVerificationRepository propertyVerificationRepo;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final AuditLogService auditLogService;

    public VerificationService(
            OwnerVerificationRepository ownerVerificationRepo,
            PropertyVerificationRepository propertyVerificationRepo,
            UserRepository userRepository,
            PropertyRepository propertyRepository,
            AuditLogService auditLogService
    ) {
        this.ownerVerificationRepo = ownerVerificationRepo;
        this.propertyVerificationRepo = propertyVerificationRepo;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OwnerVerificationResponse submitOwnerVerification(UUID ownerId, SubmitOwnerVerificationRequest req) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", "USER_NOT_FOUND"));

        OwnerVerification v = new OwnerVerification();
        v.setOwner(owner);
        v.setOwnerName(req.ownerName().trim());
        v.setContactDetails(req.contactDetails().trim());
        v.setDocumentType(req.documentType().trim());
        v.setDocumentReference(req.documentReference().trim());
        v.setVerificationStatus(VerificationStatus.PENDING);
        v = ownerVerificationRepo.save(v);

        return toOwnerResponse(v);
    }

    @Transactional
    public PropertyVerificationResponse submitPropertyVerification(UUID ownerId, UUID propertyId, SubmitPropertyVerificationRequest req) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));
        if (!property.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You do not own this property");
        }

        PropertyVerification pv = new PropertyVerification();
        pv.setProperty(property);
        pv.setDocumentReference(req.documentReference().trim());
        pv.setVerificationStatus(VerificationStatus.PENDING);
        pv = propertyVerificationRepo.save(pv);

        property.setVerificationStatus(VerificationStatus.PENDING);
        property.setRejectionReason(null);
        propertyRepository.save(property);

        return toPropertyResponse(pv);
    }

    @Transactional
    public OwnerVerificationResponse reviewOwnerVerification(UUID adminId, UUID verificationId, boolean approved, String notes) {
        OwnerVerification v = ownerVerificationRepo.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Verification record not found", "VERIFICATION_NOT_FOUND"));
        v.setVerificationStatus(approved ? VerificationStatus.APPROVED : VerificationStatus.REJECTED);
        v.setVerificationNotes(notes);
        v.setReviewedAt(Instant.now());
        v.setReviewedBy(adminId);
        v = ownerVerificationRepo.save(v);

        auditLogService.record(adminId, null, "OWNER_VERIFICATION_REVIEWED", "OwnerVerification",
                verificationId.toString(), "Status=" + v.getVerificationStatus());
        return toOwnerResponse(v);
    }

    @Transactional(readOnly = true)
    public List<OwnerVerificationResponse> getOwnerVerifications(UUID ownerId) {
        return ownerVerificationRepo.findByOwnerIdOrderBySubmittedAtDesc(ownerId)
                .stream().map(this::toOwnerResponse).toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<OwnerVerificationResponse> listByStatus(VerificationStatus status, int page, int size) {
        return PagedResponse.from(
                ownerVerificationRepo.findByVerificationStatusOrderBySubmittedAtDesc(
                        status != null ? status : VerificationStatus.PENDING,
                        PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))
                ).map(this::toOwnerResponse)
        );
    }

    private OwnerVerificationResponse toOwnerResponse(OwnerVerification v) {
        return new OwnerVerificationResponse(
                v.getId(), v.getOwner().getId(), v.getOwnerName(), v.getContactDetails(),
                v.getDocumentType(), v.getDocumentReference(), v.getVerificationStatus(),
                v.getVerificationNotes(), v.getSubmittedAt(), v.getReviewedAt()
        );
    }

    private PropertyVerificationResponse toPropertyResponse(PropertyVerification pv) {
        return new PropertyVerificationResponse(
                pv.getId(), pv.getProperty().getId(), pv.isAddressVerified(),
                pv.getDocumentReference(), pv.getVerificationStatus(),
                pv.getRejectionReason(), pv.getSubmittedAt(), pv.getReviewedAt()
        );
    }

    public record SubmitOwnerVerificationRequest(
            @NotBlank String ownerName,
            @NotBlank String contactDetails,
            @NotBlank String documentType,
            @NotBlank String documentReference
    ) {}

    public record SubmitPropertyVerificationRequest(
            @NotBlank String documentReference
    ) {}

    public record ReviewVerificationRequest(
            boolean approved,
            String notes
    ) {}

    public record OwnerVerificationResponse(
            UUID id,
            UUID ownerId,
            String ownerName,
            String contactDetails,
            String documentType,
            String documentReference,
            VerificationStatus verificationStatus,
            String verificationNotes,
            Instant submittedAt,
            Instant reviewedAt
    ) {}

    public record PropertyVerificationResponse(
            UUID id,
            UUID propertyId,
            boolean addressVerified,
            String documentReference,
            VerificationStatus verificationStatus,
            String rejectionReason,
            Instant submittedAt,
            Instant reviewedAt
    ) {}
}
