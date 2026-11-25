package com.accommodation.platform.dispute.service;

import com.accommodation.platform.audit.AuditLogService;
import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.exception.BadRequestException;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.dispute.entity.BookingDispute;
import com.accommodation.platform.dispute.repository.BookingDisputeRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DisputeService {

    private final BookingDisputeRepository disputeRepo;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    public DisputeService(
            BookingDisputeRepository disputeRepo,
            BookingRepository bookingRepository,
            AuditLogService auditLogService
    ) {
        this.disputeRepo = disputeRepo;
        this.bookingRepository = bookingRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public BookingDispute createDispute(UUID customerId, CreateDisputeRequest req) {
        Booking booking = bookingRepository.findById(req.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found", "BOOKING_NOT_FOUND"));
        if (!booking.getUser().getId().equals(customerId)) {
            throw new ForbiddenException("You can only dispute your own bookings");
        }

        BookingDispute d = new BookingDispute();
        d.setDisputeReference("DSP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        d.setBookingId(booking.getId());
        d.setCustomerId(customerId);
        d.setPropertyId(booking.getProperty().getId());
        d.setCategory(req.category().trim().toUpperCase());
        d.setDescription(req.description().trim());
        d.setEvidenceReferences(req.evidenceReferences());
        d.setStatus("OPEN");
        return disputeRepo.save(d);
    }

    @Transactional
    public BookingDispute resolveDispute(UUID adminId, UUID disputeId, String status, String resolution, String adminNotes) {
        BookingDispute d = disputeRepo.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found", "DISPUTE_NOT_FOUND"));
        if ("CLOSED".equals(d.getStatus())) {
            throw new BadRequestException("Closed disputes cannot be modified without reopening", "DISPUTE_CLOSED");
        }
        d.setStatus(status.trim().toUpperCase());
        d.setResolution(resolution);
        d.setAdminNotes(adminNotes);
        d = disputeRepo.save(d);

        auditLogService.record(adminId, null, "DISPUTE_UPDATED", "BookingDispute", disputeId.toString(), "Status=" + d.getStatus());
        return d;
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingDispute> getMyDisputes(UUID customerId, int page, int size) {
        return PagedResponse.from(disputeRepo.findByCustomerIdOrderByCreatedAtDesc(
                customerId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))));
    }

    @Transactional(readOnly = true)
    public PagedResponse<BookingDispute> listAll(String status, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return PagedResponse.from(status != null
                ? disputeRepo.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                : disputeRepo.findAll(pageable));
    }

    public record CreateDisputeRequest(
            @NotNull UUID bookingId,
            @NotBlank String category,
            @NotBlank String description,
            String evidenceReferences
    ) {}
}
