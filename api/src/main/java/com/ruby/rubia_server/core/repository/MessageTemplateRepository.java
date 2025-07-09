package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageTemplateRepository extends BaseCompanyEntityRepository<MessageTemplate> {
    
    List<MessageTemplate> findByIsAiGenerated(Boolean isAiGenerated);
    
    List<MessageTemplate> findByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated);
    
    List<MessageTemplate> findByCreatedById(UUID createdById);
    
    List<MessageTemplate> findByAiAgentId(UUID aiAgentId);
    
    List<MessageTemplate> findByTone(String tone);
    
    List<MessageTemplate> findByNameContainingIgnoreCase(String name);
    
    List<MessageTemplate> findByContentContainingIgnoreCase(String content);
    
    List<MessageTemplate> findByCompanyIdAndTone(UUID companyId, String tone);
    
    boolean existsByNameAndCompanyId(String name, UUID companyId);
    
    long countByIsAiGenerated(Boolean isAiGenerated);
    
    long countByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated);
    
    long countByCreatedById(UUID createdById);
    
    long countByAiAgentId(UUID aiAgentId);
    
    // Queries that filter out soft-deleted templates
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.isAiGenerated = :isAiGenerated")
    List<MessageTemplate> findByIsAiGeneratedAndNotDeleted(@Param("isAiGenerated") Boolean isAiGenerated);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.company.id = :companyId AND mt.isAiGenerated = :isAiGenerated")
    List<MessageTemplate> findByCompanyIdAndIsAiGeneratedAndNotDeleted(@Param("companyId") UUID companyId, @Param("isAiGenerated") Boolean isAiGenerated);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.createdBy.id = :createdById")
    List<MessageTemplate> findByCreatedByIdAndNotDeleted(@Param("createdById") UUID createdById);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.aiAgent.id = :aiAgentId")
    List<MessageTemplate> findByAiAgentIdAndNotDeleted(@Param("aiAgentId") UUID aiAgentId);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.tone = :tone")
    List<MessageTemplate> findByToneAndNotDeleted(@Param("tone") String tone);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.company.id = :companyId AND mt.tone = :tone")
    List<MessageTemplate> findByCompanyIdAndToneAndNotDeleted(@Param("companyId") UUID companyId, @Param("tone") String tone);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND LOWER(mt.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<MessageTemplate> findByNameContainingIgnoreCaseAndNotDeleted(@Param("name") String name);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND LOWER(mt.content) LIKE LOWER(CONCAT('%', :content, '%'))")
    List<MessageTemplate> findByContentContainingIgnoreCaseAndNotDeleted(@Param("content") String content);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.company.id = :companyId")
    List<MessageTemplate> findByCompanyIdAndNotDeleted(@Param("companyId") UUID companyId);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.id = :id")
    Optional<MessageTemplate> findByIdAndNotDeleted(@Param("id") UUID id);
    
    @Query("SELECT CASE WHEN COUNT(mt) > 0 THEN true ELSE false END FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.name = :name AND mt.company.id = :companyId")
    boolean existsByNameAndCompanyIdAndNotDeleted(@Param("name") String name, @Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(mt) FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.isAiGenerated = :isAiGenerated")
    long countByIsAiGeneratedAndNotDeleted(@Param("isAiGenerated") Boolean isAiGenerated);
    
    @Query("SELECT COUNT(mt) FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.company.id = :companyId AND mt.isAiGenerated = :isAiGenerated")
    long countByCompanyIdAndIsAiGeneratedAndNotDeleted(@Param("companyId") UUID companyId, @Param("isAiGenerated") Boolean isAiGenerated);
    
    @Query("SELECT COUNT(mt) FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.createdBy.id = :createdById")
    long countByCreatedByIdAndNotDeleted(@Param("createdById") UUID createdById);
    
    @Query("SELECT COUNT(mt) FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.aiAgent.id = :aiAgentId")
    long countByAiAgentIdAndNotDeleted(@Param("aiAgentId") UUID aiAgentId);
    
    @Query("SELECT COUNT(mt) FROM MessageTemplate mt WHERE mt.deletedAt IS NULL AND mt.company.id = :companyId")
    long countByCompanyIdAndNotDeleted(@Param("companyId") UUID companyId);
    
    // Queries specifically for soft-deleted templates
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NOT NULL AND mt.company.id = :companyId")
    List<MessageTemplate> findDeletedByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT mt FROM MessageTemplate mt WHERE mt.deletedAt IS NOT NULL")
    List<MessageTemplate> findAllDeleted();
}