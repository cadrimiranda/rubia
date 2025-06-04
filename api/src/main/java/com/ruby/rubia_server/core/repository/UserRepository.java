package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    List<User> findByDepartmentId(UUID departmentId);
    
    List<User> findByIsOnlineTrue();
    
    List<User> findByRole(UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.department.id = :departmentId AND u.isOnline = true")
    List<User> findAvailableAgentsByDepartment(@Param("departmentId") UUID departmentId);
    
    @Query("SELECT u FROM User u WHERE u.role = 'AGENT' AND u.isOnline = true")
    List<User> findAvailableAgents();
    
    @Query("SELECT u FROM User u ORDER BY u.name ASC")
    List<User> findAllOrderedByName();
    
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") UUID departmentId);
}