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
    
    Optional<Customer> findByPhone(String phone);
    
    Optional<Customer> findByWhatsappId(String whatsappId);
    
    List<Customer> findByIsBlockedFalse();
    
    List<Customer> findByIsBlockedTrue();
    
    @Query("SELECT c FROM Customer c WHERE c.isBlocked = false ORDER BY c.name ASC")
    List<Customer> findActiveCustomersOrderedByName();
    
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "c.phone LIKE CONCAT('%', :searchTerm, '%')")
    List<Customer> searchByNameOrPhone(@Param("searchTerm") String searchTerm);
    
    boolean existsByPhone(String phone);
    
    boolean existsByWhatsappId(String whatsappId);
    
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.isBlocked = false")
    long countActiveCustomers();
}