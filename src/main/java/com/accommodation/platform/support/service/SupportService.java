package com.accommodation.platform.support.service;

import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.support.entity.SupportMessage;
import com.accommodation.platform.support.entity.SupportTicket;
import com.accommodation.platform.support.repository.SupportMessageRepository;
import com.accommodation.platform.support.repository.SupportTicketRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupportService {

    private final SupportTicketRepository ticketRepo;
    private final SupportMessageRepository messageRepo;

    public SupportService(SupportTicketRepository ticketRepo, SupportMessageRepository messageRepo) {
        this.ticketRepo = ticketRepo;
        this.messageRepo = messageRepo;
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
    public PagedResponse<SupportTicket> listAllTickets(String status, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return PagedResponse.from(status != null
                ? ticketRepo.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable)
                : ticketRepo.findAll(pageable));
    }

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
