package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.AIAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AIAgentRepository extends JpaRepository<AIAgent, UUID> {

    List<AIAgent> findByCompanyId(UUID companyId);

    @Query("SELECT a FROM AIAgent a WHERE a.company.id = :companyId AND a.isActive = true")
    List<AIAgent> findActiveByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT a FROM AIAgent a WHERE a.company.id = :companyId ORDER BY a.name ASC")
    List<AIAgent> findByCompanyIdOrderByName(@Param("companyId") UUID companyId);

    boolean existsByNameAndCompanyId(String name, UUID companyId);

    @Query("SELECT COUNT(a) FROM AIAgent a WHERE a.company.id = :companyId")
    long countByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT COUNT(a) FROM AIAgent a WHERE a.company.id = :companyId AND a.isActive = true")
    long countActiveByCompanyId(@Param("companyId") UUID companyId);

    @Query("SELECT a FROM AIAgent a WHERE a.aiModelType = :modelType")
    List<AIAgent> findByAiModelType(@Param("modelType") String modelType);

    @Query("SELECT a FROM AIAgent a WHERE a.temperament = :temperament")
    List<AIAgent> findByTemperament(@Param("temperament") String temperament);
}