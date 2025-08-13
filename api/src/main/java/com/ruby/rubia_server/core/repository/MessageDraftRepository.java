package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.MessageDraft;
import com.ruby.rubia_server.core.entity.DraftStatus;
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
public interface MessageDraftRepository extends JpaRepository<MessageDraft, UUID> {
    
    // Buscar drafts por conversa
    List<MessageDraft> findByConversationIdAndDeletedAtIsNull(UUID conversationId);
    
    List<MessageDraft> findByConversationIdAndStatusAndDeletedAtIsNull(UUID conversationId, DraftStatus status);
    
    // Buscar drafts pendentes por empresa
    @Query("SELECT md FROM MessageDraft md WHERE md.company.id = :companyId AND md.status = :status AND md.deletedAt IS NULL ORDER BY md.createdAt DESC")
    Page<MessageDraft> findPendingDraftsByCompany(@Param("companyId") UUID companyId, @Param("status") DraftStatus status, Pageable pageable);
    
    // Buscar drafts pendentes
    List<MessageDraft> findByStatusAndDeletedAtIsNull(DraftStatus status);
    
    // Contar drafts pendentes por conversa
    long countByConversationIdAndStatusAndDeletedAtIsNull(UUID conversationId, DraftStatus status);
    
    // Contar drafts pendentes por empresa
    long countByCompanyIdAndStatusAndDeletedAtIsNull(UUID companyId, DraftStatus status);
    
    // Buscar drafts por operador revisor
    List<MessageDraft> findByReviewedByIdAndDeletedAtIsNull(UUID reviewedById);
    
    // Buscar último draft de uma conversa
    @Query("SELECT md FROM MessageDraft md WHERE md.conversation.id = :conversationId AND md.deletedAt IS NULL ORDER BY md.createdAt DESC")
    Page<MessageDraft> findLatestByConversation(@Param("conversationId") UUID conversationId, Pageable pageable);
    
    default Optional<MessageDraft> findLatestByConversation(UUID conversationId) {
        Page<MessageDraft> result = findLatestByConversation(conversationId, Pageable.ofSize(1));
        return result.hasContent() ? Optional.of(result.getContent().get(0)) : Optional.empty();
    }
    
    // Buscar drafts criados recentemente
    @Query("SELECT md FROM MessageDraft md WHERE md.company.id = :companyId AND md.createdAt >= :since AND md.deletedAt IS NULL ORDER BY md.createdAt DESC")
    List<MessageDraft> findRecentDrafts(@Param("companyId") UUID companyId, @Param("since") LocalDateTime since);
    
    // Estatísticas de drafts
    @Query("SELECT COUNT(md) FROM MessageDraft md WHERE md.company.id = :companyId AND md.status = :status AND md.deletedAt IS NULL")
    long countByCompanyAndStatus(@Param("companyId") UUID companyId, @Param("status") DraftStatus status);
    
    @Query("SELECT md.status, COUNT(md) FROM MessageDraft md WHERE md.company.id = :companyId AND md.deletedAt IS NULL GROUP BY md.status")
    List<Object[]> getDraftStatsByCompany(@Param("companyId") UUID companyId);
    
    // Buscar drafts por fonte (FAQ, Template, etc.)
    List<MessageDraft> findBySourceTypeAndSourceIdAndDeletedAtIsNull(String sourceType, UUID sourceId);
    
    // Buscar drafts por modelo de IA
    @Query("SELECT md FROM MessageDraft md WHERE md.company.id = :companyId AND md.aiModel = :aiModel AND md.deletedAt IS NULL")
    List<MessageDraft> findByCompanyAndAiModel(@Param("companyId") UUID companyId, @Param("aiModel") String aiModel);
    
    // Buscar drafts com alta confiança
    @Query("SELECT md FROM MessageDraft md WHERE md.company.id = :companyId AND md.confidence >= :minConfidence AND md.deletedAt IS NULL ORDER BY md.confidence DESC")
    List<MessageDraft> findHighConfidenceDrafts(@Param("companyId") UUID companyId, @Param("minConfidence") Double minConfidence);
    
    // Limpar drafts antigos rejeitados
    @Query("SELECT md FROM MessageDraft md WHERE md.status = :status AND md.createdAt < :beforeDate AND md.deletedAt IS NULL")
    List<MessageDraft> findOldDraftsForCleanup(@Param("status") DraftStatus status, @Param("beforeDate") LocalDateTime beforeDate);
}