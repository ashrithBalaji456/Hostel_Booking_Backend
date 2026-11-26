package com.accommodation.platform.support.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.support.entity.SupportMessage;
import com.accommodation.platform.support.entity.SupportTicket;
import com.accommodation.platform.support.repository.SupportMessageRepository;
import com.accommodation.platform.support.repository.SupportTicketRepository;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SupportService {

    private final SupportTicketRepository ticketRepo;
    private final SupportMessageRepository messageRepo;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    public SupportService(
            SupportTicketRepository ticketRepo,
            SupportMessageRepository messageRepo,
            UserRepository userRepository,
            BookingRepository bookingRepository
    ) {
        this.ticketRepo = ticketRepo;
        this.messageRepo = messageRepo;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public SupportTicket createTicket(UUID userId, CreateTicketRequest req) {
        SupportTicket ticket = new SupportTicket();
        ticket.setTicketReference("TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setUserId(userId);
        ticket.setBookingId(req.bookingId());
        ticket.setCategory(req.category().trim().toUpperCase());
        ticket.setSubject(req.subject().trim());
        ticket.setStatus("OPEN");
        ticket = ticketRepo.save(ticket);

        SupportMessage msg = new SupportMessage();
        msg.setTicketId(ticket.getId());
        msg.setSenderId(userId);
        msg.setMessage(req.message().trim());
        messageRepo.save(msg);

        return ticket;
    }

    @Transactional
    public SupportMessage addMessage(UUID actorId, UserRole role, UUID ticketId, String message) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found", "TICKET_NOT_FOUND"));
        if (role != UserRole.ADMIN && !ticket.getUserId().equals(actorId)) {
            throw new ForbiddenException("Access denied");
        }
        SupportMessage msg = new SupportMessage();
        msg.setTicketId(ticketId);
        msg.setSenderId(actorId);
        msg.setMessage(message.trim());
        return messageRepo.save(msg);
    }

    @Transactional
    public SupportTicket updateTicketByAdmin(UUID adminId, UUID ticketId, String status, String resolution) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found", "TICKET_NOT_FOUND"));
        ticket.setAssignedAdminId(adminId);
        ticket.setStatus(status.trim().toUpperCase());
        if (resolution != null) ticket.setResolution(resolution);
        return ticketRepo.save(ticket);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SupportTicket> getMyTickets(UUID userId, int page, int size) {
        return PagedResponse.from(ticketRepo.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))));
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminSupportTicketResponse> listAllTickets(String status, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        var pageRes = (status != null && !status.isBlank())
                ? ticketRepo.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable)
                : ticketRepo.findAll(pageable);
        return PagedResponse.from(pageRes.map(this::toAdminResponse));
    }

    private AdminSupportTicketResponse toAdminResponse(SupportTicket t) {
        User user = userRepository.findById(t.getUserId()).orElse(null);
        Booking booking = t.getBookingId() != null ? bookingRepository.findById(t.getBookingId()).orElse(null) : null;
        List<SupportMessage> msgs = messageRepo.findByTicketIdOrderByCreatedAtAsc(t.getId());
        String initialMsg = msgs.isEmpty() ? null : msgs.get(0).getMessage();

        return new AdminSupportTicketResponse(
                t.getId(),
                t.getTicketReference(),
                t.getUserId(),
                user != null ? user.getFullName() : "Unknown User",
                user != null ? user.getEmail() : null,
                user != null ? user.getPhoneNumber() : null,
                user != null && user.getRole() != null ? user.getRole().name() : null,
                t.getBookingId(),
                booking != null ? booking.getBookingReference() : null,
                booking != null && booking.getProperty() != null ? booking.getProperty().getName() : null,
                t.getCategory(),
                t.getSubject(),
                initialMsg,
                t.getStatus(),
                t.getResolution(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public record AdminSupportTicketResponse(
            UUID id,
            String ticketReference,
            UUID userId,
            String userName,
            String userEmail,
            String userPhone,
            String userRole,
            UUID bookingId,
            String bookingReference,
            String propertyName,
            String category,
            String subject,
            String initialMessage,
            String status,
            String resolution,
            Instant createdAt,
            Instant updatedAt
    ) {}

    @Transactional(readOnly = true)
    public List<SupportMessage> getMessages(UUID actorId, UserRole role, UUID ticketId) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found", "TICKET_NOT_FOUND"));
        if (role != UserRole.ADMIN && !ticket.getUserId().equals(actorId)) {
            throw new ForbiddenException("Access denied");
        }
        return messageRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public record CreateTicketRequest(
            UUID bookingId,
            @NotBlank String category,
            @NotBlank String subject,
            @NotBlank String message
    ) {}
}
