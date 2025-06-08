package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    
    
    
    
    // Company-scoped methods
    Optional<Customer> findByPhoneAndCompanyId(String phone, UUID companyId);
    
    Optional<Customer> findByWhatsappIdAndCompanyId(String whatsappId, UUID companyId);
    
    List<Customer> findByCompanyId(UUID companyId);
    
    List<Customer> findByIsBlockedFalseAndCompanyId(UUID companyId);
    
    List<Customer> findByIsBlockedTrueAndCompanyId(UUID companyId);
    
    @Query("SELECT c FROM Customer c WHERE c.isBlocked = false AND c.company.id = :companyId ORDER BY c.name ASC")
    List<Customer> findActiveCustomersByCompanyOrderedByName(@Param("companyId") UUID companyId);
    
    @Query("SELECT c FROM Customer c WHERE c.company.id = :companyId AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.phone LIKE CONCAT('%', :searchTerm, '%'))")
    List<Customer> searchByNameOrPhoneAndCompany(@Param("searchTerm") String searchTerm, @Param("companyId") UUID companyId);
    
    boolean existsByPhoneAndCompanyId(String phone, UUID companyId);
    
    boolean existsByWhatsappIdAndCompanyId(String whatsappId, UUID companyId);
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isBlocked = false AND c.company.id = :companyId")
    long countActiveCustomersByCompany(@Param("companyId") UUID companyId);
}