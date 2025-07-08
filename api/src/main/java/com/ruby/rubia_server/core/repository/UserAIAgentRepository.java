package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.UserAIAgent;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAIAgentRepository extends BaseCompanyEntityRepository<UserAIAgent> {
    
    List<UserAIAgent> findByUserId(UUID userId);
    
    List<UserAIAgent> findByAiAgentId(UUID aiAgentId);
    
    Optional<UserAIAgent> findByUserIdAndAiAgentId(UUID userId, UUID aiAgentId);
    
    boolean existsByUserIdAndAiAgentId(UUID userId, UUID aiAgentId);
    
    List<UserAIAgent> findByIsDefault(Boolean isDefault);
    
    Optional<UserAIAgent> findByUserIdAndIsDefault(UUID userId, Boolean isDefault);
    
    long countByAiAgentId(UUID aiAgentId);
    
    long countByUserId(UUID userId);
    
    long countByIsDefault(Boolean isDefault);
}