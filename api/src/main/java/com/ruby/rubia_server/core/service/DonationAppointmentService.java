package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateDonationAppointmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDonationAppointmentDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class DonationAppointmentService extends BaseCompanyEntityService<DonationAppointment, CreateDonationAppointmentDTO, UpdateDonationAppointmentDTO> {

    private final DonationAppointmentRepository donationAppointmentRepository;
    private final CustomerRepository customerRepository;
    private final ConversationRepository conversationRepository;

    public DonationAppointmentService(DonationAppointmentRepository donationAppointmentRepository,
                                     CompanyRepository companyRepository,
                                     CustomerRepository customerRepository,
                                     ConversationRepository conversationRepository,
                                     EntityRelationshipValidator relationshipValidator) {
        super(donationAppointmentRepository, companyRepository, relationshipValidator);
        this.donationAppointmentRepository = donationAppointmentRepository;
        this.customerRepository = customerRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    protected String getEntityName() {
        return "DonationAppointment";
    }

    @Override
    protected DonationAppointment buildEntityFromDTO(CreateDonationAppointmentDTO createDTO) {
        DonationAppointment.DonationAppointmentBuilder builder = DonationAppointment.builder()
                .externalAppointmentId(createDTO.getExternalAppointmentId())
                .appointmentDateTime(createDTO.getAppointmentDateTime())
                .status(createDTO.getStatus())
                .confirmationUrl(createDTO.getConfirmationUrl())
                .notes(createDTO.getNotes());

        // Validate and set required customer
        Customer customer = customerRepository.findById(createDTO.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + createDTO.getCustomerId()));
        builder.customer(customer);

        // Handle optional conversation relationship
        if (createDTO.getConversationId() != null) {
            Conversation conversation = conversationRepository.findById(createDTO.getConversationId())
                    .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + createDTO.getConversationId()));
            builder.conversation(conversation);
        }

        return builder.build();
    }

    @Override
    protected void updateEntityFromDTO(DonationAppointment donationAppointment, UpdateDonationAppointmentDTO updateDTO) {
        if (updateDTO.getAppointmentDateTime() != null) {
            donationAppointment.setAppointmentDateTime(updateDTO.getAppointmentDateTime());
        }
        if (updateDTO.getStatus() != null) {
            donationAppointment.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getConfirmationUrl() != null) {
            donationAppointment.setConfirmationUrl(updateDTO.getConfirmationUrl());
        }
        if (updateDTO.getNotes() != null) {
            donationAppointment.setNotes(updateDTO.getNotes());
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateDonationAppointmentDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<DonationAppointment> findByCustomerId(UUID customerId) {
        log.debug("Finding DonationAppointments by customer id: {}", customerId);
        return donationAppointmentRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> findByConversationId(UUID conversationId) {
        log.debug("Finding DonationAppointments by conversation id: {}", conversationId);
        return donationAppointmentRepository.findByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> findByStatus(DonationAppointmentStatus status) {
        log.debug("Finding DonationAppointments by status: {}", status);
        return donationAppointmentRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> findByCompanyIdAndStatus(UUID companyId, DonationAppointmentStatus status) {
        log.debug("Finding DonationAppointments by company: {} and status: {}", companyId, status);
        return donationAppointmentRepository.findByCompanyIdAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> findByCustomerIdAndStatus(UUID customerId, DonationAppointmentStatus status) {
        log.debug("Finding DonationAppointments by customer: {} and status: {}", customerId, status);
        return donationAppointmentRepository.findByCustomerIdAndStatus(customerId, status);
    }

    @Transactional(readOnly = true)
    public Optional<DonationAppointment> findByExternalAppointmentId(String externalAppointmentId) {
        log.debug("Finding DonationAppointment by external appointment id: {}", externalAppointmentId);
        return donationAppointmentRepository.findByExternalAppointmentId(externalAppointmentId);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> findByDateRange(LocalDateTime start, LocalDateTime end) {
        log.debug("Finding DonationAppointments between: {} and {}", start, end);
        return donationAppointmentRepository.findByAppointmentDateTimeBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> findByCompanyIdAndDateRange(UUID companyId, LocalDateTime start, LocalDateTime end) {
        log.debug("Finding DonationAppointments by company: {} between: {} and {}", companyId, start, end);
        return donationAppointmentRepository.findByCompanyIdAndAppointmentDateTimeBetween(companyId, start, end);
    }

    @Transactional(readOnly = true)
    public long countByCustomerId(UUID customerId) {
        log.debug("Counting DonationAppointments by customer id: {}", customerId);
        return donationAppointmentRepository.countByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public long countByStatus(DonationAppointmentStatus status) {
        log.debug("Counting DonationAppointments by status: {}", status);
        return donationAppointmentRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countByCompanyIdAndStatus(UUID companyId, DonationAppointmentStatus status) {
        log.debug("Counting DonationAppointments by company: {} and status: {}", companyId, status);
        return donationAppointmentRepository.countByCompanyIdAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public boolean existsByExternalAppointmentId(String externalAppointmentId) {
        log.debug("Checking if DonationAppointment exists by external appointment id: {}", externalAppointmentId);
        return donationAppointmentRepository.existsByExternalAppointmentId(externalAppointmentId);
    }

    @Transactional
    public Optional<DonationAppointment> confirmAppointment(UUID appointmentId) {
        log.debug("Confirming DonationAppointment with id: {}", appointmentId);

        Optional<DonationAppointment> optionalAppointment = donationAppointmentRepository.findById(appointmentId);
        if (optionalAppointment.isEmpty()) {
            log.warn("DonationAppointment not found with id: {}", appointmentId);
            return Optional.empty();
        }

        DonationAppointment appointment = optionalAppointment.get();
        appointment.setStatus(DonationAppointmentStatus.CONFIRMED);

        DonationAppointment updatedAppointment = donationAppointmentRepository.save(appointment);
        log.debug("DonationAppointment confirmed successfully with id: {}", updatedAppointment.getId());

        return Optional.of(updatedAppointment);
    }

    @Transactional
    public Optional<DonationAppointment> cancelAppointment(UUID appointmentId) {
        log.debug("Canceling DonationAppointment with id: {}", appointmentId);

        Optional<DonationAppointment> optionalAppointment = donationAppointmentRepository.findById(appointmentId);
        if (optionalAppointment.isEmpty()) {
            log.warn("DonationAppointment not found with id: {}", appointmentId);
            return Optional.empty();
        }

        DonationAppointment appointment = optionalAppointment.get();
        appointment.setStatus(DonationAppointmentStatus.CANCELED);

        DonationAppointment updatedAppointment = donationAppointmentRepository.save(appointment);
        log.debug("DonationAppointment canceled successfully with id: {}", updatedAppointment.getId());

        return Optional.of(updatedAppointment);
    }

    @Transactional
    public Optional<DonationAppointment> completeAppointment(UUID appointmentId) {
        log.debug("Completing DonationAppointment with id: {}", appointmentId);

        Optional<DonationAppointment> optionalAppointment = donationAppointmentRepository.findById(appointmentId);
        if (optionalAppointment.isEmpty()) {
            log.warn("DonationAppointment not found with id: {}", appointmentId);
            return Optional.empty();
        }

        DonationAppointment appointment = optionalAppointment.get();
        appointment.setStatus(DonationAppointmentStatus.COMPLETED);

        DonationAppointment updatedAppointment = donationAppointmentRepository.save(appointment);
        log.debug("DonationAppointment completed successfully with id: {}", updatedAppointment.getId());

        return Optional.of(updatedAppointment);
    }

    @Transactional
    public Optional<DonationAppointment> markAsMissed(UUID appointmentId) {
        log.debug("Marking DonationAppointment as missed with id: {}", appointmentId);

        Optional<DonationAppointment> optionalAppointment = donationAppointmentRepository.findById(appointmentId);
        if (optionalAppointment.isEmpty()) {
            log.warn("DonationAppointment not found with id: {}", appointmentId);
            return Optional.empty();
        }

        DonationAppointment appointment = optionalAppointment.get();
        appointment.setStatus(DonationAppointmentStatus.MISSED);

        DonationAppointment updatedAppointment = donationAppointmentRepository.save(appointment);
        log.debug("DonationAppointment marked as missed successfully with id: {}", updatedAppointment.getId());

        return Optional.of(updatedAppointment);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> getUpcomingAppointments(UUID companyId) {
        log.debug("Getting upcoming appointments for company: {}", companyId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureLimit = now.plusDays(30); // Next 30 days
        return findByCompanyIdAndDateRange(companyId, now, futureLimit);
    }

    @Transactional(readOnly = true)
    public List<DonationAppointment> getTodayAppointments(UUID companyId) {
        log.debug("Getting today's appointments for company: {}", companyId);
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return findByCompanyIdAndDateRange(companyId, startOfDay, endOfDay);
    }
}