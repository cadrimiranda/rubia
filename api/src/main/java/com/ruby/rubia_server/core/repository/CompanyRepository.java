package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.ruby.rubia_server.core.enums.CompanyPlanType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {
    
    Optional<Company> findBySlug(String slug);
    
    List<Company> findByIsActiveTrue();
    
    @Query("SELECT c FROM Company c WHERE c.isActive = true AND c.planType = :planType")
    List<Company> findByPlanType(@Param("planType") CompanyPlanType planType);
    
    boolean existsBySlug(String slug);
    
    Optional<Company> findByCompanyGroupId(UUID companyGroupId);
}