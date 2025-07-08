package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.ConversationParticipant;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationParticipantRepository extends BaseCompanyEntityRepository<ConversationParticipant> {
    
    List<ConversationParticipant> findByConversationId(UUID conversationId);
    
    List<ConversationParticipant> findByConversationIdAndIsActive(UUID conversationId, Boolean isActive);
    
    List<ConversationParticipant> findByCustomerId(UUID customerId);
    
    List<ConversationParticipant> findByUserId(UUID userId);
    
    List<ConversationParticipant> findByAiAgentId(UUID aiAgentId);
    
    List<ConversationParticipant> findByCustomerIdAndIsActive(UUID customerId, Boolean isActive);
    
    List<ConversationParticipant> findByUserIdAndIsActive(UUID userId, Boolean isActive);
    
    List<ConversationParticipant> findByAiAgentIdAndIsActive(UUID aiAgentId, Boolean isActive);
    
    long countByConversationId(UUID conversationId);
    
    long countByConversationIdAndIsActive(UUID conversationId, Boolean isActive);
    
    long countByCustomerId(UUID customerId);
    
    long countByUserId(UUID userId);
    
    long countByAiAgentId(UUID aiAgentId);
    
    boolean existsByConversationIdAndCustomerId(UUID conversationId, UUID customerId);
    
    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);
    
    boolean existsByConversationIdAndAiAgentId(UUID conversationId, UUID aiAgentId);
}