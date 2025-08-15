package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.*;
import com.ruby.rubia_server.core.service.AIAutoMessageService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
@Slf4j
public class MessageDraftController {
    
    private final AIAutoMessageService aiAutoMessageService;
    private final CompanyContextUtil companyContextUtil;
    
    /**
     * Gera draft automaticamente para uma conversa
     */
    @PostMapping("/generate")
    public ResponseEntity<MessageDTO> generateDraft(@Valid @RequestBody GenerateDraftRequest request) {
        try {
            MessageDTO draft = aiAutoMessageService.generateDraftResponse(
                request.getConversationId(), 
                request.getUserMessage()
            );
            
            if (draft == null) {
                return ResponseEntity.noContent().build();
            }
            
            log.info("Generated draft for conversation: {}", request.getConversationId());
            return ResponseEntity.ok(draft);
            
        } catch (Exception e) {
            log.error("Error generating draft", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Cria draft manualmente
     */
    @PostMapping
    public ResponseEntity<MessageDraftDTO> createDraft(@Valid @RequestBody CreateMessageDraftDTO createDTO) {
        try {
            MessageDraftDTO draft = aiAutoMessageService.createDraft(createDTO);
            
            log.info("Created draft: {}", draft.getId());
            return ResponseEntity.ok(draft);
            
        } catch (Exception e) {
            log.error("Error creating draft", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Lista drafts por conversa
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageDraftDTO>> getDraftsByConversation(@PathVariable UUID conversationId) {
        try {
            List<MessageDraftDTO> drafts = aiAutoMessageService.getDraftsByConversation(conversationId);
            
            log.info("Retrieved {} drafts for conversation: {}", drafts.size(), conversationId);
            return ResponseEntity.ok(drafts);
            
        } catch (Exception e) {
            log.error("Error retrieving drafts for conversation: " + conversationId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Lista drafts pendentes paginados
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<MessageDraftDTO>> getPendingDrafts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<MessageDraftDTO> drafts = aiAutoMessageService.getPendingDrafts(companyId, pageable);
            
            log.info("Retrieved {} pending drafts for company: {}", drafts.getTotalElements(), companyId);
            return ResponseEntity.ok(drafts);
            
        } catch (Exception e) {
            log.error("Error retrieving pending drafts", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Aprova um draft
     */
    @PostMapping("/{draftId}/approve")
    public ResponseEntity<MessageDTO> approveDraft(
            @PathVariable UUID draftId, 
            @Valid @RequestBody DraftReviewDTO reviewDTO) {
        
        try {
            reviewDTO.setAction(com.ruby.rubia_server.core.entity.DraftStatus.APPROVED);
            MessageDTO message = aiAutoMessageService.approveDraft(draftId, reviewDTO);
            
            log.info("Approved draft: {}", draftId);
            
            if (message != null) {
                return ResponseEntity.ok(message);
            } else {
                return ResponseEntity.ok().build();
            }
            
        } catch (RuntimeException e) {
            log.warn("Error approving draft: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error approving draft: " + draftId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Edita e aprova um draft
     */
    @PostMapping("/{draftId}/edit")
    public ResponseEntity<MessageDTO> editDraft(
            @PathVariable UUID draftId, 
            @Valid @RequestBody DraftReviewDTO reviewDTO) {
        
        try {
            reviewDTO.setAction(com.ruby.rubia_server.core.entity.DraftStatus.EDITED);
            MessageDTO message = aiAutoMessageService.approveDraft(draftId, reviewDTO);
            
            log.info("Edited and approved draft: {}", draftId);
            
            if (message != null) {
                return ResponseEntity.ok(message);
            } else {
                return ResponseEntity.ok().build();
            }
            
        } catch (RuntimeException e) {
            log.warn("Error editing draft: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error editing draft: " + draftId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Rejeita um draft
     */
    @PostMapping("/{draftId}/reject")
    public ResponseEntity<Void> rejectDraft(
            @PathVariable UUID draftId, 
            @Valid @RequestBody DraftReviewDTO reviewDTO) {
        
        try {
            reviewDTO.setAction(com.ruby.rubia_server.core.entity.DraftStatus.REJECTED);
            aiAutoMessageService.rejectDraft(draftId, reviewDTO);
            
            log.info("Rejected draft: {}", draftId);
            return ResponseEntity.ok().build();
            
        } catch (RuntimeException e) {
            log.warn("Error rejecting draft: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error rejecting draft: " + draftId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtém estatísticas de drafts
     */
    @GetMapping("/stats")
    public ResponseEntity<DraftStatsDTO> getDraftStats() {
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            DraftStatsDTO stats = aiAutoMessageService.getDraftStats(companyId);
            
            log.info("Retrieved draft stats for company: {}", companyId);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving draft stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // DTO auxiliar para request de geração
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GenerateDraftRequest {
        @jakarta.validation.constraints.NotNull
        private UUID conversationId;
        
        @jakarta.validation.constraints.NotBlank
        private String userMessage;
    }
}