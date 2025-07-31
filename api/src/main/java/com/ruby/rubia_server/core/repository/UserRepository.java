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
    
    Optional<User> findByEmailAndCompanyId(String email, UUID companyId);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.company.companyGroup.id = :companyGroupId")
    Optional<User> findByEmailAndCompanyGroupId(@Param("email") String email, @Param("companyGroupId") UUID companyGroupId);
    
    Optional<User> findByWhatsappNumberAndCompanyId(String whatsappNumber, UUID companyId);
    
    List<User> findByCompanyId(UUID companyId);
    
    List<User> findByDepartmentIdAndCompanyId(UUID departmentId, UUID companyId);
    
    List<User> findByIsOnlineTrueAndCompanyId(UUID companyId);
    
    List<User> findByRoleAndCompanyId(UserRole role, UUID companyId);
    
    List<User> findByIsWhatsappActiveTrueAndCompanyId(UUID companyId);
    
    @Query("SELECT u FROM User u WHERE u.department.id = :departmentId AND u.isOnline = true AND u.company.id = :companyId")
    List<User> findAvailableAgentsByDepartmentAndCompany(@Param("departmentId") UUID departmentId, @Param("companyId") UUID companyId);
    
    @Query("SELECT u FROM User u WHERE u.role = 2 AND u.isOnline = true AND u.company.id = :companyId")
    List<User> findAvailableAgentsByCompany(@Param("companyId") UUID companyId);
    
    @Query("SELECT u FROM User u WHERE u.company.id = :companyId ORDER BY u.name ASC")
    List<User> findByCompanyIdOrderedByName(@Param("companyId") UUID companyId);
    
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndCompanyId(String email, UUID companyId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.department.id = :departmentId AND u.company.id = :companyId")
    long countByDepartmentIdAndCompanyId(@Param("departmentId") UUID departmentId, @Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.company.id = :companyId")
    long countByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT u FROM User u WHERE u.whatsappNumber = :whatsappNumber AND u.isWhatsappActive = true")
    Optional<User> findActiveByWhatsappNumber(@Param("whatsappNumber") String whatsappNumber);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company c LEFT JOIN FETCH c.companyGroup LEFT JOIN FETCH u.department WHERE u.email = :email")
    Optional<User> findByEmailWithCompanyAndDepartment(@Param("email") String email);
}