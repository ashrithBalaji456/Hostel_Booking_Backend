package com.accommodation.platform.support.controller;

import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.common.response.PagedResponse;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.security.UserPrincipal;
import com.accommodation.platform.support.entity.SupportMessage;
import com.accommodation.platform.support.entity.SupportTicket;
import com.accommodation.platform.support.service.SupportService;
import com.accommodation.platform.support.service.SupportService.CreateTicketRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Customer Support", description = "Support tickets and messaging for customers and admins")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/support/tickets")
    @Operation(summary = "Create a support ticket")
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Support ticket created",
                        supportService.createTicket(SecurityUtils.getCurrentUserId(), request)));
    }

    @GetMapping("/support/tickets")
    @Operation(summary = "List my support tickets")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicket>>> myTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tickets fetched",
                supportService.getMyTickets(SecurityUtils.getCurrentUserId(), page, size)));
    }

    @PostMapping("/support/tickets/{ticketId}/messages")
    @Operation(summary = "Add a message to a support ticket")
    public ResponseEntity<ApiResponse<SupportMessage>> addMessage(
            @PathVariable UUID ticketId,
            @RequestParam String message
    ) {
        UserPrincipal p = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Message sent",
                        supportService.addMessage(p.getId(), p.getRole(), ticketId, message)));
    }

    @GetMapping("/support/tickets/{ticketId}/messages")
    @Operation(summary = "Get messages for a support ticket")
    public ResponseEntity<ApiResponse<List<SupportMessage>>> getMessages(@PathVariable UUID ticketId) {
        UserPrincipal p = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Messages fetched",
                supportService.getMessages(p.getId(), p.getRole(), ticketId)));
    }

    @GetMapping("/admin/support/tickets")
    @Operation(summary = "List all support tickets (admin)")
    public ResponseEntity<ApiResponse<PagedResponse<SupportTicket>>> adminListTickets(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tickets fetched",
                supportService.listAllTickets(status, page, size)));
    }

    @PostMapping("/admin/support/tickets/{ticketId}/status")
    @Operation(summary = "Update support ticket status and resolution (admin)")
    public ResponseEntity<ApiResponse<SupportTicket>> updateTicket(
            @PathVariable UUID ticketId,
            @RequestParam String status,
            @RequestParam(required = false) String resolution
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Ticket updated",
                supportService.updateTicketByAdmin(SecurityUtils.getCurrentUserId(), ticketId, status, resolution)));
    }
}
