package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationSummaryDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.service.ConversationService;
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
    
    @PostMapping
    public ResponseEntity<ConversationDTO> create(@Valid @RequestBody CreateConversationDTO createDTO) {
        log.info("Creating conversation for customer: {}", createDTO.getCustomerId());
        
        try {
            ConversationDTO created = conversationService.create(createDTO);
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
            ConversationDTO conversation = conversationService.findById(id);
            return ResponseEntity.ok(conversation);
        } catch (IllegalArgumentException e) {
            log.warn("Conversation not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<ConversationDTO>> findByStatus(
            @RequestParam ConversationStatus status) {
        log.debug("Finding conversations by status: {}", status);
        
        List<ConversationDTO> conversations = conversationService.findByStatus(status);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/paginated")
    public ResponseEntity<Page<ConversationDTO>> findByStatusPaginated(
            @RequestParam ConversationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("Finding conversations by status with pagination: {}", status);
        
        Page<ConversationDTO> conversations = conversationService.findByStatusWithPagination(status, pageable);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/summaries")
    public ResponseEntity<List<ConversationSummaryDTO>> findSummariesByStatus(
            @RequestParam ConversationStatus status) {
        log.debug("Finding conversation summaries by status: {}", status);
        
        List<ConversationSummaryDTO> summaries = conversationService.findSummariesByStatus(status);
        return ResponseEntity.ok(summaries);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<ConversationDTO>> findByCustomer(@PathVariable UUID customerId) {
        log.debug("Finding conversations by customer: {}", customerId);
        
        List<ConversationDTO> conversations = conversationService.findByCustomer(customerId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDTO>> findByAssignedUser(@PathVariable UUID userId) {
        log.debug("Finding conversations by assigned user: {}", userId);
        
        List<ConversationDTO> conversations = conversationService.findByAssignedUser(userId);
        return ResponseEntity.ok(conversations);
    }
    
    @GetMapping("/unassigned")
    public ResponseEntity<List<ConversationDTO>> findUnassigned() {
        log.debug("Finding unassigned conversations");
        
        List<ConversationDTO> conversations = conversationService.findUnassigned();
        return ResponseEntity.ok(conversations);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ConversationDTO> update(@PathVariable UUID id, 
                                                 @Valid @RequestBody UpdateConversationDTO updateDTO) {
        log.info("Updating conversation: {}", id);
        
        try {
            ConversationDTO updated = conversationService.update(id, updateDTO);
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
            ConversationDTO assigned = conversationService.assignToUser(conversationId, userId);
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
            ConversationDTO updated = conversationService.changeStatus(conversationId, status);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error changing conversation status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{conversationId}/pin")
    public ResponseEntity<ConversationDTO> pinConversation(@PathVariable UUID conversationId) {
        log.info("Toggling pin status for conversation: {}", conversationId);
        
        try {
            ConversationDTO pinned = conversationService.pinConversation(conversationId);
            return ResponseEntity.ok(pinned);
        } catch (IllegalArgumentException e) {
            log.warn("Error pinning conversation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/stats/count")
    public ResponseEntity<Long> countByStatus(@RequestParam ConversationStatus status) {
        log.debug("Counting conversations by status: {}", status);
        
        long count = conversationService.countByStatus(status);
        return ResponseEntity.ok(count);
    }
    
    @GetMapping("/stats/active-by-user/{userId}")
    public ResponseEntity<Long> countActiveByUser(@PathVariable UUID userId) {
        log.debug("Counting active conversations by user: {}", userId);
        
        long count = conversationService.countActiveByUser(userId);
        return ResponseEntity.ok(count);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting conversation: {}", id);
        
        try {
            conversationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting conversation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}