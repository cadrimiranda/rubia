package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.AILog;
import com.ruby.rubia_server.core.enums.AILogStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AILogRepository extends JpaRepository<AILog, UUID> {

    List<AILog> findByCompanyId(UUID companyId);

    List<AILog> findByStatus(AILogStatus status);

    List<AILog> findByAiAgentId(UUID aiAgentId);

    List<AILog> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByCompanyIdAndStatus(UUID companyId, AILogStatus status);

    @Query("SELECT SUM(a.estimatedCost) FROM AILog a WHERE a.company.id = :companyId")
    BigDecimal sumEstimatedCostByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT SUM(a.tokensUsedInput) + SUM(a.tokensUsedOutput) FROM AILog a WHERE a.company.id = :companyId")
    Long sumTokensUsedByCompanyId(@Param("companyId") UUID companyId);
}