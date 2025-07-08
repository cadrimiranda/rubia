package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.UserAIAgentDTO;
import com.ruby.rubia_server.core.dto.CreateUserAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateUserAIAgentDTO;
import com.ruby.rubia_server.core.entity.UserAIAgent;
import com.ruby.rubia_server.core.service.UserAIAgentService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user-ai-agents")
@Slf4j
public class UserAIAgentController extends BaseCompanyEntityController<UserAIAgent, CreateUserAIAgentDTO, UpdateUserAIAgentDTO, UserAIAgentDTO> {

    private final UserAIAgentService userAIAgentService;

    public UserAIAgentController(UserAIAgentService userAIAgentService, CompanyContextUtil companyContextUtil) {
        super(userAIAgentService, companyContextUtil);
        this.userAIAgentService = userAIAgentService;
    }

    @Override
    protected String getEntityName() {
        return "UserAIAgent";
    }

    @Override
    protected UserAIAgentDTO convertToDTO(UserAIAgent userAIAgent) {
        return UserAIAgentDTO.builder()
                .id(userAIAgent.getId())
                .companyId(userAIAgent.getCompany().getId())
                .companyName(userAIAgent.getCompany().getName())
                .userId(userAIAgent.getUser().getId())
                .userName(userAIAgent.getUser().getName())
                .aiAgentId(userAIAgent.getAiAgent().getId())
                .aiAgentName(userAIAgent.getAiAgent().getName())
                .isDefault(userAIAgent.getIsDefault())
                .assignedAt(userAIAgent.getAssignedAt())
                .createdAt(userAIAgent.getCreatedAt())
                .updatedAt(userAIAgent.getUpdatedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateUserAIAgentDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // Endpoints específicos da entidade
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAIAgentDTO>> findByUserId(@PathVariable UUID userId) {
        log.debug("Finding UserAIAgents by user id via API: {}", userId);
        
        List<UserAIAgent> userAIAgents = userAIAgentService.findByUserId(userId);
        List<UserAIAgentDTO> responseDTOs = userAIAgents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/ai-agent/{aiAgentId}")
    public ResponseEntity<List<UserAIAgentDTO>> findByAiAgentId(@PathVariable UUID aiAgentId) {
        log.debug("Finding UserAIAgents by ai agent id via API: {}", aiAgentId);
        
        List<UserAIAgent> userAIAgents = userAIAgentService.findByAiAgentId(aiAgentId);
        List<UserAIAgentDTO> responseDTOs = userAIAgents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/user/{userId}/ai-agent/{aiAgentId}")
    public ResponseEntity<UserAIAgentDTO> findByUserIdAndAiAgentId(
            @PathVariable UUID userId, 
            @PathVariable UUID aiAgentId) {
        log.debug("Finding UserAIAgent by user id: {} and ai agent id: {}", userId, aiAgentId);
        
        Optional<UserAIAgent> userAIAgent = userAIAgentService.findByUserIdAndAiAgentId(userId, aiAgentId);
        if (userAIAgent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserAIAgentDTO responseDTO = convertToDTO(userAIAgent.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/default/{isDefault}")
    public ResponseEntity<List<UserAIAgentDTO>> findByIsDefault(@PathVariable Boolean isDefault) {
        log.debug("Finding UserAIAgents by isDefault via API: {}", isDefault);
        
        List<UserAIAgent> userAIAgents = userAIAgentService.findByIsDefault(isDefault);
        List<UserAIAgentDTO> responseDTOs = userAIAgents.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/user/{userId}/default")
    public ResponseEntity<UserAIAgentDTO> findByUserIdAndIsDefault(@PathVariable UUID userId) {
        log.debug("Finding default UserAIAgent for user id via API: {}", userId);
        
        Optional<UserAIAgent> userAIAgent = userAIAgentService.findByUserIdAndIsDefault(userId, true);
        if (userAIAgent.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserAIAgentDTO responseDTO = convertToDTO(userAIAgent.get());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/user/{userId}/ai-agent/{aiAgentId}/exists")
    public ResponseEntity<Boolean> existsByUserIdAndAiAgentId(
            @PathVariable UUID userId, 
            @PathVariable UUID aiAgentId) {
        log.debug("Checking if UserAIAgent exists by user id: {} and ai agent id: {}", userId, aiAgentId);
        
        boolean exists = userAIAgentService.existsByUserIdAndAiAgentId(userId, aiAgentId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count/ai-agent/{aiAgentId}")
    public ResponseEntity<Long> countByAiAgentId(@PathVariable UUID aiAgentId) {
        log.debug("Counting UserAIAgents by ai agent id via API: {}", aiAgentId);
        
        long count = userAIAgentService.countByAiAgentId(aiAgentId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/user/{userId}")
    public ResponseEntity<Long> countByUserId(@PathVariable UUID userId) {
        log.debug("Counting UserAIAgents by user id via API: {}", userId);
        
        long count = userAIAgentService.countByUserId(userId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/default/{isDefault}")
    public ResponseEntity<Long> countByIsDefault(@PathVariable Boolean isDefault) {
        log.debug("Counting UserAIAgents by isDefault via API: {}", isDefault);
        
        long count = userAIAgentService.countByIsDefault(isDefault);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/set-default")
    public ResponseEntity<UserAIAgentDTO> setAsDefault(
            @PathVariable UUID id, 
            @RequestParam Boolean isDefault) {
        log.debug("Setting UserAIAgent as default via API with id: {} and status: {}", id, isDefault);
        
        Optional<UserAIAgent> updated = userAIAgentService.setAsDefault(id, isDefault);
        if (updated.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        UserAIAgentDTO responseDTO = convertToDTO(updated.get());
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/user/{userId}/clear-defaults")
    public ResponseEntity<Void> clearDefaultForUser(@PathVariable UUID userId) {
        log.debug("Clearing default UserAIAgent for user via API: {}", userId);
        
        userAIAgentService.clearDefaultForUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/assign")
    public ResponseEntity<UserAIAgentDTO> assignUserToAgent(
            @RequestParam UUID userId,
            @RequestParam UUID aiAgentId,
            @RequestParam(defaultValue = "false") Boolean isDefault) {
        log.debug("Assigning user: {} to AI agent: {} with default: {}", userId, aiAgentId, isDefault);
        
        UserAIAgent userAIAgent = userAIAgentService.assignUserToAgent(userId, aiAgentId, isDefault);
        UserAIAgentDTO responseDTO = convertToDTO(userAIAgent);
        
        return ResponseEntity.ok(responseDTO);
    }

    @DeleteMapping("/user/{userId}/ai-agent/{aiAgentId}")
    public ResponseEntity<Void> removeUserFromAgent(
            @PathVariable UUID userId, 
            @PathVariable UUID aiAgentId) {
        log.debug("Removing user: {} from AI agent: {}", userId, aiAgentId);
        
        boolean removed = userAIAgentService.removeUserFromAgent(userId, aiAgentId);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.noContent().build();
    }
}