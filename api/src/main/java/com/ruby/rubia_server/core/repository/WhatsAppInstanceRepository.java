package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppInstanceRepository extends JpaRepository<WhatsAppInstance, UUID> {

    List<WhatsAppInstance> findByCompanyAndIsActiveTrue(Company company);

    Optional<WhatsAppInstance> findByCompanyAndIsPrimaryTrueAndIsActiveTrue(Company company);

    List<WhatsAppInstance> findByCompanyAndStatusAndIsActiveTrue(Company company, WhatsAppInstanceStatus status);

    Optional<WhatsAppInstance> findByInstanceIdAndIsActiveTrue(String instanceId);

    Optional<WhatsAppInstance> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    @Query("SELECT COUNT(w) FROM WhatsAppInstance w WHERE w.company = :company AND w.isActive = true")
    Long countActiveInstancesByCompany(@Param("company") Company company);

    @Query("SELECT COUNT(w) FROM WhatsAppInstance w WHERE w.company = :company AND w.status = :status AND w.isActive = true")
    Long countInstancesByCompanyAndStatus(@Param("company") Company company, @Param("status") WhatsAppInstanceStatus status);

    @Query("SELECT w FROM WhatsAppInstance w WHERE w.company = :company AND w.status IN :statuses AND w.isActive = true")
    List<WhatsAppInstance> findByCompanyAndStatusInAndIsActiveTrue(@Param("company") Company company, @Param("statuses") List<WhatsAppInstanceStatus> statuses);

    boolean existsByCompanyAndPhoneNumberAndIsActiveTrue(Company company, String phoneNumber);
}