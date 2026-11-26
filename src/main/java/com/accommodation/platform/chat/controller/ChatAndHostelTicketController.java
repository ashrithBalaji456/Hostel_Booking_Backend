package com.accommodation.platform.chat.controller;

import com.accommodation.platform.chat.service.ChatAndHostelTicketService;
import com.accommodation.platform.chat.service.ChatAndHostelTicketService.*;
import com.accommodation.platform.common.response.ApiResponse;
import com.accommodation.platform.security.SecurityUtils;
import com.accommodation.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Chat & Hostel Tickets", description = "Isolated customer-owner WhatsApp-style chat and resident hostel tickets")
public class ChatAndHostelTicketController {

    private final ChatAndHostelTicketService service;

    public ChatAndHostelTicketController(ChatAndHostelTicketService service) {
        this.service = service;
    }

    @PostMapping("/chat/conversations")
    @Operation(summary = "Start or get conversation with property owner")
    public ResponseEntity<ApiResponse<ConversationResponse>> startConversation(@RequestParam UUID propertyId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Conversation ready",
                service.startOrGetConversation(SecurityUtils.getCurrentUserId(), propertyId)
        ));
    }

    @GetMapping("/chat/conversations")
    @Operation(summary = "List my chat conversations")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> listConversations() {
        UserPrincipal p = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok(
                "Conversations fetched",
                service.listMyConversations(p.getId(), p.getRole())
        ));
    }

    @GetMapping("/chat/conversations/{conversationId}/messages")
    @Operation(summary = "Get messages for a conversation")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(@PathVariable UUID conversationId) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Messages fetched",
                service.getMessages(SecurityUtils.getCurrentUserId(), conversationId)
        ));
    }

    @PostMapping("/chat/conversations/{conversationId}/messages")
    @Operation(summary = "Send a chat message")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest req
    ) {
        UserPrincipal p = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Message sent",
                        service.sendMessage(p.getId(), p.getRole(), conversationId, req.message())
                ));
    }

    @DeleteMapping("/chat/messages/{messageId}")
    @Operation(summary = "Delete a chat message sent by current user")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable UUID messageId) {
        service.deleteMessage(SecurityUtils.getCurrentUserId(), messageId);
        return ResponseEntity.ok(ApiResponse.ok("Message deleted", null));
    }

    @PostMapping("/hostel-tickets")
    @Operation(summary = "Customer raises a hostel/hotel issue ticket to owner (supports anonymous)")
    public ResponseEntity<ApiResponse<HostelTicketResponse>> createHostelTicket(
            @Valid @RequestBody CreateHostelTicketRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Hostel ticket raised",
                        service.createHostelTicket(SecurityUtils.getCurrentUserId(), req)
                ));
    }

    @GetMapping("/hostel-tickets/eligible-properties")
    @Operation(summary = "List properties where current customer has an active stay")
    public ResponseEntity<ApiResponse<List<EligibleTicketPropertyResponse>>> eligibleTicketProperties() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Eligible properties fetched",
                service.getEligiblePropertiesForTickets(SecurityUtils.getCurrentUserId())
        ));
    }

    @GetMapping("/hostel-tickets/my")
    @Operation(summary = "List hostel tickets raised by current customer")
    public ResponseEntity<ApiResponse<List<HostelTicketResponse>>> myHostelTickets() {
        return ResponseEntity.ok(ApiResponse.ok(
                "My hostel tickets fetched",
                service.getMyHostelTickets(SecurityUtils.getCurrentUserId())
        ));
    }

    @GetMapping("/owner/hostel-tickets")
    @Operation(summary = "List hostel tickets for owner's properties")
    public ResponseEntity<ApiResponse<List<HostelTicketResponse>>> ownerHostelTickets() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Owner hostel tickets fetched",
                service.getOwnerHostelTickets(SecurityUtils.getCurrentUserId())
        ));
    }

    @PostMapping("/owner/hostel-tickets/{ticketId}/status")
    @Operation(summary = "Owner updates hostel ticket status and resolution")
    public ResponseEntity<ApiResponse<HostelTicketResponse>> updateOwnerHostelTicket(
            @PathVariable UUID ticketId,
            @RequestParam String status,
            @RequestParam(required = false) String resolution
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Hostel ticket updated",
                service.updateHostelTicketByOwner(SecurityUtils.getCurrentUserId(), ticketId, status, resolution)
        ));
    }

    @GetMapping("/owner/hostel-tickets/stats")
    @Operation(summary = "Owner hostel ticket analytics counts")
    public ResponseEntity<ApiResponse<TicketStatsResponse>> ownerTicketStats() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Owner ticket stats fetched",
                service.getOwnerTicketStats(SecurityUtils.getCurrentUserId())
        ));
    }

    @GetMapping("/admin/ticket-stats")
    @Operation(summary = "Admin platform-wide ticket analytics counts")
    public ResponseEntity<ApiResponse<TicketStatsResponse>> adminTicketStats() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Admin ticket stats fetched",
                service.getAdminTicketStats()
        ));
    }

    public record SendMessageRequest(@NotBlank String message) {}
}
