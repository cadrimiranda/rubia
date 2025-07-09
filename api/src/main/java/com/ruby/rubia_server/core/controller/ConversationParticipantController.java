package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.ConversationParticipantDTO;
import com.ruby.rubia_server.core.dto.CreateConversationParticipantDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationParticipantDTO;
import com.ruby.rubia_server.core.entity.ConversationParticipant;
import com.ruby.rubia_server.core.service.ConversationParticipantService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversation-participants")
@Slf4j
public class ConversationParticipantController {

    private final ConversationParticipantService conversationParticipantService;
    private final CompanyContextUtil companyContextUtil;

    public ConversationParticipantController(ConversationParticipantService conversationParticipantService, CompanyContextUtil companyContextUtil) {
        this.conversationParticipantService = conversationParticipantService;
        this.companyContextUtil = companyContextUtil;
    }

    private ConversationParticipantDTO convertToDTO(ConversationParticipant conversationParticipant) {
        return ConversationParticipantDTO.builder()
                .id(conversationParticipant.getId())
                .companyId(conversationParticipant.getCompany().getId())
                .companyName(conversationParticipant.getCompany().getName())
                .conversationId(conversationParticipant.getConversation().getId())
                .customerId(conversationParticipant.getCustomer() != null ? conversationParticipant.getCustomer().getId() : null)
                .customerName(conversationParticipant.getCustomer() != null ? conversationParticipant.getCustomer().getName() : null)
                .userId(conversationParticipant.getUser() != null ? conversationParticipant.getUser().getId() : null)
                .userName(conversationParticipant.getUser() != null ? conversationParticipant.getUser().getName() : null)
                .aiAgentId(conversationParticipant.getAiAgent() != null ? conversationParticipant.getAiAgent().getId() : null)
                .aiAgentName(conversationParticipant.getAiAgent() != null ? conversationParticipant.getAiAgent().getName() : null)
                .isActive(conversationParticipant.getIsActive())
                .joinedAt(conversationParticipant.getJoinedAt())
                .leftAt(conversationParticipant.getLeftAt())
                .createdAt(conversationParticipant.getCreatedAt())
                .updatedAt(conversationParticipant.getUpdatedAt())
                .build();
    }

