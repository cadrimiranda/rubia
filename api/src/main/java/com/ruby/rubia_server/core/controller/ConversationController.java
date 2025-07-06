package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationSummaryDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {
    
    private final ConversationService conversationService;
    private final CompanyContextUtil companyContextUtil;
    
    @PostMapping
    public ResponseEntity<ConversationDTO> create(@Valid @RequestBody CreateConversationDTO createDTO) {
        log.info("Creating conversation for customer: {}", createDTO.getCustomerId());
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO created = conversationService.create(createDTO, currentCompanyId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating conversation: {}", e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ConversationDTO> findById(@PathVariable UUID id) {
        log.debug("Finding conversation by id: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO conversation = conversationService.findById(id, currentCompanyId);
            return ResponseEntity.ok(conversation);
        } catch (IllegalArgumentException e) {
            log.warn("Conversation not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<Page<ConversationDTO>> findByStatus(
            @RequestParam ConversationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Finding conversations by status: {} with pageable: {}", status, pageable);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        Page<ConversationDTO> conversations = conversationService.findByStatusAndCompanyWithPagination(status, currentCompanyId, pageable);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<Page<ConversationDTO>> findByStatusPaginated(
            @RequestParam ConversationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Finding conversations by status with pagination: {}", status);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        Page<ConversationDTO> conversations = conversationService.findByStatusAndCompanyWithPagination(status, currentCompanyId, pageable);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/summaries")
    public ResponseEntity<List<ConversationSummaryDTO>> findSummariesByStatus(
            @RequestParam ConversationStatus status) {
        log.debug("Finding conversation summaries by status: {}", status);
        
        // This endpoint is not implemented for multi-tenant - use /api/conversations instead
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ConversationDTO>> findByCustomer(@PathVariable UUID customerId) {
        log.debug("Finding conversations by customer: {}", customerId);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findByCustomerAndCompany(customerId, currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDTO>> findByAssignedUser(@PathVariable UUID userId) {
        log.debug("Finding conversations by assigned user: {}", userId);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findByAssignedUserAndCompany(userId, currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/unassigned")
    public ResponseEntity<List<ConversationDTO>> findUnassigned() {
        log.debug("Finding unassigned conversations");
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<ConversationDTO> conversations = conversationService.findUnassignedByCompany(currentCompanyId);
        return ResponseEntity.ok(conversations);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ConversationDTO> update(@PathVariable UUID id, 
                                                 @Valid @RequestBody UpdateConversationDTO updateDTO) {
        log.info("Updating conversation: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO updated = conversationService.update(id, updateDTO, currentCompanyId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating conversation: {}", e.getMessage());
            if (e.getMessage().contains("não encontrad")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
    
    @PutMapping("/{conversationId}/assign/{userId}")
    public ResponseEntity<ConversationDTO> assignToUser(@PathVariable UUID conversationId,
                                                        @PathVariable UUID userId) {
        log.info("Assigning conversation {} to user {}", conversationId, userId);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO assigned = conversationService.assignToUser(conversationId, userId, currentCompanyId);
            return ResponseEntity.ok(assigned);
        } catch (IllegalArgumentException e) {
            log.warn("Error assigning conversation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{conversationId}/status")
    public ResponseEntity<ConversationDTO> changeStatus(@PathVariable UUID conversationId,
                                                        @RequestParam ConversationStatus status) {
        log.info("Changing conversation {} status to {}", conversationId, status);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            ConversationDTO updated = conversationService.changeStatus(conversationId, status, currentCompanyId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error changing conversation status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    
    
    @GetMapping("/stats/count")
    public ResponseEntity<Long> countByStatus(@RequestParam ConversationStatus status) {
        log.debug("Counting conversations by status: {}", status);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        long count = conversationService.countByStatusAndCompany(status, currentCompanyId);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/active-by-user/{userId}")
    public ResponseEntity<Long> countActiveByUser(@PathVariable UUID userId) {
        log.debug("Counting active conversations by user: {}", userId);
        
        // This endpoint is not implemented for multi-tenant
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting conversation: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            conversationService.delete(id, currentCompanyId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting conversation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}