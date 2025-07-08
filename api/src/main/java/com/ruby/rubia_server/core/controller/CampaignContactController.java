package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CampaignContactDTO;
import com.ruby.rubia_server.core.dto.CreateCampaignContactDTO;
import com.ruby.rubia_server.core.dto.UpdateCampaignContactDTO;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.service.CampaignContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaign-contacts")
@RequiredArgsConstructor
@Slf4j
public class CampaignContactController {

    private final CampaignContactService campaignContactService;

    @PostMapping
    public ResponseEntity<CampaignContactDTO> create(@Valid @RequestBody CreateCampaignContactDTO createDTO) {
        log.debug("Creating CampaignContact via API: {}", createDTO);
        
        CampaignContact created = campaignContactService.create(createDTO);
        CampaignContactDTO responseDTO = convertToDTO(created);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignContactDTO> findById(@PathVariable UUID id) {
        log.debug("Finding CampaignContact by id via API: {}", id);
        
        Optional<CampaignContact> campaignContact = campaignContactService.findById(id);
        if (campaignContact.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CampaignContactDTO responseDTO = convertToDTO(campaignContact.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping
    public ResponseEntity<Page<CampaignContactDTO>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        log.debug("Finding all CampaignContacts via API with pageable: {}", pageable);
        
        Page<CampaignContact> campaignContacts = campaignContactService.findAll(pageable);
        Page<CampaignContactDTO> responseDTOs = campaignContacts.map(this::convertToDTO);
        
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignContactDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateCampaignContactDTO updateDTO) {
        log.debug("Updating CampaignContact via API with id: {}", id);
        
        Optional<CampaignContact> updated = campaignContactService.update(id, updateDTO);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CampaignContactDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        log.debug("Deleting CampaignContact via API with id: {}", id);
        
        boolean deleted = campaignContactService.deleteById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<CampaignContactDTO>> findByCampaignId(@PathVariable UUID campaignId) {
        log.debug("Finding CampaignContacts by campaign id via API: {}", campaignId);
        
        List<CampaignContact> campaignContacts = campaignContactService.findByCampaignId(campaignId);
        List<CampaignContactDTO> responseDTOs = campaignContacts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CampaignContactDTO>> findByCustomerId(@PathVariable UUID customerId) {
        log.debug("Finding CampaignContacts by customer id via API: {}", customerId);
        
        List<CampaignContact> campaignContacts = campaignContactService.findByCustomerId(customerId);
        List<CampaignContactDTO> responseDTOs = campaignContacts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CampaignContactDTO>> findByStatus(@PathVariable CampaignContactStatus status) {
        log.debug("Finding CampaignContacts by status via API: {}", status);
        
        List<CampaignContact> campaignContacts = campaignContactService.findByStatus(status);
        List<CampaignContactDTO> responseDTOs = campaignContacts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/campaign/{campaignId}/status/{status}")
    public ResponseEntity<List<CampaignContactDTO>> findByCampaignIdAndStatus(
            @PathVariable UUID campaignId, 
            @PathVariable CampaignContactStatus status) {
        log.debug("Finding CampaignContacts by campaign id: {} and status: {}", campaignId, status);
        
        List<CampaignContact> campaignContacts = campaignContactService.findByCampaignIdAndStatus(campaignId, status);
        List<CampaignContactDTO> responseDTOs = campaignContacts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/customer/{customerId}/status/{status}")
    public ResponseEntity<List<CampaignContactDTO>> findByCustomerIdAndStatus(
            @PathVariable UUID customerId, 
            @PathVariable CampaignContactStatus status) {
        log.debug("Finding CampaignContacts by customer id: {} and status: {}", customerId, status);
        
        List<CampaignContact> campaignContacts = campaignContactService.findByCustomerIdAndStatus(customerId, status);
        List<CampaignContactDTO> responseDTOs = campaignContacts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/campaign/{campaignId}/count")
    public ResponseEntity<Long> countByCampaignId(@PathVariable UUID campaignId) {
        log.debug("Counting CampaignContacts by campaign id via API: {}", campaignId);
        
        long count = campaignContactService.countByCampaignId(campaignId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/customer/{customerId}/count")
    public ResponseEntity<Long> countByCustomerId(@PathVariable UUID customerId) {
        log.debug("Counting CampaignContacts by customer id via API: {}", customerId);
        
        long count = campaignContactService.countByCustomerId(customerId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/campaign/{campaignId}/count/status/{status}")
    public ResponseEntity<Long> countByCampaignIdAndStatus(
            @PathVariable UUID campaignId, 
            @PathVariable CampaignContactStatus status) {
        log.debug("Counting CampaignContacts by campaign id: {} and status: {}", campaignId, status);
        
        long count = campaignContactService.countByCampaignIdAndStatus(campaignId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByCampaignIdAndCustomerId(
            @RequestParam UUID campaignId, 
            @RequestParam UUID customerId) {
        log.debug("Checking if CampaignContact exists by campaign id: {} and customer id: {}", campaignId, customerId);
        
        boolean exists = campaignContactService.existsByCampaignIdAndCustomerId(campaignId, customerId);
        return ResponseEntity.ok(exists);
    }

    @PutMapping("/{id}/mark-completed")
    public ResponseEntity<CampaignContactDTO> markAsCompleted(@PathVariable UUID id) {
        log.debug("Marking CampaignContact as completed via API with id: {}", id);
        
        Optional<CampaignContact> updated = campaignContactService.markAsCompleted(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CampaignContactDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}/mark-responded")
    public ResponseEntity<CampaignContactDTO> markAsResponded(@PathVariable UUID id) {
        log.debug("Marking CampaignContact as responded via API with id: {}", id);
        
        Optional<CampaignContact> updated = campaignContactService.markAsResponded(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        CampaignContactDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    private CampaignContactDTO convertToDTO(CampaignContact campaignContact) {
        return CampaignContactDTO.builder()
                .id(campaignContact.getId())
                .campaignId(campaignContact.getCampaign().getId())
                .campaignName(campaignContact.getCampaign().getName())
                .customerId(campaignContact.getCustomer().getId())
                .customerName(campaignContact.getCustomer().getName())
                .status(campaignContact.getStatus())
                .messageSentAt(campaignContact.getMessageSentAt())
                .responseReceivedAt(campaignContact.getResponseReceivedAt())
                .notes(campaignContact.getNotes())
                .createdAt(campaignContact.getCreatedAt())
                .updatedAt(campaignContact.getUpdatedAt())
                .build();
    }
}