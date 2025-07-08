package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.MessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.entity.MessageTemplateRevision;
import com.ruby.rubia_server.core.service.MessageTemplateRevisionService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/message-template-revisions")
@Slf4j
public class MessageTemplateRevisionController extends BaseCompanyEntityController<MessageTemplateRevision, CreateMessageTemplateRevisionDTO, UpdateMessageTemplateRevisionDTO, MessageTemplateRevisionDTO> {

    private final MessageTemplateRevisionService messageTemplateRevisionService;

    public MessageTemplateRevisionController(MessageTemplateRevisionService messageTemplateRevisionService, CompanyContextUtil companyContextUtil) {
        super(messageTemplateRevisionService, companyContextUtil);
        this.messageTemplateRevisionService = messageTemplateRevisionService;
    }

    @Override
    protected String getEntityName() {
        return "MessageTemplateRevision";
    }

    @Override
    protected MessageTemplateRevisionDTO convertToDTO(MessageTemplateRevision revision) {
        return MessageTemplateRevisionDTO.builder()
                .id(revision.getId())
                .templateId(revision.getTemplate().getId())
                .templateName(revision.getTemplate().getName())
                .revisionNumber(revision.getRevisionNumber())
                .content(revision.getContent())
                .editedByUserId(revision.getEditedBy() != null ? revision.getEditedBy().getId() : null)
                .editedByUserName(revision.getEditedBy() != null ? revision.getEditedBy().getName() : null)
                .revisionTimestamp(revision.getRevisionTimestamp())
                .createdAt(revision.getCreatedAt())
                .updatedAt(revision.getUpdatedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateMessageTemplateRevisionDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // Endpoints específicos da entidade
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<MessageTemplateRevisionDTO>> findByTemplateId(@PathVariable UUID templateId) {
        log.debug("Finding MessageTemplateRevisions by template id via API: {}", templateId);
        
        List<MessageTemplateRevision> revisions = messageTemplateRevisionService.findByTemplateId(templateId);
        List<MessageTemplateRevisionDTO> responseDTOs = revisions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/template/{templateId}/count")
    public ResponseEntity<Long> countByTemplateId(@PathVariable UUID templateId) {
        log.debug("Counting MessageTemplateRevisions by template id via API: {}", templateId);
        
        long count = messageTemplateRevisionService.countByTemplateId(templateId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/edited-by-user/{userId}")
    public ResponseEntity<List<MessageTemplateRevisionDTO>> findByEditedByUserId(@PathVariable UUID userId) {
        log.debug("Finding MessageTemplateRevisions by edited by user id via API: {}", userId);
        
        List<MessageTemplateRevision> revisions = messageTemplateRevisionService.findByEditedByUserId(userId);
        List<MessageTemplateRevisionDTO> responseDTOs = revisions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/template/{templateId}/revision/{revisionNumber}")
    public ResponseEntity<MessageTemplateRevisionDTO> findByTemplateIdAndRevisionNumber(
            @PathVariable UUID templateId, 
            @PathVariable Integer revisionNumber) {
        log.debug("Finding MessageTemplateRevision by template id: {} and revision number: {}", templateId, revisionNumber);
        
        Optional<MessageTemplateRevision> revision = messageTemplateRevisionService.findByTemplateIdAndRevisionNumber(templateId, revisionNumber);
        if (revision.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        MessageTemplateRevisionDTO responseDTO = convertToDTO(revision.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/template/{templateId}/ordered")
    public ResponseEntity<List<MessageTemplateRevisionDTO>> findByTemplateIdOrderByRevisionNumberDesc(@PathVariable UUID templateId) {
        log.debug("Finding MessageTemplateRevisions by template id ordered by revision number desc via API: {}", templateId);
        
        List<MessageTemplateRevision> revisions = messageTemplateRevisionService.findByTemplateIdOrderByRevisionNumberDesc(templateId);
        List<MessageTemplateRevisionDTO> responseDTOs = revisions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/template/{templateId}/latest")
    public ResponseEntity<MessageTemplateRevisionDTO> getLatestRevision(@PathVariable UUID templateId) {
        log.debug("Getting latest revision for template id via API: {}", templateId);
        
        Optional<MessageTemplateRevision> revision = messageTemplateRevisionService.getLatestRevision(templateId);
        if (revision.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        MessageTemplateRevisionDTO responseDTO = convertToDTO(revision.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/template/{templateId}/original")
    public ResponseEntity<MessageTemplateRevisionDTO> getOriginalRevision(@PathVariable UUID templateId) {
        log.debug("Getting original revision for template id via API: {}", templateId);
        
        Optional<MessageTemplateRevision> revision = messageTemplateRevisionService.getOriginalRevision(templateId);
        if (revision.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        MessageTemplateRevisionDTO responseDTO = convertToDTO(revision.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/template/{templateId}/revision/{revisionNumber}/exists")
    public ResponseEntity<Boolean> existsByTemplateIdAndRevisionNumber(
            @PathVariable UUID templateId, 
            @PathVariable Integer revisionNumber) {
        log.debug("Checking if revision exists for template id: {} and revision number: {}", templateId, revisionNumber);
        
        boolean exists = messageTemplateRevisionService.existsByTemplateIdAndRevisionNumber(templateId, revisionNumber);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/template/{templateId}/revisions/{minRevision}/{maxRevision}")
    public ResponseEntity<List<MessageTemplateRevisionDTO>> findRevisionsBetweenNumbers(
            @PathVariable UUID templateId,
            @PathVariable Integer minRevision,
            @PathVariable Integer maxRevision) {
        log.debug("Finding revisions between {} and {} for template id: {}", minRevision, maxRevision, templateId);
        
        List<MessageTemplateRevision> revisions = messageTemplateRevisionService.findRevisionsBetweenNumbers(templateId, minRevision, maxRevision);
        List<MessageTemplateRevisionDTO> responseDTOs = revisions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/template/{templateId}/next-revision-number")
    public ResponseEntity<Integer> getNextRevisionNumber(@PathVariable UUID templateId) {
        log.debug("Getting next revision number for template id via API: {}", templateId);
        
        Integer nextNumber = messageTemplateRevisionService.getNextRevisionNumber(templateId);
        return ResponseEntity.ok(nextNumber);
    }

    @PostMapping("/template/{templateId}/create-revision")
    public ResponseEntity<MessageTemplateRevisionDTO> createRevisionFromTemplate(
            @PathVariable UUID templateId,
            @RequestParam String content,
            @RequestParam UUID editedByUserId) {
        log.debug("Creating revision from template id: {} with content by user: {}", templateId, editedByUserId);
        
        MessageTemplateRevision revision = messageTemplateRevisionService.createRevisionFromTemplate(templateId, content, editedByUserId);
        MessageTemplateRevisionDTO responseDTO = convertToDTO(revision);
        
        return ResponseEntity.ok(responseDTO);
    }
}