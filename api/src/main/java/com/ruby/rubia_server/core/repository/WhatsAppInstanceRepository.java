package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
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

    Optional<WhatsAppInstance> findByInstanceIdAndIsActiveTrue(String instanceId);

    Optional<WhatsAppInstance> findByPhoneNumberAndIsActiveTrue(String phoneNumber);

    @Query("SELECT COUNT(w) FROM WhatsAppInstance w WHERE w.company = :company AND w.isActive = true")
    Long countActiveInstancesByCompany(@Param("company") Company company);

    boolean existsByCompanyAndPhoneNumberAndIsActiveTrue(Company company, String phoneNumber);

    Optional<WhatsAppInstance> findByInstanceId(String instanceId);

    List<WhatsAppInstance> findByIsActiveTrueAndInstanceIdIsNotNullAndAccessTokenIsNotNull();
}