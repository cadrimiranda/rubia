package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.ConversationLastMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationLastMessageRepository extends JpaRepository<ConversationLastMessage, UUID> {

    Optional<ConversationLastMessage> findByConversationId(UUID conversationId);

    @Modifying
    @Query("UPDATE ConversationLastMessage clm SET " +
           "clm.lastMessageDate = :lastMessageDate, " +
           "clm.lastMessageId = :lastMessageId, " +
           "clm.lastMessageContent = :lastMessageContent " +
           "WHERE clm.conversationId = :conversationId")
    void updateLastMessage(@Param("conversationId") UUID conversationId,
                          @Param("lastMessageDate") LocalDateTime lastMessageDate,
                          @Param("lastMessageId") UUID lastMessageId,
                          @Param("lastMessageContent") String lastMessageContent);

    @Modifying
    @Query("DELETE FROM ConversationLastMessage clm WHERE clm.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") UUID conversationId);
}