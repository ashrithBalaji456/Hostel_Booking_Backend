package com.accommodation.platform.chat.service;

import com.accommodation.platform.booking.entity.Booking;
import com.accommodation.platform.booking.repository.BookingRepository;
import com.accommodation.platform.chat.entity.ChatConversation;
import com.accommodation.platform.chat.entity.ChatMessage;
import com.accommodation.platform.chat.entity.HostelTicket;
import com.accommodation.platform.chat.repository.ChatConversationRepository;
import com.accommodation.platform.chat.repository.ChatMessageRepository;
import com.accommodation.platform.chat.repository.HostelTicketRepository;
import com.accommodation.platform.common.enums.BookingStatus;
import com.accommodation.platform.common.enums.NotificationType;
import com.accommodation.platform.common.enums.UserRole;
import com.accommodation.platform.common.exception.ForbiddenException;
import com.accommodation.platform.common.exception.ResourceNotFoundException;
import com.accommodation.platform.notification.service.NotificationService;
import com.accommodation.platform.property.entity.Property;
import com.accommodation.platform.property.repository.PropertyRepository;
import com.accommodation.platform.support.repository.SupportTicketRepository;
import com.accommodation.platform.user.entity.User;
import com.accommodation.platform.user.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatAndHostelTicketService {

    private final ChatConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final HostelTicketRepository hostelTicketRepo;
    private final SupportTicketRepository supportTicketRepo;
    private final PropertyRepository propertyRepo;
    private final UserRepository userRepo;
    private final BookingRepository bookingRepo;
    private final NotificationService notificationService;

    public ChatAndHostelTicketService(
            ChatConversationRepository conversationRepo,
            ChatMessageRepository messageRepo,
            HostelTicketRepository hostelTicketRepo,
            SupportTicketRepository supportTicketRepo,
            PropertyRepository propertyRepo,
            UserRepository userRepo,
            BookingRepository bookingRepo,
            NotificationService notificationService
    ) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.hostelTicketRepo = hostelTicketRepo;
        this.supportTicketRepo = supportTicketRepo;
        this.propertyRepo = propertyRepo;
        this.userRepo = userRepo;
        this.bookingRepo = bookingRepo;
        this.notificationService = notificationService;
    }

    @Transactional
    public ConversationResponse startOrGetConversation(UUID customerId, UUID propertyId) {
        Property property = propertyRepo.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));
        ChatConversation conv = conversationRepo.findByPropertyIdAndCustomerId(propertyId, customerId)
                .orElseGet(() -> {
                    ChatConversation c = new ChatConversation();
                    c.setPropertyId(propertyId);
                    c.setCustomerId(customerId);
                    c.setOwnerId(property.getOwner().getId());
                    c.setLastMessageAt(Instant.now());
                    return conversationRepo.save(c);
                });
        return toConversationResponse(conv, customerId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMyConversations(UUID userId, UserRole role) {
        List<ChatConversation> list = role == UserRole.PROPERTY_OWNER
                ? conversationRepo.findByOwnerIdOrderByLastMessageAtDesc(userId)
                : conversationRepo.findByCustomerIdOrderByLastMessageAtDesc(userId);
        return list.stream().map(c -> toConversationResponse(c, userId)).toList();
    }

    @Transactional
    public List<ChatMessageResponse> getMessages(UUID actorId, UUID conversationId) {
        ChatConversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found", "CONVERSATION_NOT_FOUND"));
        if (!conv.getCustomerId().equals(actorId) && !conv.getOwnerId().equals(actorId)) {
            throw new ForbiddenException("Access denied to this conversation");
        }
        messageRepo.markConversationRead(conversationId, actorId);
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID actorId, UserRole role, UUID conversationId, String message) {
        ChatConversation conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found", "CONVERSATION_NOT_FOUND"));
        if (!conv.getCustomerId().equals(actorId) && !conv.getOwnerId().equals(actorId)) {
            throw new ForbiddenException("Access denied to this conversation");
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderId(actorId);
        msg.setSenderRole(role.name());
        msg.setMessage(message.trim());
        msg = messageRepo.save(msg);

        conv.setLastMessageAt(Instant.now());
        conversationRepo.save(conv);

        UUID recipientId = conv.getCustomerId().equals(actorId) ? conv.getOwnerId() : conv.getCustomerId();
        User recipient = userRepo.findById(recipientId).orElse(null);
        User sender = userRepo.findById(actorId).orElse(null);
        Property prop = propertyRepo.findById(conv.getPropertyId()).orElse(null);
        if (recipient != null) {
            String senderLabel = sender != null ? sender.getFullName() : "User";
            if (conv.getCustomerId().equals(actorId)) {
                Optional<Booking> active = findActiveBookingForCustomer(actorId, conv.getPropertyId());
                if (active.isPresent()) {
                    senderLabel += " (Room " + active.get().getBed().getRoom().getRoomNumber() + ")";
                }
            }
            notificationService.notify(
                    recipient,
                    "New chat message from " + senderLabel,
                    (prop != null ? "[" + prop.getName() + "] " : "") + message.trim(),
                    NotificationType.CHAT_MESSAGE
            );
        }
        return toMessageResponse(msg);
    }

    @Transactional
    public void deleteMessage(UUID actorId, UUID messageId) {
        ChatMessage msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found", "MESSAGE_NOT_FOUND"));
        if (!msg.getSenderId().equals(actorId)) {
            throw new ForbiddenException("You can only delete your own messages");
        }
        messageRepo.delete(msg);
    }

    @Transactional
    public HostelTicketResponse createHostelTicket(UUID customerId, CreateHostelTicketRequest req) {
        Property property = propertyRepo.findById(req.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found", "PROPERTY_NOT_FOUND"));

        HostelTicket ticket = new HostelTicket();
        ticket.setTicketReference("HTK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        ticket.setPropertyId(property.getId());
        ticket.setCustomerId(customerId);
        ticket.setOwnerId(property.getOwner().getId());
        ticket.setCategory(req.category().trim().toUpperCase());
        ticket.setSubject(req.subject().trim());
        ticket.setDescription(req.description().trim());
        ticket.setAnonymous(req.anonymous());
        ticket.setStatus("OPEN");
        ticket = hostelTicketRepo.save(ticket);

        User owner = property.getOwner();
        User customer = userRepo.findById(customerId).orElse(null);
        String who = req.anonymous()
                ? "Anonymous Resident"
                : (customer != null ? customer.getFullName() : "Resident");
        notificationService.notify(
                owner,
                "New Hostel Ticket (" + ticket.getTicketReference() + ")",
                who + " raised [" + ticket.getCategory() + "] for " + property.getName() + ": " + ticket.getSubject(),
                NotificationType.HOSTEL_TICKET
        );
        return toTicketResponse(ticket, false);
    }

    @Transactional(readOnly = true)
    public List<HostelTicketResponse> getMyHostelTickets(UUID customerId) {
        return hostelTicketRepo.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(t -> toTicketResponse(t, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HostelTicketResponse> getOwnerHostelTickets(UUID ownerId) {
        return hostelTicketRepo.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(t -> toTicketResponse(t, t.isAnonymous()))
                .toList();
    }

    @Transactional
    public HostelTicketResponse updateHostelTicketByOwner(UUID ownerId, UUID ticketId, String status, String resolution) {
        HostelTicket ticket = hostelTicketRepo.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found", "TICKET_NOT_FOUND"));
        if (!ticket.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Access denied");
        }
        ticket.setStatus(status.trim().toUpperCase());
        if (resolution != null) ticket.setOwnerResolution(resolution.trim());
        ticket = hostelTicketRepo.save(ticket);

        User customer = userRepo.findById(ticket.getCustomerId()).orElse(null);
        if (customer != null) {
            notificationService.notify(
                    customer,
                    "Hostel Ticket Updated (" + ticket.getTicketReference() + ")",
                    "Status is now " + ticket.getStatus() + (ticket.getOwnerResolution() != null ? " — " + ticket.getOwnerResolution() : ""),
                    NotificationType.HOSTEL_TICKET
            );
        }
        return toTicketResponse(ticket, ticket.isAnonymous());
    }

    @Transactional(readOnly = true)
    public TicketStatsResponse getOwnerTicketStats(UUID ownerId) {
        long total = hostelTicketRepo.countByOwnerId(ownerId);
        long open = hostelTicketRepo.countByOwnerIdAndStatusIn(ownerId, List.of("OPEN"));
        long inProgress = hostelTicketRepo.countByOwnerIdAndStatusIn(ownerId, List.of("IN_PROGRESS"));
        long resolved = hostelTicketRepo.countByOwnerIdAndStatusIn(ownerId, List.of("RESOLVED", "CLOSED"));
        return new TicketStatsResponse(total, open, inProgress, resolved);
    }

    @Transactional(readOnly = true)
    public TicketStatsResponse getAdminTicketStats() {
        var allSupport = supportTicketRepo.findAll();
        long sTotal = allSupport.size();
        long sOpen = allSupport.stream().filter(t -> "OPEN".equalsIgnoreCase(t.getStatus())).count();
        long sInProgress = allSupport.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
        long sResolved = allSupport.stream().filter(t -> "RESOLVED".equalsIgnoreCase(t.getStatus()) || "CLOSED".equalsIgnoreCase(t.getStatus())).count();

        long hTotal = hostelTicketRepo.count();
        long hOpen = hostelTicketRepo.countByStatusIn(List.of("OPEN"));
        long hInProgress = hostelTicketRepo.countByStatusIn(List.of("IN_PROGRESS"));
        long hResolved = hostelTicketRepo.countByStatusIn(List.of("RESOLVED", "CLOSED"));

        return new TicketStatsResponse(sTotal + hTotal, sOpen + hOpen, sInProgress + hInProgress, sResolved + hResolved);
    }

    private Optional<Booking> findActiveBookingForCustomer(UUID customerId, UUID propertyId) {
        return bookingRepo.findByUserIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, 30))
                .getContent()
                .stream()
                .filter(b -> b.getProperty().getId().equals(propertyId)
                        && (b.getBookingStatus() == BookingStatus.CONFIRMED || b.getBookingStatus() == BookingStatus.COMPLETED))
                .findFirst();
    }

    private ConversationResponse toConversationResponse(ChatConversation c, UUID viewerId) {
        Property prop = propertyRepo.findById(c.getPropertyId()).orElse(null);
        User customer = userRepo.findById(c.getCustomerId()).orElse(null);
        User owner = userRepo.findById(c.getOwnerId()).orElse(null);
        Optional<Booking> booking = findActiveBookingForCustomer(c.getCustomerId(), c.getPropertyId());

        List<ChatMessage> msgs = messageRepo.findByConversationIdOrderByCreatedAtAsc(c.getId());
        String lastMsg = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1).getMessage();
        long unread = messageRepo.countByConversationIdAndSenderIdNotAndReadByRecipientFalse(c.getId(), viewerId);

        return new ConversationResponse(
                c.getId(),
                c.getPropertyId(),
                prop != null ? prop.getName() : "Hostel",
                c.getCustomerId(),
                customer != null ? customer.getFullName() : "Guest",
                c.getOwnerId(),
                owner != null ? owner.getFullName() : "Owner",
                booking.map(b -> b.getBed().getRoom().getRoomNumber()).orElse(null),
                booking.map(b -> b.getBed().getBedNumber()).orElse(null),
                lastMsg,
                unread,
                c.getLastMessageAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(), m.getConversationId(), m.getSenderId(), m.getSenderRole(),
                m.getMessage(), m.isReadByRecipient(), m.getCreatedAt()
        );
    }

    private HostelTicketResponse toTicketResponse(HostelTicket t, boolean maskIdentity) {
        Property prop = propertyRepo.findById(t.getPropertyId()).orElse(null);
        User customer = maskIdentity ? null : userRepo.findById(t.getCustomerId()).orElse(null);
        Optional<Booking> booking = maskIdentity ? Optional.empty() : findActiveBookingForCustomer(t.getCustomerId(), t.getPropertyId());

        return new HostelTicketResponse(
                t.getId(),
                t.getTicketReference(),
                t.getPropertyId(),
                prop != null ? prop.getName() : "Hostel",
                maskIdentity ? "Anonymous Resident" : (customer != null ? customer.getFullName() : "Resident"),
                maskIdentity ? null : (customer != null ? customer.getEmail() : null),
                maskIdentity ? null : (customer != null ? customer.getPhoneNumber() : null),
                booking.map(b -> b.getBed().getRoom().getRoomNumber()).orElse(null),
                booking.map(b -> b.getBed().getBedNumber()).orElse(null),
                t.getCategory(),
                t.getSubject(),
                t.getDescription(),
                t.isAnonymous(),
                t.getStatus(),
                t.getOwnerResolution(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }

    public record CreateHostelTicketRequest(
            @NotNull UUID propertyId,
            @NotBlank String category,
            @NotBlank String subject,
            @NotBlank String description,
            boolean anonymous
    ) {}

    public record ConversationResponse(
            UUID id,
            UUID propertyId,
            String propertyName,
            UUID customerId,
            String customerName,
            UUID ownerId,
            String ownerName,
            String roomNumber,
            String bedNumber,
            String lastMessage,
            long unreadCount,
            Instant lastMessageAt
    ) {}

    public record ChatMessageResponse(
            UUID id,
            UUID conversationId,
            UUID senderId,
            String senderRole,
            String message,
            boolean readByRecipient,
            Instant createdAt
    ) {}

    public record HostelTicketResponse(
            UUID id,
            String ticketReference,
            UUID propertyId,
            String propertyName,
            String customerName,
            String customerEmail,
            String customerPhone,
            String roomNumber,
            String bedNumber,
            String category,
            String subject,
            String description,
            boolean anonymous,
            String status,
            String ownerResolution,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TicketStatsResponse(
            long total,
            long open,
            long inProgress,
            long resolvedOrClosed
    ) {}
}
