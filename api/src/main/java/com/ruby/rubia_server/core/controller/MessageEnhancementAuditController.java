package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.MessageEnhancementAudit;
import com.ruby.rubia_server.core.service.MessageEnhancementAuditService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/message-enhancement-audit")
@RequiredArgsConstructor
@Slf4j
public class MessageEnhancementAuditController {

    private final MessageEnhancementAuditService auditService;
    private final CompanyContextUtil companyContextUtil;

    @GetMapping("/company/{companyId}")
    public ResponseEntity<Page<MessageEnhancementAudit>> getAuditsByCompany(
            @PathVariable UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.debug("Fetching message enhancement audits for company: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MessageEnhancementAudit> audits = auditService.getAuditsByCompany(companyId, pageable);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/company/{companyId}/stats")
    public ResponseEntity<MessageEnhancementAuditService.EnhancementStats> getCompanyStats(
            @PathVariable UUID companyId) {

        log.debug("Fetching enhancement stats for company: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        MessageEnhancementAuditService.EnhancementStats stats = auditService.getCompanyStats(companyId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/company/{companyId}/by-date-range")
    public ResponseEntity<Page<MessageEnhancementAudit>> getAuditsByDateRange(
            @PathVariable UUID companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching audits for company: {} between {} and {}", companyId, startDate, endDate);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MessageEnhancementAudit> audits = auditService.getAuditsByDateRange(companyId, startDate, endDate, pageable);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/company/{companyId}/by-temperament/{temperament}")
    public ResponseEntity<Page<MessageEnhancementAudit>> getAuditsByTemperament(
            @PathVariable UUID companyId,
            @PathVariable String temperament,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching audits for company: {} with temperament: {}", companyId, temperament);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MessageEnhancementAudit> audits = auditService.getAuditsByTemperament(companyId, temperament, pageable);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/company/{companyId}/by-ai-model/{model}")
    public ResponseEntity<Page<MessageEnhancementAudit>> getAuditsByAiModel(
            @PathVariable UUID companyId,
            @PathVariable String model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching audits for company: {} with AI model: {}", companyId, model);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MessageEnhancementAudit> audits = auditService.getAuditsByAiModel(companyId, model, pageable);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<MessageEnhancementAudit>> getAuditsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching audits for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MessageEnhancementAudit> audits = auditService.getAuditsByUser(userId, pageable);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/ai-agent/{aiAgentId}")
    public ResponseEntity<Page<MessageEnhancementAudit>> getAuditsByAiAgent(
            @PathVariable UUID aiAgentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Fetching audits for AI agent: {}", aiAgentId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<MessageEnhancementAudit> audits = auditService.getAuditsByAiAgent(aiAgentId, pageable);
        return ResponseEntity.ok(audits);
    }

    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<MessageEnhancementAudit>> getAuditsByConversation(
            @PathVariable UUID conversationId) {

        log.debug("Fetching audits for conversation: {}", conversationId);

        List<MessageEnhancementAudit> audits = auditService.getAuditsByConversation(conversationId);
        return ResponseEntity.ok(audits);
    }
}