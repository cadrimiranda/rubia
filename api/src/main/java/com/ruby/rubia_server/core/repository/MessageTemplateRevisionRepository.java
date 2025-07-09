package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.MessageTemplateRevision;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageTemplateRevisionRepository extends BaseCompanyEntityRepository<MessageTemplateRevision> {
    
    List<MessageTemplateRevision> findByTemplateId(UUID templateId);
    
    List<MessageTemplateRevision> findByEditedById(UUID editedById);
    
    Optional<MessageTemplateRevision> findByTemplateIdAndRevisionNumber(UUID templateId, Integer revisionNumber);
    
    List<MessageTemplateRevision> findByTemplateIdOrderByRevisionNumberDesc(UUID templateId);
    
    Optional<MessageTemplateRevision> findFirstByTemplateIdOrderByRevisionNumberDesc(UUID templateId);
    
    boolean existsByTemplateIdAndRevisionNumber(UUID templateId, Integer revisionNumber);
    
    List<MessageTemplateRevision> findByTemplateIdAndRevisionNumberBetweenOrderByRevisionNumber(
            UUID templateId, Integer minRevision, Integer maxRevision);
    
    long countByTemplateId(UUID templateId);
    
    @Query("SELECT MAX(r.revisionNumber) FROM MessageTemplateRevision r WHERE r.template.id = :templateId")
    Optional<Integer> findMaxRevisionNumberByTemplateId(@Param("templateId") UUID templateId);
}