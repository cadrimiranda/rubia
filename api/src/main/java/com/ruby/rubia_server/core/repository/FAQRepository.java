package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.FAQ;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FAQRepository extends BaseCompanyEntityRepository<FAQ> {
    
    // Basic queries
    List<FAQ> findByIsActive(Boolean isActive);
    
    List<FAQ> findByCompanyIdAndIsActive(UUID companyId, Boolean isActive);
    
    List<FAQ> findByCreatedById(UUID createdById);
    
    // Search queries
    List<FAQ> findByQuestionContainingIgnoreCase(String question);
    
    List<FAQ> findByAnswerContainingIgnoreCase(String answer);
    
    @Query("SELECT f FROM FAQ f JOIN f.keywords k WHERE LOWER(k) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FAQ> findByKeywordsContaining(@Param("keyword") String keyword);
    
    @Query("SELECT f FROM FAQ f JOIN f.triggers t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :trigger, '%'))")
    List<FAQ> findByTriggersContaining(@Param("trigger") String trigger);
    
    // Combined search for AI matching
    @Query("SELECT DISTINCT f FROM FAQ f LEFT JOIN f.keywords k LEFT JOIN f.triggers t " +
           "WHERE f.deletedAt IS NULL AND f.isActive = true AND f.company.id = :companyId " +
           "AND (LOWER(f.question) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(f.answer) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(k) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(t) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<FAQ> findRelevantFAQs(@Param("companyId") UUID companyId, @Param("searchTerm") String searchTerm);
    
    // Exact keyword match for higher priority results
    @Query("SELECT DISTINCT f FROM FAQ f JOIN f.keywords k " +
           "WHERE f.deletedAt IS NULL AND f.isActive = true AND f.company.id = :companyId " +
           "AND LOWER(k) = LOWER(:keyword)")
    List<FAQ> findByExactKeyword(@Param("companyId") UUID companyId, @Param("keyword") String keyword);
    
    // Exact trigger match
    @Query("SELECT DISTINCT f FROM FAQ f JOIN f.triggers t " +
           "WHERE f.deletedAt IS NULL AND f.isActive = true AND f.company.id = :companyId " +
           "AND LOWER(t) = LOWER(:trigger)")
    List<FAQ> findByExactTrigger(@Param("companyId") UUID companyId, @Param("trigger") String trigger);
    
    // Statistics queries
    long countByIsActive(Boolean isActive);
    
    long countByCompanyIdAndIsActive(UUID companyId, Boolean isActive);
    
    @Query("SELECT AVG(f.successRate) FROM FAQ f WHERE f.deletedAt IS NULL AND f.company.id = :companyId")
    Double getAverageSuccessRateByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT SUM(f.usageCount) FROM FAQ f WHERE f.deletedAt IS NULL AND f.company.id = :companyId")
    Long getTotalUsageCountByCompanyId(@Param("companyId") UUID companyId);
    
    // Top performing FAQs
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.isActive = true AND f.company.id = :companyId " +
           "ORDER BY f.successRate DESC, f.usageCount DESC")
    List<FAQ> findTopPerformingFAQs(@Param("companyId") UUID companyId);
    
    // Most used FAQs
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.isActive = true AND f.company.id = :companyId " +
           "ORDER BY f.usageCount DESC, f.successRate DESC")
    List<FAQ> findMostUsedFAQs(@Param("companyId") UUID companyId);
    
    // Queries that filter out soft-deleted FAQs
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.isActive = :isActive")
    List<FAQ> findByIsActiveAndNotDeleted(@Param("isActive") Boolean isActive);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.company.id = :companyId AND f.isActive = :isActive")
    List<FAQ> findByCompanyIdAndIsActiveAndNotDeleted(@Param("companyId") UUID companyId, @Param("isActive") Boolean isActive);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.createdBy.id = :createdById")
    List<FAQ> findByCreatedByIdAndNotDeleted(@Param("createdById") UUID createdById);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND LOWER(f.question) LIKE LOWER(CONCAT('%', :question, '%'))")
    List<FAQ> findByQuestionContainingIgnoreCaseAndNotDeleted(@Param("question") String question);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND LOWER(f.answer) LIKE LOWER(CONCAT('%', :answer, '%'))")
    List<FAQ> findByAnswerContainingIgnoreCaseAndNotDeleted(@Param("answer") String answer);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.company.id = :companyId")
    List<FAQ> findByCompanyIdAndNotDeleted(@Param("companyId") UUID companyId);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NULL AND f.id = :id")
    Optional<FAQ> findByIdAndNotDeleted(@Param("id") UUID id);
    
    @Query("SELECT COUNT(f) FROM FAQ f WHERE f.deletedAt IS NULL AND f.isActive = :isActive")
    long countByIsActiveAndNotDeleted(@Param("isActive") Boolean isActive);
    
    @Query("SELECT COUNT(f) FROM FAQ f WHERE f.deletedAt IS NULL AND f.company.id = :companyId AND f.isActive = :isActive")
    long countByCompanyIdAndIsActiveAndNotDeleted(@Param("companyId") UUID companyId, @Param("isActive") Boolean isActive);
    
    @Query("SELECT COUNT(f) FROM FAQ f WHERE f.deletedAt IS NULL AND f.createdBy.id = :createdById")
    long countByCreatedByIdAndNotDeleted(@Param("createdById") UUID createdById);
    
    @Query("SELECT COUNT(f) FROM FAQ f WHERE f.deletedAt IS NULL AND f.company.id = :companyId")
    long countByCompanyIdAndNotDeleted(@Param("companyId") UUID companyId);
    
    // Queries specifically for soft-deleted FAQs
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NOT NULL AND f.company.id = :companyId")
    List<FAQ> findDeletedByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT f FROM FAQ f WHERE f.deletedAt IS NOT NULL")
    List<FAQ> findAllDeleted();
    
    // Check if FAQ with same question exists
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FAQ f " +
           "WHERE f.deletedAt IS NULL AND LOWER(f.question) = LOWER(:question) AND f.company.id = :companyId")
    boolean existsByQuestionAndCompanyIdAndNotDeleted(@Param("question") String question, @Param("companyId") UUID companyId);
}