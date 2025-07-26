package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AIAgentService {

    private final AIAgentRepository aiAgentRepository;
    private final CompanyRepository companyRepository;
    private final AIModelRepository aiModelRepository;

    public AIAgent createAIAgent(CreateAIAgentDTO createDTO) {
        log.info("Creating AI agent with name: {} for company: {}", createDTO.getName(), createDTO.getCompanyId());

        // Validate company exists
        Company company = companyRepository.findById(createDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + createDTO.getCompanyId()));

        // Validate AI model exists
        AIModel aiModel = aiModelRepository.findById(createDTO.getAiModelId())
                .orElseThrow(() -> new RuntimeException("AI Model not found with ID: " + createDTO.getAiModelId()));

        // Create AI agent
        AIAgent aiAgent = AIAgent.builder()
                .company(company)
                .aiModel(aiModel)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .avatarUrl(createDTO.getAvatarUrl())
                .temperament(createDTO.getTemperament())
                .maxResponseLength(createDTO.getMaxResponseLength())
                .temperature(createDTO.getTemperature())
                .isActive(createDTO.getIsActive())
                .build();

        aiAgent = aiAgentRepository.save(aiAgent);
        log.info("AI agent created successfully with id: {}", aiAgent.getId());
        
        // Força refresh para garantir que timestamps do banco sejam carregados
        aiAgentRepository.flush();
        return aiAgentRepository.findById(aiAgent.getId()).orElse(aiAgent);
    }

    @Transactional(readOnly = true)
    public Optional<AIAgent> getAIAgentById(UUID id) {
        log.debug("Fetching AI agent with id: {}", id);
        return aiAgentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<AIAgent> getAllAIAgents(Pageable pageable) {
        log.debug("Fetching all AI agents with pagination: {}", pageable);
        return aiAgentRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByCompanyId(UUID companyId) {
        log.debug("Fetching AI agents for company: {}", companyId);
        return aiAgentRepository.findByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getActiveAIAgentsByCompanyId(UUID companyId) {
        log.debug("Fetching active AI agents for company: {}", companyId);
        return aiAgentRepository.findActiveByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByCompanyIdOrderByName(UUID companyId) {
        log.debug("Fetching AI agents for company ordered by name: {}", companyId);
        return aiAgentRepository.findByCompanyIdOrderByName(companyId);
    }

    public Optional<AIAgent> updateAIAgent(UUID id, UpdateAIAgentDTO updateDTO) {
        log.info("Updating AI agent with id: {}", id);

        Optional<AIAgent> aiAgentOpt = aiAgentRepository.findById(id);
        if (aiAgentOpt.isEmpty()) {
            log.warn("AI agent not found with id: {}", id);
            return Optional.empty();
        }

        AIAgent aiAgent = aiAgentOpt.get();

        // Update fields if provided
        if (updateDTO.getName() != null) {
            aiAgent.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            aiAgent.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getAvatarUrl() != null) {
            aiAgent.setAvatarUrl(updateDTO.getAvatarUrl());
        }
        if (updateDTO.getAiModelId() != null) {
            AIModel aiModel = aiModelRepository.findById(updateDTO.getAiModelId())
                    .orElseThrow(() -> new RuntimeException("AI Model not found with ID: " + updateDTO.getAiModelId()));
            aiAgent.setAiModel(aiModel);
        }
        if (updateDTO.getTemperament() != null) {
            aiAgent.setTemperament(updateDTO.getTemperament());
        }
        if (updateDTO.getMaxResponseLength() != null) {
            aiAgent.setMaxResponseLength(updateDTO.getMaxResponseLength());
        }
        if (updateDTO.getTemperature() != null) {
            aiAgent.setTemperature(updateDTO.getTemperature());
        }
        if (updateDTO.getIsActive() != null) {
            aiAgent.setIsActive(updateDTO.getIsActive());
        }

        aiAgent = aiAgentRepository.save(aiAgent);
        log.info("AI agent updated successfully with id: {}", aiAgent.getId());
        
        return Optional.of(aiAgent);
    }

    public boolean deleteAIAgent(UUID id) {
        log.info("Deleting AI agent with id: {}", id);

        if (!aiAgentRepository.existsById(id)) {
            log.warn("AI agent not found with id: {}", id);
            return false;
        }

        aiAgentRepository.deleteById(id);
        log.info("AI agent deleted successfully");
        return true;
    }

    @Transactional(readOnly = true)
    public long countAIAgentsByCompanyId(UUID companyId) {
        log.debug("Counting AI agents for company: {}", companyId);
        return aiAgentRepository.countByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public long countActiveAIAgentsByCompanyId(UUID companyId) {
        log.debug("Counting active AI agents for company: {}", companyId);
        return aiAgentRepository.countActiveByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        log.debug("Checking if AI agent exists with name: {} for company: {}", name, companyId);
        return aiAgentRepository.existsByNameAndCompanyId(name, companyId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByModelId(UUID modelId) {
        log.debug("Fetching AI agents by model id: {}", modelId);
        return aiAgentRepository.findByAiModelId(modelId);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByModelName(String modelName) {
        log.debug("Fetching AI agents by model name: {}", modelName);
        return aiAgentRepository.findByAiModelName(modelName);
    }

    @Transactional(readOnly = true)
    public List<AIAgent> getAIAgentsByTemperament(String temperament) {
        log.debug("Fetching AI agents by temperament: {}", temperament);
        return aiAgentRepository.findByTemperament(temperament);
    }
}