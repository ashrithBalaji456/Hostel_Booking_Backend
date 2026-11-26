package com.accommodation.platform.chat.repository;

import com.accommodation.platform.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    long countByConversationIdAndSenderIdNotAndReadByRecipientFalse(UUID conversationId, UUID recipientId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.readByRecipient = true WHERE m.conversationId = :convId AND m.senderId <> :recipientId AND m.readByRecipient = false")
    void markConversationRead(@Param("convId") UUID convId, @Param("recipientId") UUID recipientId);
}
