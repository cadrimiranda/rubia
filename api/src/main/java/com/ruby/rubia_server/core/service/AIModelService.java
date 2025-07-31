package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AIModelService {

    private final AIModelRepository aiModelRepository;

    @Cacheable(value = "activeAIModels", unless = "#result.size() == 0")
    public List<AIModel> getActiveModels() {
        log.debug("Fetching active AI models");
        return aiModelRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc();
    }

    public List<AIModel> getAllModels() {
        log.debug("Fetching all AI models");
        return aiModelRepository.findAllByOrderBySortOrderAscNameAsc();
    }

    public List<AIModel> getModelsByProvider(String provider) {
        log.debug("Fetching AI models by provider: {}", provider);
        return aiModelRepository.findByProviderOrderBySortOrderAscNameAsc(provider);
    }

    public Optional<AIModel> getModelById(UUID id) {
        log.debug("Fetching AI model with id: {}", id);
        return aiModelRepository.findById(id);
    }

    public Optional<AIModel> getModelByName(String name) {
        log.debug("Fetching AI model with name: {}", name);
        return aiModelRepository.findByName(name);
    }

    public boolean existsByName(String name) {
        return aiModelRepository.existsByName(name);
    }

    public long countActiveModels() {
        return aiModelRepository.countActiveModels();
    }
}