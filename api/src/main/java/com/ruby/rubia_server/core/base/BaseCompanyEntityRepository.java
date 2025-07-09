package com.ruby.rubia_server.core.base;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@NoRepositoryBean
public interface BaseCompanyEntityRepository<T extends BaseEntity> extends JpaRepository<T, UUID> {
    
    List<T> findByCompanyId(UUID companyId);
    
    long countByCompanyId(UUID companyId);
    
    @Query("SELECT e FROM #{#entityName} e WHERE e.company.id = :companyId AND e.createdAt BETWEEN :startDate AND :endDate")
    List<T> findByCompanyIdAndDateRange(@Param("companyId") UUID companyId, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
}