package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateUserAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateUserAIAgentDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class UserAIAgentService extends BaseCompanyEntityService<UserAIAgent, CreateUserAIAgentDTO, UpdateUserAIAgentDTO> {

    private final UserAIAgentRepository userAIAgentRepository;
    private final UserRepository userRepository;
    private final AIAgentRepository aiAgentRepository;

    public UserAIAgentService(UserAIAgentRepository userAIAgentRepository,
                             CompanyRepository companyRepository,
                             UserRepository userRepository,
                             AIAgentRepository aiAgentRepository,
                             EntityRelationshipValidator relationshipValidator) {
        super(userAIAgentRepository, companyRepository, relationshipValidator);
        this.userAIAgentRepository = userAIAgentRepository;
        this.userRepository = userRepository;
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    protected String getEntityName() {
        return "UserAIAgent";
    }

    @Override
    protected UserAIAgent buildEntityFromDTO(CreateUserAIAgentDTO createDTO) {
        User user = userRepository.findById(createDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getUserId()));

        AIAgent aiAgent = aiAgentRepository.findById(createDTO.getAiAgentId())
                .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + createDTO.getAiAgentId()));

        // Check if assignment already exists
        if (userAIAgentRepository.existsByUserIdAndAiAgentId(createDTO.getUserId(), createDTO.getAiAgentId())) {
            throw new RuntimeException("User is already assigned to this AI Agent");
        }

        return UserAIAgent.builder()
                .user(user)
                .aiAgent(aiAgent)
                .isDefault(createDTO.getIsDefault())
                .build();
    }

    @Override
    protected void updateEntityFromDTO(UserAIAgent userAIAgent, UpdateUserAIAgentDTO updateDTO) {
        if (updateDTO.getIsDefault() != null) {
            userAIAgent.setIsDefault(updateDTO.getIsDefault());
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateUserAIAgentDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<UserAIAgent> findByUserId(UUID userId) {
        log.debug("Finding UserAIAgents by user id: {}", userId);
        return userAIAgentRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<UserAIAgent> findByAiAgentId(UUID aiAgentId) {
        log.debug("Finding UserAIAgents by ai agent id: {}", aiAgentId);
        return userAIAgentRepository.findByAiAgentId(aiAgentId);
    }

    @Transactional(readOnly = true)
    public Optional<UserAIAgent> findByUserIdAndAiAgentId(UUID userId, UUID aiAgentId) {
        log.debug("Finding UserAIAgent by user id: {} and ai agent id: {}", userId, aiAgentId);
        return userAIAgentRepository.findByUserIdAndAiAgentId(userId, aiAgentId);
    }

    @Transactional(readOnly = true)
    public boolean existsByUserIdAndAiAgentId(UUID userId, UUID aiAgentId) {
        log.debug("Checking if UserAIAgent exists by user id: {} and ai agent id: {}", userId, aiAgentId);
        return userAIAgentRepository.existsByUserIdAndAiAgentId(userId, aiAgentId);
    }

    @Transactional(readOnly = true)
    public List<UserAIAgent> findByIsDefault(Boolean isDefault) {
        log.debug("Finding UserAIAgents by isDefault: {}", isDefault);
        return userAIAgentRepository.findByIsDefault(isDefault);
    }

    @Transactional(readOnly = true)
    public Optional<UserAIAgent> findByUserIdAndIsDefault(UUID userId, Boolean isDefault) {
        log.debug("Finding UserAIAgent by user id: {} and isDefault: {}", userId, isDefault);
        return userAIAgentRepository.findByUserIdAndIsDefault(userId, isDefault);
    }

    @Transactional(readOnly = true)
    public long countByAiAgentId(UUID aiAgentId) {
        log.debug("Counting UserAIAgents by ai agent id: {}", aiAgentId);
        return userAIAgentRepository.countByAiAgentId(aiAgentId);
    }

    @Transactional(readOnly = true)
    public long countByUserId(UUID userId) {
        log.debug("Counting UserAIAgents by user id: {}", userId);
        return userAIAgentRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public long countByIsDefault(Boolean isDefault) {
        log.debug("Counting UserAIAgents by isDefault: {}", isDefault);
        return userAIAgentRepository.countByIsDefault(isDefault);
    }

    @Transactional
    public Optional<UserAIAgent> setAsDefault(UUID userAIAgentId, Boolean isDefault) {
        log.debug("Setting UserAIAgent as default: {} with status: {}", userAIAgentId, isDefault);

        Optional<UserAIAgent> optionalUserAIAgent = userAIAgentRepository.findById(userAIAgentId);
        if (optionalUserAIAgent.isEmpty()) {
            log.warn("UserAIAgent not found with id: {}", userAIAgentId);
            return Optional.empty();
        }

        UserAIAgent userAIAgent = optionalUserAIAgent.get();
        
        // If setting as default, clear other defaults for this user
        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultForUser(userAIAgent.getUser().getId());
        }
        
        userAIAgent.setIsDefault(isDefault);
        UserAIAgent updatedUserAIAgent = userAIAgentRepository.save(userAIAgent);
        
        log.debug("UserAIAgent default status updated successfully for id: {}", updatedUserAIAgent.getId());
        return Optional.of(updatedUserAIAgent);
    }

    @Transactional
    public void clearDefaultForUser(UUID userId) {
        log.debug("Clearing default UserAIAgent for user: {}", userId);
        
        List<UserAIAgent> defaultAssignments = userAIAgentRepository.findByUserId(userId)
                .stream()
                .filter(assignment -> Boolean.TRUE.equals(assignment.getIsDefault()))
                .toList();
        
        defaultAssignments.forEach(assignment -> assignment.setIsDefault(false));
        userAIAgentRepository.saveAll(defaultAssignments);
        
        log.debug("Cleared {} default assignments for user: {}", defaultAssignments.size(), userId);
    }

    @Transactional
    public UserAIAgent assignUserToAgent(UUID userId, UUID aiAgentId, Boolean isDefault) {
        log.debug("Assigning user: {} to AI agent: {} with default: {}", userId, aiAgentId, isDefault);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        AIAgent aiAgent = aiAgentRepository.findById(aiAgentId)
                .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + aiAgentId));

        // Check if assignment already exists
        if (userAIAgentRepository.existsByUserIdAndAiAgentId(userId, aiAgentId)) {
            throw new RuntimeException("User is already assigned to this AI Agent");
        }

        // If setting as default, clear other defaults for this user
        if (Boolean.TRUE.equals(isDefault)) {
            clearDefaultForUser(userId);
        }

        UserAIAgent userAIAgent = UserAIAgent.builder()
                .company(user.getCompany()) // Assuming user has company relationship
                .user(user)
                .aiAgent(aiAgent)
                .isDefault(isDefault)
                .build();

        UserAIAgent savedUserAIAgent = userAIAgentRepository.save(userAIAgent);
        log.debug("User assigned to AI agent successfully with id: {}", savedUserAIAgent.getId());

        return savedUserAIAgent;
    }

    @Transactional
    public boolean removeUserFromAgent(UUID userId, UUID aiAgentId) {
        log.debug("Removing user: {} from AI agent: {}", userId, aiAgentId);

        Optional<UserAIAgent> optionalUserAIAgent = userAIAgentRepository.findByUserIdAndAiAgentId(userId, aiAgentId);
        if (optionalUserAIAgent.isEmpty()) {
            log.warn("UserAIAgent assignment not found for user: {} and ai agent: {}", userId, aiAgentId);
            return false;
        }

        userAIAgentRepository.delete(optionalUserAIAgent.get());
        log.debug("User removed from AI agent successfully");
        return true;
    }
}