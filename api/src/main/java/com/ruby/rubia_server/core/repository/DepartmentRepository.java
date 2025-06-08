package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    
    Optional<Department> findByIdAndCompanyId(UUID id, UUID companyId);
    
    Optional<Department> findByNameAndCompanyId(String name, UUID companyId);
    
    List<Department> findByAutoAssignTrueAndCompanyId(UUID companyId);
    
    @Query("SELECT d FROM Department d WHERE d.company.id = :companyId ORDER BY d.name ASC")
    List<Department> findAllByCompanyIdOrderedByName(UUID companyId);
    
    boolean existsByNameAndCompanyId(String name, UUID companyId);
    
    boolean existsByIdAndCompanyId(UUID id, UUID companyId);
    
    void deleteByIdAndCompanyId(UUID id, UUID companyId);
}