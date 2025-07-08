package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.DonationAppointmentDTO;
import com.ruby.rubia_server.core.dto.CreateDonationAppointmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDonationAppointmentDTO;
import com.ruby.rubia_server.core.entity.DonationAppointment;
import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
import com.ruby.rubia_server.core.service.DonationAppointmentService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donation-appointments")
@Slf4j
public class DonationAppointmentController extends BaseCompanyEntityController<DonationAppointment, CreateDonationAppointmentDTO, UpdateDonationAppointmentDTO, DonationAppointmentDTO> {

    private final DonationAppointmentService donationAppointmentService;

    public DonationAppointmentController(DonationAppointmentService donationAppointmentService, CompanyContextUtil companyContextUtil) {
        super(donationAppointmentService, companyContextUtil);
        this.donationAppointmentService = donationAppointmentService;
    }

    @Override
    protected String getEntityName() {
        return "DonationAppointment";
    }

    @Override
    protected DonationAppointmentDTO convertToDTO(DonationAppointment donationAppointment) {
        return DonationAppointmentDTO.builder()
                .id(donationAppointment.getId())
                .companyId(donationAppointment.getCompany().getId())
                .companyName(donationAppointment.getCompany().getName())
                .customerId(donationAppointment.getCustomer().getId())
                .customerName(donationAppointment.getCustomer().getName())
                .conversationId(donationAppointment.getConversation() != null ? donationAppointment.getConversation().getId() : null)
                .externalAppointmentId(donationAppointment.getExternalAppointmentId())
                .appointmentDateTime(donationAppointment.getAppointmentDateTime())
                .status(donationAppointment.getStatus())
                .confirmationUrl(donationAppointment.getConfirmationUrl())
                .notes(donationAppointment.getNotes())
                .createdAt(donationAppointment.getCreatedAt())
                .updatedAt(donationAppointment.getUpdatedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateDonationAppointmentDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // Endpoints específicos da entidade
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<DonationAppointmentDTO>> findByCustomerId(@PathVariable UUID customerId) {
        log.debug("Finding DonationAppointments by customer id via API: {}", customerId);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByCustomerId(customerId);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<DonationAppointmentDTO>> findByConversationId(@PathVariable UUID conversationId) {
        log.debug("Finding DonationAppointments by conversation id via API: {}", conversationId);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByConversationId(conversationId);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<DonationAppointmentDTO>> findByStatus(@PathVariable DonationAppointmentStatus status) {
        log.debug("Finding DonationAppointments by status via API: {}", status);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByStatus(status);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/status/{status}")
    public ResponseEntity<List<DonationAppointmentDTO>> findByCompanyAndStatus(
            @PathVariable UUID companyId, 
            @PathVariable DonationAppointmentStatus status) {
        log.debug("Finding DonationAppointments by company: {} and status: {}", companyId, status);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByCompanyIdAndStatus(companyId, status);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    public ResponseEntity<List<DonationAppointmentDTO>> findByCustomerAndStatus(
            @PathVariable UUID customerId, 
            @PathVariable DonationAppointmentStatus status) {
        log.debug("Finding DonationAppointments by customer: {} and status: {}", customerId, status);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByCustomerIdAndStatus(customerId, status);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/external/{externalAppointmentId}")
    public ResponseEntity<DonationAppointmentDTO> findByExternalAppointmentId(@PathVariable String externalAppointmentId) {
        log.debug("Finding DonationAppointment by external appointment id via API: {}", externalAppointmentId);
        
        Optional<DonationAppointment> donationAppointment = donationAppointmentService.findByExternalAppointmentId(externalAppointmentId);
        if (donationAppointment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DonationAppointmentDTO responseDTO = convertToDTO(donationAppointment.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<DonationAppointmentDTO>> findByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.debug("Finding DonationAppointments by date range via API: {} to {}", start, end);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByDateRange(start, end);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/date-range")
    public ResponseEntity<List<DonationAppointmentDTO>> findByCompanyAndDateRange(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.debug("Finding DonationAppointments by company: {} and date range: {} to {}", companyId, start, end);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.findByCompanyIdAndDateRange(companyId, start, end);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/customer/{customerId}/count")
    public ResponseEntity<Long> countByCustomerId(@PathVariable UUID customerId) {
        log.debug("Counting DonationAppointments by customer id via API: {}", customerId);
        
        long count = donationAppointmentService.countByCustomerId(customerId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/status/{status}/count")
    public ResponseEntity<Long> countByStatus(@PathVariable DonationAppointmentStatus status) {
        log.debug("Counting DonationAppointments by status via API: {}", status);
        
        long count = donationAppointmentService.countByStatus(status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/company/{companyId}/status/{status}/count")
    public ResponseEntity<Long> countByCompanyAndStatus(
            @PathVariable UUID companyId, 
            @PathVariable DonationAppointmentStatus status) {
        log.debug("Counting DonationAppointments by company: {} and status: {}", companyId, status);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long count = donationAppointmentService.countByCompanyIdAndStatus(companyId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/external/{externalAppointmentId}/exists")
    public ResponseEntity<Boolean> existsByExternalAppointmentId(@PathVariable String externalAppointmentId) {
        log.debug("Checking if DonationAppointment exists by external appointment id via API: {}", externalAppointmentId);
        
        boolean exists = donationAppointmentService.existsByExternalAppointmentId(externalAppointmentId);
        return ResponseEntity.ok(exists);
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<DonationAppointmentDTO> confirmAppointment(@PathVariable UUID id) {
        log.debug("Confirming DonationAppointment via API with id: {}", id);
        
        Optional<DonationAppointment> updated = donationAppointmentService.confirmAppointment(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DonationAppointmentDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<DonationAppointmentDTO> cancelAppointment(@PathVariable UUID id) {
        log.debug("Canceling DonationAppointment via API with id: {}", id);
        
        Optional<DonationAppointment> updated = donationAppointmentService.cancelAppointment(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DonationAppointmentDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<DonationAppointmentDTO> completeAppointment(@PathVariable UUID id) {
        log.debug("Completing DonationAppointment via API with id: {}", id);
        
        Optional<DonationAppointment> updated = donationAppointmentService.completeAppointment(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DonationAppointmentDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}/mark-missed")
    public ResponseEntity<DonationAppointmentDTO> markAsMissed(@PathVariable UUID id) {
        log.debug("Marking DonationAppointment as missed via API with id: {}", id);
        
        Optional<DonationAppointment> updated = donationAppointmentService.markAsMissed(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DonationAppointmentDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/company/{companyId}/upcoming")
    public ResponseEntity<List<DonationAppointmentDTO>> getUpcomingAppointments(@PathVariable UUID companyId) {
        log.debug("Getting upcoming appointments for company via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.getUpcomingAppointments(companyId);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/today")
    public ResponseEntity<List<DonationAppointmentDTO>> getTodayAppointments(@PathVariable UUID companyId) {
        log.debug("Getting today's appointments for company via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<DonationAppointment> donationAppointments = donationAppointmentService.getTodayAppointments(companyId);
        List<DonationAppointmentDTO> responseDTOs = donationAppointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }
}