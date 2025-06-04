package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.SenderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    
    List<Message> findByConversationId(UUID conversationId);
    
    Page<Message> findByConversationId(UUID conversationId, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    List<Message> findByConversationIdOrderedByCreatedAt(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    Page<Message> findByConversationIdOrderedByCreatedAtDesc(@Param("conversationId") UUID conversationId, Pageable pageable);
    
    Optional<Message> findByExternalMessageId(String externalMessageId);
    
    List<Message> findBySenderType(SenderType senderType);
    
    List<Message> findBySenderId(UUID senderId);
    
    List<Message> findByStatus(MessageStatus status);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.status IN ('SENT', 'DELIVERED') AND m.senderType = 'CUSTOMER'")
    List<Message> findUnreadCustomerMessages(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.status IN ('SENT', 'DELIVERED') AND m.senderType = 'CUSTOMER'")
    long countUnreadCustomerMessages(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Message> findLastMessageByConversation(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT m FROM Message m WHERE m.isAiGenerated = true AND m.conversation.id = :conversationId")
    List<Message> findAiGeneratedMessagesByConversation(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT m FROM Message m WHERE " +
           "to_tsvector('portuguese', m.content) @@ plainto_tsquery('portuguese', :searchTerm)")
    List<Message> searchByContent(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND " +
           "to_tsvector('portuguese', m.content) @@ plainto_tsquery('portuguese', :searchTerm)")
    List<Message> searchByContentInConversation(@Param("conversationId") UUID conversationId, 
                                              @Param("searchTerm") String searchTerm);
    
    @Query("SELECT m FROM Message m WHERE m.createdAt BETWEEN :startDate AND :endDate")
    List<Message> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                 @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.senderType = :senderType")
    long countByConversationIdAndSenderType(@Param("conversationId") UUID conversationId, 
                                          @Param("senderType") SenderType senderType);
    
    @Query("SELECT m FROM Message m WHERE m.senderId = :userId AND m.senderType = 'AGENT' ORDER BY m.createdAt DESC")
    List<Message> findRecentMessagesByAgent(@Param("userId") UUID userId, Pageable pageable);
    
    @Query("SELECT DISTINCT m.conversation.id FROM Message m WHERE m.senderId = :userId AND m.senderType = 'AGENT'")
    List<UUID> findConversationIdsByAgent(@Param("userId") UUID userId);
    
    boolean existsByExternalMessageId(String externalMessageId);
}