package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.EnhanceTemplateDTO;
import com.ruby.rubia_server.core.dto.EnhancedTemplateResponseDTO;
import com.ruby.rubia_server.core.dto.SaveTemplateWithAIMetadataDTO;
import com.ruby.rubia_server.core.dto.MessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.service.TemplateEnhancementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/template-enhancement")
@RequiredArgsConstructor
@Slf4j
public class TemplateEnhancementController {

    private final TemplateEnhancementService templateEnhancementService;

    @PostMapping("/enhance")
    public ResponseEntity<EnhancedTemplateResponseDTO> enhanceTemplate(
            @Valid @RequestBody EnhanceTemplateDTO request) {
        
        log.info("POST /api/template-enhancement/enhance - Enhancing template for company: {}", request.getCompanyId());
        
        try {
            EnhancedTemplateResponseDTO response = templateEnhancementService.enhanceTemplate(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error enhancing template: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/save-with-ai-metadata")
    public ResponseEntity<MessageTemplateRevisionDTO> saveTemplateWithAIMetadata(
            @Valid @RequestBody SaveTemplateWithAIMetadataDTO request) {
        
        log.info("POST /api/template-enhancement/save-with-ai-metadata - Saving template with AI metadata for template: {}", request.getTemplateId());
        
        try {
            MessageTemplateRevisionDTO response = templateEnhancementService.saveTemplateWithAIMetadata(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error saving template with AI metadata: {}", e.getMessage(), e);
            throw e;
        }
    }
}