package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.DonationAppointment;
import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationAppointmentRepository extends BaseCompanyEntityRepository<DonationAppointment> {
    
    List<DonationAppointment> findByCustomerId(UUID customerId);
    
    List<DonationAppointment> findByConversationId(UUID conversationId);
    
    List<DonationAppointment> findByStatus(DonationAppointmentStatus status);
    
    List<DonationAppointment> findByCompanyIdAndStatus(UUID companyId, DonationAppointmentStatus status);
    
    List<DonationAppointment> findByCustomerIdAndStatus(UUID customerId, DonationAppointmentStatus status);
    
    Optional<DonationAppointment> findByExternalAppointmentId(String externalAppointmentId);
    
    List<DonationAppointment> findByAppointmentDateTimeBetween(LocalDateTime start, LocalDateTime end);
    
    List<DonationAppointment> findByCompanyIdAndAppointmentDateTimeBetween(UUID companyId, LocalDateTime start, LocalDateTime end);
    
    long countByCustomerId(UUID customerId);
    
    long countByStatus(DonationAppointmentStatus status);
    
    long countByCompanyIdAndStatus(UUID companyId, DonationAppointmentStatus status);
    
    boolean existsByExternalAppointmentId(String externalAppointmentId);
}