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
    
    Optional<Department> findByName(String name);
    
    List<Department> findByAutoAssignTrue();
    
    @Query("SELECT d FROM Department d ORDER BY d.name ASC")
    List<Department> findAllOrderedByName();
    
    boolean existsByName(String name);
}