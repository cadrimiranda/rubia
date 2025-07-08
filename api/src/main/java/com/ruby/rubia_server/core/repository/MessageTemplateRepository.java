package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageTemplateRepository extends BaseCompanyEntityRepository<MessageTemplate> {
    
    List<MessageTemplate> findByIsAiGenerated(Boolean isAiGenerated);
    
    List<MessageTemplate> findByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated);
    
    List<MessageTemplate> findByCreatedByUserId(UUID createdByUserId);
    
    List<MessageTemplate> findByAiAgentId(UUID aiAgentId);
    
    List<MessageTemplate> findByTone(String tone);
    
    List<MessageTemplate> findByNameContainingIgnoreCase(String name);
    
    List<MessageTemplate> findByContentContainingIgnoreCase(String content);
    
    List<MessageTemplate> findByCompanyIdAndTone(UUID companyId, String tone);
    
    boolean existsByNameAndCompanyId(String name, UUID companyId);
    
    long countByIsAiGenerated(Boolean isAiGenerated);
    
    long countByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated);
    
    long countByCreatedByUserId(UUID createdByUserId);
    
    long countByAiAgentId(UUID aiAgentId);
}