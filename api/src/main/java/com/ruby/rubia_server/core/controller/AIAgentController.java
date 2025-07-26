package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.AIAgentDTO;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.service.AIAgentService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-agents")
@RequiredArgsConstructor
@Slf4j
public class AIAgentController {

    private final AIAgentService aiAgentService;
    private final CompanyContextUtil companyContextUtil;

    @PostMapping
    public ResponseEntity<AIAgentDTO> createAIAgent(@Valid @RequestBody CreateAIAgentDTO createDTO) {
        log.info("Creating AI agent: {}", createDTO.getName());

        // Validate company context
        companyContextUtil.ensureCompanyAccess(createDTO.getCompanyId());

        AIAgent aiAgent = aiAgentService.createAIAgent(createDTO);
        AIAgentDTO responseDTO = convertToDTO(aiAgent);

        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AIAgentDTO> getAIAgent(@PathVariable UUID id) {
        log.debug("Fetching AI agent with id: {}", id);

        return aiAgentService.getAIAgentById(id)
                .map(aiAgent -> {
                    // Validate company context
                    companyContextUtil.ensureCompanyAccess(aiAgent.getCompany().getId());
                    return ResponseEntity.ok(convertToDTO(aiAgent));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<AIAgentDTO>> getAllAIAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.debug("Fetching AI agents - page: {}, size: {}, sortBy: {}, sortDir: {}", page, size, sortBy, sortDir);

        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AIAgent> aiAgents = aiAgentService.getAllAIAgents(pageable);
        Page<AIAgentDTO> responseDTOs = aiAgents.map(this::convertToDTO);

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AIAgentDTO>> getAIAgentsByCompany(@PathVariable UUID companyId) {
        log.debug("Fetching AI agents for company: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        List<AIAgent> aiAgents = aiAgentService.getAIAgentsByCompanyId(companyId);
        List<AIAgentDTO> responseDTOs = aiAgents.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/active")
    public ResponseEntity<List<AIAgentDTO>> getActiveAIAgentsByCompany(@PathVariable UUID companyId) {
        log.debug("Fetching active AI agents for company: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        List<AIAgent> aiAgents = aiAgentService.getActiveAIAgentsByCompanyId(companyId);
        List<AIAgentDTO> responseDTOs = aiAgents.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/ordered")
    public ResponseEntity<List<AIAgentDTO>> getAIAgentsByCompanyOrderedByName(@PathVariable UUID companyId) {
        log.debug("Fetching AI agents for company ordered by name: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        List<AIAgent> aiAgents = aiAgentService.getAIAgentsByCompanyIdOrderByName(companyId);
        List<AIAgentDTO> responseDTOs = aiAgents.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AIAgentDTO> updateAIAgent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAIAgentDTO updateDTO) {

        log.info("Updating AI agent with id: {}", id);

        // First check if AI agent exists and validate company context
        return aiAgentService.getAIAgentById(id)
                .map(existingAgent -> {
                    companyContextUtil.ensureCompanyAccess(existingAgent.getCompany().getId());
                    
                    return aiAgentService.updateAIAgent(id, updateDTO)
                            .map(updatedAgent -> ResponseEntity.ok(convertToDTO(updatedAgent)))
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAIAgent(@PathVariable UUID id) {
        log.info("Deleting AI agent with id: {}", id);

        // First check if AI agent exists and validate company context
        return aiAgentService.getAIAgentById(id)
                .map(aiAgent -> {
                    companyContextUtil.ensureCompanyAccess(aiAgent.getCompany().getId());
                    
                    boolean deleted = aiAgentService.deleteAIAgent(id);
                    return deleted ? 
                        ResponseEntity.noContent().<Void>build() : 
                        ResponseEntity.notFound().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/company/{companyId}/count")
    public ResponseEntity<Long> countAIAgentsByCompany(@PathVariable UUID companyId) {
        log.debug("Counting AI agents for company: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        long count = aiAgentService.countAIAgentsByCompanyId(companyId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/company/{companyId}/count/active")
    public ResponseEntity<Long> countActiveAIAgentsByCompany(@PathVariable UUID companyId) {
        log.debug("Counting active AI agents for company: {}", companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        long count = aiAgentService.countActiveAIAgentsByCompanyId(companyId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/company/{companyId}/exists")
    public ResponseEntity<Boolean> checkAIAgentExists(
            @PathVariable UUID companyId,
            @RequestParam String name) {

        log.debug("Checking if AI agent exists with name: {} for company: {}", name, companyId);

        // Validate company context
        companyContextUtil.ensureCompanyAccess(companyId);

        boolean exists = aiAgentService.existsByNameAndCompanyId(name, companyId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/model/{modelName}")
    public ResponseEntity<List<AIAgentDTO>> getAIAgentsByModelName(@PathVariable String modelName) {
        log.debug("Fetching AI agents by model name: {}", modelName);

        List<AIAgent> aiAgents = aiAgentService.getAIAgentsByModelName(modelName);
        List<AIAgentDTO> responseDTOs = aiAgents.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/model-id/{modelId}")
    public ResponseEntity<List<AIAgentDTO>> getAIAgentsByModelId(@PathVariable UUID modelId) {
        log.debug("Fetching AI agents by model id: {}", modelId);

        List<AIAgent> aiAgents = aiAgentService.getAIAgentsByModelId(modelId);
        List<AIAgentDTO> responseDTOs = aiAgents.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/temperament/{temperament}")
    public ResponseEntity<List<AIAgentDTO>> getAIAgentsByTemperament(@PathVariable String temperament) {
        log.debug("Fetching AI agents by temperament: {}", temperament);

        List<AIAgent> aiAgents = aiAgentService.getAIAgentsByTemperament(temperament);
        List<AIAgentDTO> responseDTOs = aiAgents.stream()
                .map(this::convertToDTO)
                .toList();

        return ResponseEntity.ok(responseDTOs);
    }

    private AIAgentDTO convertToDTO(AIAgent aiAgent) {
        return AIAgentDTO.builder()
                .id(aiAgent.getId())
                .companyId(aiAgent.getCompany().getId())
                .companyName(aiAgent.getCompany().getName())
                .name(aiAgent.getName())
                .description(aiAgent.getDescription())
                .avatarUrl(aiAgent.getAvatarUrl())
                .aiModelId(aiAgent.getAiModel().getId())
                .aiModelName(aiAgent.getAiModel().getName())
                .aiModelDisplayName(aiAgent.getAiModel().getDisplayName())
                .temperament(aiAgent.getTemperament())
                .maxResponseLength(aiAgent.getMaxResponseLength())
                .temperature(aiAgent.getTemperature())
                .isActive(aiAgent.getIsActive())
                .createdAt(aiAgent.getCreatedAt())
                .updatedAt(aiAgent.getUpdatedAt())
                .build();
    }
}