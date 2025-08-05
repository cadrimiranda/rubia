package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.service.AIModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai-models")
@RequiredArgsConstructor
@Slf4j
public class AIModelController {

    private final AIModelService aiModelService;

    @GetMapping
    public ResponseEntity<List<AIModel>> getAllModels() {
        log.info("GET /api/ai-models - Fetching all AI models");
        List<AIModel> models = aiModelService.getAllModels();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/active")
    public ResponseEntity<List<AIModel>> getActiveModels() {
        log.info("GET /api/ai-models/active - Fetching active AI models");
        List<AIModel> models = aiModelService.getActiveModels();
        return ResponseEntity.ok(models);
    }

    @GetMapping("/provider/{provider}")
    public ResponseEntity<List<AIModel>> getModelsByProvider(@PathVariable String provider) {
        log.info("GET /api/ai-models/provider/{} - Fetching AI models by provider", provider);
        List<AIModel> models = aiModelService.getModelsByProvider(provider);
        return ResponseEntity.ok(models);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AIModel> getModelById(@PathVariable UUID id) {
        log.info("GET /api/ai-models/{} - Fetching AI model by id", id);
        return aiModelService.getModelById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}