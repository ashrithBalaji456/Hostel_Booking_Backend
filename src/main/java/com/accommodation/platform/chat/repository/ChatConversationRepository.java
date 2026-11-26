package com.accommodation.platform.chat.repository;

import com.accommodation.platform.chat.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {
    Optional<ChatConversation> findByPropertyIdAndCustomerId(UUID propertyId, UUID customerId);
    List<ChatConversation> findByCustomerIdOrderByLastMessageAtDesc(UUID customerId);
    List<ChatConversation> findByOwnerIdOrderByLastMessageAtDesc(UUID ownerId);
}
