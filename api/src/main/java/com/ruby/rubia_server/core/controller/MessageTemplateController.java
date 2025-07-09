package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.MessageTemplateDTO;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.service.MessageTemplateService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/message-templates")
@Slf4j
public class MessageTemplateController extends BaseCompanyEntityController<MessageTemplate, CreateMessageTemplateDTO, UpdateMessageTemplateDTO, MessageTemplateDTO> {

    private final MessageTemplateService messageTemplateService;

    public MessageTemplateController(MessageTemplateService messageTemplateService, CompanyContextUtil companyContextUtil) {
        super(messageTemplateService, companyContextUtil);
        this.messageTemplateService = messageTemplateService;
    }

    @Override
    protected String getEntityName() {
        return "MessageTemplate";
    }

    @Override
    protected MessageTemplateDTO convertToDTO(MessageTemplate messageTemplate) {
        return MessageTemplateDTO.builder()
                .id(messageTemplate.getId())
                .companyId(messageTemplate.getCompany().getId())
                .companyName(messageTemplate.getCompany().getName())
                .name(messageTemplate.getName())
                .content(messageTemplate.getContent())
                .isAiGenerated(messageTemplate.getIsAiGenerated())
                .createdByUserId(messageTemplate.getCreatedBy() != null ? messageTemplate.getCreatedBy().getId() : null)
                .createdByUserName(messageTemplate.getCreatedBy() != null ? messageTemplate.getCreatedBy().getName() : null)
                .aiAgentId(messageTemplate.getAiAgent() != null ? messageTemplate.getAiAgent().getId() : null)
                .aiAgentName(messageTemplate.getAiAgent() != null ? messageTemplate.getAiAgent().getName() : null)
                .tone(messageTemplate.getTone())
                .lastEditedByUserId(messageTemplate.getLastEditedBy() != null ? messageTemplate.getLastEditedBy().getId() : null)
                .lastEditedByUserName(messageTemplate.getLastEditedBy() != null ? messageTemplate.getLastEditedBy().getName() : null)
                .editCount(messageTemplate.getEditCount())
                .createdAt(messageTemplate.getCreatedAt())
                .updatedAt(messageTemplate.getUpdatedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateMessageTemplateDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // Endpoints específicos da entidade
    @GetMapping("/ai-generated/{isAiGenerated}")
    public ResponseEntity<List<MessageTemplateDTO>> findByIsAiGenerated(@PathVariable Boolean isAiGenerated) {
        log.debug("Finding MessageTemplates by AI generated flag via API: {}", isAiGenerated);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByIsAiGenerated(isAiGenerated);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/ai-generated/{isAiGenerated}")
    public ResponseEntity<List<MessageTemplateDTO>> findByCompanyAndIsAiGenerated(
            @PathVariable UUID companyId, 
            @PathVariable Boolean isAiGenerated) {
        log.debug("Finding MessageTemplates by company: {} and AI generated flag: {}", companyId, isAiGenerated);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByCompanyIdAndIsAiGenerated(companyId, isAiGenerated);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/created-by-user/{userId}")
    public ResponseEntity<List<MessageTemplateDTO>> findByCreatedByUserId(@PathVariable UUID userId) {
        log.debug("Finding MessageTemplates by created by user id via API: {}", userId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByCreatedByUserId(userId);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/ai-agent/{aiAgentId}")
    public ResponseEntity<List<MessageTemplateDTO>> findByAiAgentId(@PathVariable UUID aiAgentId) {
        log.debug("Finding MessageTemplates by AI agent id via API: {}", aiAgentId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByAiAgentId(aiAgentId);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/tone/{tone}")
    public ResponseEntity<List<MessageTemplateDTO>> findByTone(@PathVariable String tone) {
        log.debug("Finding MessageTemplates by tone via API: {}", tone);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByTone(tone);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/tone/{tone}")
    public ResponseEntity<List<MessageTemplateDTO>> findByCompanyAndTone(
            @PathVariable UUID companyId, 
            @PathVariable String tone) {
        log.debug("Finding MessageTemplates by company: {} and tone: {}", companyId, tone);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByCompanyIdAndTone(companyId, tone);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<MessageTemplateDTO>> findByNameContaining(@RequestParam String name) {
        log.debug("Finding MessageTemplates by name containing via API: {}", name);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByNameContaining(name);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/search/content")
    public ResponseEntity<List<MessageTemplateDTO>> findByContentContaining(@RequestParam String content) {
        log.debug("Finding MessageTemplates by content containing via API: {}", content);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findByContentContaining(content);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MessageTemplateDTO>> searchTemplates(@RequestParam String term) {
        log.debug("Searching MessageTemplates via API with term: {}", term);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.searchTemplates(term);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByNameAndCompanyId(
            @RequestParam String name, 
            @RequestParam UUID companyId) {
        log.debug("Checking if MessageTemplate exists by name: {} and company: {}", name, companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        boolean exists = messageTemplateService.existsByNameAndCompanyId(name, companyId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count/ai-generated/{isAiGenerated}")
    public ResponseEntity<Long> countByIsAiGenerated(@PathVariable Boolean isAiGenerated) {
        log.debug("Counting MessageTemplates by AI generated flag via API: {}", isAiGenerated);
        
        long count = messageTemplateService.countByIsAiGenerated(isAiGenerated);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/company/{companyId}/ai-generated/{isAiGenerated}")
    public ResponseEntity<Long> countByCompanyAndIsAiGenerated(
            @PathVariable UUID companyId, 
            @PathVariable Boolean isAiGenerated) {
        log.debug("Counting MessageTemplates by company: {} and AI generated flag: {}", companyId, isAiGenerated);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long count = messageTemplateService.countByCompanyIdAndIsAiGenerated(companyId, isAiGenerated);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/created-by-user/{userId}")
    public ResponseEntity<Long> countByCreatedByUserId(@PathVariable UUID userId) {
        log.debug("Counting MessageTemplates by created by user id via API: {}", userId);
        
        long count = messageTemplateService.countByCreatedByUserId(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/ai-agent/{aiAgentId}")
    public ResponseEntity<Long> countByAiAgentId(@PathVariable UUID aiAgentId) {
        log.debug("Counting MessageTemplates by AI agent id via API: {}", aiAgentId);
        
        long count = messageTemplateService.countByAiAgentId(aiAgentId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/increment-edit-count")
    public ResponseEntity<MessageTemplateDTO> incrementEditCount(
            @PathVariable UUID id, 
            @RequestParam UUID editorUserId) {
        log.debug("Incrementing edit count for MessageTemplate via API with id: {} by user: {}", id, editorUserId);
        
        Optional<MessageTemplate> updated = messageTemplateService.incrementEditCount(id, editorUserId);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        MessageTemplateDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<MessageTemplateDTO> cloneTemplate(
            @PathVariable UUID id, 
            @RequestParam String newName) {
        log.debug("Cloning MessageTemplate via API with id: {} and new name: {}", id, newName);
        
        Optional<MessageTemplate> cloned = messageTemplateService.cloneTemplate(id, newName);
        if (cloned.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        MessageTemplateDTO responseDTO = convertToDTO(cloned.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/company/{companyId}/human-created")
    public ResponseEntity<List<MessageTemplateDTO>> getHumanCreatedTemplates(@PathVariable UUID companyId) {
        log.debug("Getting human-created templates for company via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.getHumanCreatedTemplates(companyId);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/ai-generated")
    public ResponseEntity<List<MessageTemplateDTO>> getAiGeneratedTemplates(@PathVariable UUID companyId) {
        log.debug("Getting AI-generated templates for company via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.getAiGeneratedTemplates(companyId);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    /**
     * Override to use soft delete instead of hard delete
     */
    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        log.info("Soft deleting MessageTemplate via API with id: {}", id);
        
        Optional<MessageTemplate> entity = messageTemplateService.findById(id);
        if (entity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        companyContextUtil.ensureCompanyAccess(entity.get().getCompany().getId());
        
        boolean deleted = messageTemplateService.softDeleteById(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Endpoint to restore a soft-deleted template
     */
    @PutMapping("/{id}/restore")
    public ResponseEntity<MessageTemplateDTO> restoreById(@PathVariable UUID id) {
        log.info("Restoring MessageTemplate via API with id: {}", id);
        
        // Use findByIdIncludingDeleted to check if template exists (including soft-deleted ones)
        Optional<MessageTemplate> entity = messageTemplateService.findByIdIncludingDeleted(id);
        if (entity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        companyContextUtil.ensureCompanyAccess(entity.get().getCompany().getId());
        
        boolean restored = messageTemplateService.restoreById(id);
        if (restored) {
            Optional<MessageTemplate> restoredTemplate = messageTemplateService.findById(id);
            if (restoredTemplate.isPresent()) {
                MessageTemplateDTO responseDTO = convertToDTO(restoredTemplate.get());
                return ResponseEntity.ok(responseDTO);
            }
        }
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Endpoint to get soft-deleted templates for a company
     */
    @GetMapping("/company/{companyId}/deleted")
    public ResponseEntity<List<MessageTemplateDTO>> getDeletedTemplates(@PathVariable UUID companyId) {
        log.debug("Getting deleted templates for company via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<MessageTemplate> messageTemplates = messageTemplateService.findDeletedByCompanyId(companyId);
        List<MessageTemplateDTO> responseDTOs = messageTemplates.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }
}