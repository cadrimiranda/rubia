package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.*;
import com.ruby.rubia_server.core.entity.FAQ;
import com.ruby.rubia_server.core.service.FAQService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import jakarta.validation.Valid;
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
@RequestMapping("/api/faqs")
@Slf4j
public class FAQController extends BaseCompanyEntityController<FAQ, CreateFAQDTO, UpdateFAQDTO, FAQDTO> {

    private final FAQService faqService;

    public FAQController(FAQService faqService, CompanyContextUtil companyContextUtil) {
        super(faqService, companyContextUtil);
        this.faqService = faqService;
    }

    @Override
    protected String getEntityName() {
        return "FAQ";
    }

    @Override
    protected FAQDTO convertToDTO(FAQ faq) {
        return FAQDTO.builder()
                .id(faq.getId())
                .companyId(faq.getCompany().getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .keywords(faq.getKeywords())
                .triggers(faq.getTriggers())
                .usageCount(faq.getUsageCount())
                .successRate(faq.getSuccessRate())
                .isActive(faq.getIsActive())
                .createdById(faq.getCreatedBy().getId())
                .createdByName(faq.getCreatedBy().getName())
                .lastEditedById(faq.getLastEditedBy() != null ? faq.getLastEditedBy().getId() : null)
                .lastEditedByName(faq.getLastEditedBy() != null ? faq.getLastEditedBy().getName() : null)
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateFAQDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // FAQ-specific endpoints

    /**
     * Search FAQs with pagination and filtering
     */
    @GetMapping("/search")
    public ResponseEntity<Page<FAQDTO>> searchFAQs(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            Page<FAQDTO> faqs = faqService.searchFAQs(companyId, searchTerm, isActive, pageable);
            
            log.info("Searched FAQs for company: {}, searchTerm: {}, isActive: {}, found: {}", 
                companyId, searchTerm, isActive, faqs.getTotalElements());
                
            return ResponseEntity.ok(faqs);
            
        } catch (Exception e) {
            log.error("Error searching FAQs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search FAQs relevant to a user message (for AI integration)
     */
    @PostMapping("/search/relevant")
    public ResponseEntity<List<FAQMatchDTO>> searchRelevantFAQs(@Valid @RequestBody FAQSearchDTO searchDTO) {
        try {
            if (searchDTO.getCompanyId() == null) {
                searchDTO.setCompanyId(companyContextUtil.getCurrentCompanyId());
            }
            
            List<FAQMatchDTO> matches = faqService.searchRelevantFAQs(searchDTO);
            
            log.info("Found {} relevant FAQs for message: '{}'", matches.size(), searchDTO.getUserMessage());
            
            return ResponseEntity.ok(matches);
            
        } catch (Exception e) {
            log.error("Error searching relevant FAQs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Record FAQ usage for AI learning
     */
    @PostMapping("/{id}/usage")
    public ResponseEntity<Void> recordFAQUsage(
            @PathVariable UUID id,
            @RequestParam String userMessage,
            @RequestParam boolean wasApproved) {
        
        try {
            faqService.recordFAQUsage(id, userMessage, wasApproved);
            
            log.info("Recorded FAQ usage - ID: {}, approved: {}", id, wasApproved);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("Error recording FAQ usage for ID: " + id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get FAQ statistics for current company
     */
    @GetMapping("/stats")
    public ResponseEntity<FAQStatsDTO> getFAQStats() {
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            FAQStatsDTO stats = faqService.getCompanyFAQStats(companyId);
            
            log.info("Retrieved FAQ stats for company: {}", companyId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving FAQ stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Soft delete a FAQ
     */
    @DeleteMapping("/{id}/soft")
    public ResponseEntity<Void> softDeleteFAQ(@PathVariable UUID id) {
        try {
            faqService.softDelete(id);
            
            log.info("Soft deleted FAQ: {}", id);
            
            return ResponseEntity.noContent().build();
            
        } catch (RuntimeException e) {
            log.warn("FAQ not found for soft delete: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error soft deleting FAQ: " + id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Restore a soft deleted FAQ
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<FAQDTO> restoreFAQ(@PathVariable UUID id) {
        try {
            FAQDTO restoredFAQ = faqService.restore(id);
            
            log.info("Restored FAQ: {}", id);
            
            return ResponseEntity.ok(restoredFAQ);
            
        } catch (RuntimeException e) {
            log.warn("FAQ not found for restore: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error restoring FAQ: " + id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get deleted FAQs for current company
     */
    @GetMapping("/deleted")
    public ResponseEntity<List<FAQDTO>> getDeletedFAQs() {
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            List<FAQDTO> deletedFAQs = faqService.getDeletedFAQs(companyId);
            
            log.info("Retrieved {} deleted FAQs for company: {}", deletedFAQs.size(), companyId);
            
            return ResponseEntity.ok(deletedFAQs);
            
        } catch (Exception e) {
            log.error("Error retrieving deleted FAQs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get active FAQs for current company (simplified endpoint)
     */
    @GetMapping("/active")
    public ResponseEntity<List<FAQDTO>> getActiveFAQs() {
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            
            // Use search with isActive=true filter
            Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<FAQDTO> faqs = faqService.searchFAQs(companyId, null, true, pageable);
            
            log.info("Retrieved {} active FAQs for company: {}", faqs.getContent().size(), companyId);
            
            return ResponseEntity.ok(faqs.getContent());
            
        } catch (Exception e) {
            log.error("Error retrieving active FAQs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Test FAQ search (for frontend testing)
     */
    @PostMapping("/test-search")
    public ResponseEntity<List<FAQMatchDTO>> testFAQSearch(@RequestBody TestSearchRequest request) {
        try {
            UUID companyId = companyContextUtil.getCurrentCompanyId();
            
            FAQSearchDTO searchDTO = FAQSearchDTO.builder()
                    .companyId(companyId)
                    .userMessage(request.getMessage())
                    .limit(5)
                    .minConfidenceScore(0.1)
                    .build();
            
            List<FAQMatchDTO> matches = faqService.searchRelevantFAQs(searchDTO);
            
            log.info("Test search for '{}' returned {} matches", request.getMessage(), matches.size());
            
            return ResponseEntity.ok(matches);
            
        } catch (Exception e) {
            log.error("Error in test FAQ search", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper DTO for test search
    public static class TestSearchRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}