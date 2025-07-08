package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CreateAILogDTO;
import com.ruby.rubia_server.core.dto.UpdateAILogDTO;
import com.ruby.rubia_server.core.entity.AILog;
import com.ruby.rubia_server.core.enums.AILogStatus;
import com.ruby.rubia_server.core.service.AILogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-logs")
@RequiredArgsConstructor
@Slf4j
public class AILogController {

    private final AILogService aiLogService;

    @PostMapping
    public ResponseEntity<AILog> createAILog(@Valid @RequestBody CreateAILogDTO createDTO) {
        log.info("Creating new AI log for company: {}", createDTO.getCompanyId());
        AILog aiLog = aiLogService.createAILog(createDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(aiLog);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AILog> getAILogById(@PathVariable UUID id) {
        log.info("Fetching AI log with ID: {}", id);
        Optional<AILog> aiLog = aiLogService.getAILogById(id);
        return aiLog.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<AILog>> getAllAILogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AILog> aiLogs = aiLogService.getAllAILogs(pageable);
        return ResponseEntity.ok(aiLogs);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AILog>> getAILogsByCompanyId(@PathVariable UUID companyId) {
        log.info("Fetching AI logs for company: {}", companyId);
        List<AILog> aiLogs = aiLogService.getAILogsByCompanyId(companyId);
        return ResponseEntity.ok(aiLogs);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AILog>> getAILogsByStatus(@PathVariable AILogStatus status) {
        log.info("Fetching AI logs with status: {}", status);
        List<AILog> aiLogs = aiLogService.getAILogsByStatus(status);
        return ResponseEntity.ok(aiLogs);
    }

    @GetMapping("/ai-agent/{aiAgentId}")
    public ResponseEntity<List<AILog>> getAILogsByAIAgentId(@PathVariable UUID aiAgentId) {
        log.info("Fetching AI logs for AI agent: {}", aiAgentId);
        List<AILog> aiLogs = aiLogService.getAILogsByAIAgentId(aiAgentId);
        return ResponseEntity.ok(aiLogs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AILog> updateAILog(@PathVariable UUID id, @Valid @RequestBody UpdateAILogDTO updateDTO) {
        log.info("Updating AI log with ID: {}", id);
        Optional<AILog> updatedAILog = aiLogService.updateAILog(id, updateDTO);
        return updatedAILog.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAILog(@PathVariable UUID id) {
        log.info("Deleting AI log with ID: {}", id);
        boolean deleted = aiLogService.deleteAILog(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/company/{companyId}/total-cost")
    public ResponseEntity<BigDecimal> getTotalCostByCompanyId(@PathVariable UUID companyId) {
        log.info("Calculating total cost for company: {}", companyId);
        BigDecimal totalCost = aiLogService.getTotalCostByCompanyId(companyId);
        return ResponseEntity.ok(totalCost);
    }

    @GetMapping("/company/{companyId}/total-tokens")
    public ResponseEntity<Long> getTotalTokensUsedByCompanyId(@PathVariable UUID companyId) {
        log.info("Calculating total tokens used for company: {}", companyId);
        Long totalTokens = aiLogService.getTotalTokensUsedByCompanyId(companyId);
        return ResponseEntity.ok(totalTokens);
    }

    @GetMapping("/company/{companyId}/count")
    public ResponseEntity<Long> countAILogsByCompanyIdAndStatus(
            @PathVariable UUID companyId,
            @RequestParam AILogStatus status) {
        log.info("Counting AI logs for company: {} with status: {}", companyId, status);
        long count = aiLogService.countAILogsByCompanyIdAndStatus(companyId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/date-range")
    public ResponseEntity<List<AILog>> getAILogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("Fetching AI logs between {} and {}", startDate, endDate);
        List<AILog> aiLogs = aiLogService.getAILogsByDateRange(startDate, endDate);
        return ResponseEntity.ok(aiLogs);
    }
}