    @PostMapping
    public ResponseEntity<ConversationParticipantDTO> create(@RequestBody CreateConversationParticipantDTO createDTO) {
        log.debug("Creating ConversationParticipant via API with data: {}", createDTO);
        
        companyContextUtil.ensureCompanyAccess(createDTO.getCompanyId());
        
        ConversationParticipant entity = conversationParticipantService.create(createDTO);
        ConversationParticipantDTO responseDTO = convertToDTO(entity);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationParticipantDTO> findById(@PathVariable UUID id) {
        log.debug("Finding ConversationParticipant by id via API: {}", id);
        
        Optional<ConversationParticipant> entity = conversationParticipantService.findById(id);
        if (entity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationParticipantDTO responseDTO = convertToDTO(entity.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping
    public ResponseEntity<Page<ConversationParticipantDTO>> findAll(Pageable pageable) {
        log.debug("Finding all ConversationParticipant via API with pageable: {}", pageable);
        
        Page<ConversationParticipant> entities = conversationParticipantService.findAll(pageable);
        Page<ConversationParticipantDTO> responseDTOs = entities.map(this::convertToDTO);
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ConversationParticipantDTO>> findByCompanyId(@PathVariable UUID companyId) {
        log.debug("Finding ConversationParticipant by company id via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<ConversationParticipant> entities = conversationParticipantService.findByCompanyId(companyId);
        List<ConversationParticipantDTO> responseDTOs = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversationParticipantDTO> update(@PathVariable UUID id, @RequestBody UpdateConversationParticipantDTO updateDTO) {
        log.debug("Updating ConversationParticipant via API with id: {} and data: {}", id, updateDTO);
        
        Optional<ConversationParticipant> updated = conversationParticipantService.update(id, updateDTO);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationParticipantDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        log.debug("Deleting ConversationParticipant via API with id: {}", id);
        
        boolean deleted = conversationParticipantService.deleteById(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/company/{companyId}/count")
    public ResponseEntity<Long> countByCompanyId(@PathVariable UUID companyId) {
        log.debug("Counting ConversationParticipant by company id via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long count = conversationParticipantService.countByCompanyId(companyId);
        return ResponseEntity.ok(count);
    }

    // Endpoints específicos da entidade
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<ConversationParticipantDTO>> findByConversationId(@PathVariable UUID conversationId) {
        log.debug("Finding ConversationParticipants by conversation id via API: {}", conversationId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findByConversationId(conversationId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/conversation/{conversationId}/active")
    public ResponseEntity<List<ConversationParticipantDTO>> findActiveByConversationId(@PathVariable UUID conversationId) {
        log.debug("Finding active ConversationParticipants by conversation id via API: {}", conversationId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findActiveByConversationId(conversationId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ConversationParticipantDTO>> findByCustomerId(@PathVariable UUID customerId) {
        log.debug("Finding ConversationParticipants by customer id via API: {}", customerId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findByCustomerId(customerId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationParticipantDTO>> findByUserId(@PathVariable UUID userId) {
        log.debug("Finding ConversationParticipants by user id via API: {}", userId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findByUserId(userId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/ai-agent/{aiAgentId}")
    public ResponseEntity<List<ConversationParticipantDTO>> findByAiAgentId(@PathVariable UUID aiAgentId) {
        log.debug("Finding ConversationParticipants by AI agent id via API: {}", aiAgentId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findByAiAgentId(aiAgentId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/customer/{customerId}/active")
    public ResponseEntity<List<ConversationParticipantDTO>> findActiveByCustomerId(@PathVariable UUID customerId) {
        log.debug("Finding active ConversationParticipants by customer id via API: {}", customerId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findActiveByCustomerId(customerId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<ConversationParticipantDTO>> findActiveByUserId(@PathVariable UUID userId) {
        log.debug("Finding active ConversationParticipants by user id via API: {}", userId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findActiveByUserId(userId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/ai-agent/{aiAgentId}/active")
    public ResponseEntity<List<ConversationParticipantDTO>> findActiveByAiAgentId(@PathVariable UUID aiAgentId) {
        log.debug("Finding active ConversationParticipants by AI agent id via API: {}", aiAgentId);
        
        List<ConversationParticipant> conversationParticipants = conversationParticipantService.findActiveByAiAgentId(aiAgentId);
        List<ConversationParticipantDTO> responseDTOs = conversationParticipants.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/conversation/{conversationId}/count")
    public ResponseEntity<Long> countByConversationId(@PathVariable UUID conversationId) {
        log.debug("Counting ConversationParticipants by conversation id via API: {}", conversationId);
        
        long count = conversationParticipantService.countByConversationId(conversationId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/conversation/{conversationId}/count/active")
    public ResponseEntity<Long> countActiveByConversationId(@PathVariable UUID conversationId) {
        log.debug("Counting active ConversationParticipants by conversation id via API: {}", conversationId);
        
        long count = conversationParticipantService.countActiveByConversationId(conversationId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/exists/conversation/{conversationId}/customer/{customerId}")
    public ResponseEntity<Boolean> existsByConversationIdAndCustomerId(
            @PathVariable UUID conversationId, 
            @PathVariable UUID customerId) {
        log.debug("Checking if ConversationParticipant exists by conversation id: {} and customer id: {}", conversationId, customerId);
        
        boolean exists = conversationParticipantService.existsByConversationIdAndCustomerId(conversationId, customerId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exists/conversation/{conversationId}/user/{userId}")
    public ResponseEntity<Boolean> existsByConversationIdAndUserId(
            @PathVariable UUID conversationId, 
            @PathVariable UUID userId) {
        log.debug("Checking if ConversationParticipant exists by conversation id: {} and user id: {}", conversationId, userId);
        
        boolean exists = conversationParticipantService.existsByConversationIdAndUserId(conversationId, userId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/exists/conversation/{conversationId}/ai-agent/{aiAgentId}")
    public ResponseEntity<Boolean> existsByConversationIdAndAiAgentId(
            @PathVariable UUID conversationId, 
            @PathVariable UUID aiAgentId) {
        log.debug("Checking if ConversationParticipant exists by conversation id: {} and AI agent id: {}", conversationId, aiAgentId);
        
        boolean exists = conversationParticipantService.existsByConversationIdAndAiAgentId(conversationId, aiAgentId);
        return ResponseEntity.ok(exists);
    }

    @PutMapping("/{id}/leave")
    public ResponseEntity<ConversationParticipantDTO> leaveConversation(@PathVariable UUID id) {
        log.debug("Marking ConversationParticipant as left via API with id: {}", id);
        
        Optional<ConversationParticipant> updated = conversationParticipantService.leaveConversation(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationParticipantDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PutMapping("/{id}/rejoin")
    public ResponseEntity<ConversationParticipantDTO> rejoinConversation(@PathVariable UUID id) {
        log.debug("Marking ConversationParticipant as rejoined via API with id: {}", id);
        
        Optional<ConversationParticipant> updated = conversationParticipantService.rejoinConversation(id);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversationParticipantDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/conversation/{conversationId}/add-customer/{customerId}")
    public ResponseEntity<ConversationParticipantDTO> addCustomerToConversation(
            @PathVariable UUID conversationId, 
            @PathVariable UUID customerId,
            @RequestParam UUID companyId) {
        log.debug("Adding customer: {} to conversation: {} via API", customerId, conversationId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        try {
            ConversationParticipant participant = conversationParticipantService.addCustomerToConversation(conversationId, customerId, companyId);
            ConversationParticipantDTO responseDTO = convertToDTO(participant);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/conversation/{conversationId}/add-user/{userId}")
    public ResponseEntity<ConversationParticipantDTO> addUserToConversation(
            @PathVariable UUID conversationId, 
            @PathVariable UUID userId,
            @RequestParam UUID companyId) {
        log.debug("Adding user: {} to conversation: {} via API", userId, conversationId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        try {
            ConversationParticipant participant = conversationParticipantService.addUserToConversation(conversationId, userId, companyId);
            ConversationParticipantDTO responseDTO = convertToDTO(participant);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/conversation/{conversationId}/add-ai-agent/{aiAgentId}")
    public ResponseEntity<ConversationParticipantDTO> addAiAgentToConversation(
            @PathVariable UUID conversationId, 
            @PathVariable UUID aiAgentId,
            @RequestParam UUID companyId) {
        log.debug("Adding AI agent: {} to conversation: {} via API", aiAgentId, conversationId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        try {
            ConversationParticipant participant = conversationParticipantService.addAiAgentToConversation(conversationId, aiAgentId, companyId);
            ConversationParticipantDTO responseDTO = convertToDTO(participant);
            return ResponseEntity.ok(responseDTO);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}