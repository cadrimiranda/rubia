package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.MessageEnhancementAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageEnhancementAuditRepository extends JpaRepository<MessageEnhancementAudit, UUID> {

    // Buscar auditorias por empresa
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId ORDER BY mea.createdAt DESC")
    Page<MessageEnhancementAudit> findByCompanyId(@Param("companyId") UUID companyId, Pageable pageable);

    // Buscar auditorias por usuário
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.user.id = :userId ORDER BY mea.createdAt DESC")
    Page<MessageEnhancementAudit> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    // Buscar auditorias por agente IA
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.aiAgent.id = :aiAgentId ORDER BY mea.createdAt DESC")
    Page<MessageEnhancementAudit> findByAiAgentId(@Param("aiAgentId") UUID aiAgentId, Pageable pageable);

    // Buscar auditorias por conversa
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.conversationId = :conversationId ORDER BY mea.createdAt DESC")
    List<MessageEnhancementAudit> findByConversationId(@Param("conversationId") UUID conversationId);

    // Estatísticas - contar melhorias bem-sucedidas por empresa
    @Query("SELECT COUNT(mea) FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.success = true")
    long countSuccessfulEnhancementsByCompany(@Param("companyId") UUID companyId);

    // Estatísticas - contar melhorias falhadas por empresa
    @Query("SELECT COUNT(mea) FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.success = false")
    long countFailedEnhancementsByCompany(@Param("companyId") UUID companyId);

    // Estatísticas - somar tokens consumidos por empresa
    @Query("SELECT COALESCE(SUM(mea.tokensConsumed), 0) FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.success = true")
    long sumTokensConsumedByCompany(@Param("companyId") UUID companyId);

    // Estatísticas - tempo médio de resposta por empresa
    @Query("SELECT AVG(mea.responseTimeMs) FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.success = true")
    Double averageResponseTimeByCompany(@Param("companyId") UUID companyId);

    // Buscar melhorias por período
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.createdAt BETWEEN :startDate AND :endDate ORDER BY mea.createdAt DESC")
    Page<MessageEnhancementAudit> findByCompanyIdAndDateRange(
        @Param("companyId") UUID companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // Buscar melhorias por temperamento
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.temperamentUsed = :temperament ORDER BY mea.createdAt DESC")
    Page<MessageEnhancementAudit> findByCompanyIdAndTemperament(
        @Param("companyId") UUID companyId,
        @Param("temperament") String temperament,
        Pageable pageable
    );

    // Buscar melhorias por modelo de IA
    @Query("SELECT mea FROM MessageEnhancementAudit mea WHERE mea.company.id = :companyId AND mea.aiModelUsed = :model ORDER BY mea.createdAt DESC")
    Page<MessageEnhancementAudit> findByCompanyIdAndAiModel(
        @Param("companyId") UUID companyId,
        @Param("model") String model,
        Pageable pageable
    );
}