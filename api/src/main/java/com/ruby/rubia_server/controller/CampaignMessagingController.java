package com.ruby.rubia_server.controller;

import com.ruby.rubia_server.core.service.CampaignMessagingService;
import com.ruby.rubia_server.core.service.CampaignQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Slf4j
public class CampaignMessagingController {

    private final CampaignMessagingService campaignMessagingService;
    private final CampaignQueueService campaignQueueService;

    @PostMapping("/{campaignId}/start-messaging")
    public ResponseEntity<Map<String, Object>> startCampaignMessaging(@PathVariable UUID campaignId) {
        log.info("Iniciando envio de mensagens para campanha: {}", campaignId);
        
        try {
            campaignQueueService.enqueueCampaign(campaignId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campanha adicionada à fila de processamento",
                "campaignId", campaignId
            ));
        } catch (Exception e) {
            log.error("Erro ao adicionar campanha {} à fila: {}", campaignId, e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "campaignId", campaignId
            ));
        }
    }

    @PostMapping("/{campaignId}/pause-messaging")
    public ResponseEntity<Map<String, Object>> pauseCampaignMessaging(@PathVariable UUID campaignId) {
        log.info("Pausando envio de mensagens para campanha: {}", campaignId);
        
        try {
            campaignQueueService.pauseCampaign(campaignId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campanha pausada na fila de processamento",
                "campaignId", campaignId
            ));
        } catch (Exception e) {
            log.error("Erro ao pausar campanha {} na fila: {}", campaignId, e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "campaignId", campaignId
            ));
        }
    }

    @PostMapping("/{campaignId}/resume-messaging")
    public ResponseEntity<Map<String, Object>> resumeCampaignMessaging(@PathVariable UUID campaignId) {
        log.info("Retomando envio de mensagens para campanha: {}", campaignId);
        
        try {
            campaignQueueService.resumeCampaign(campaignId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campanha retomada na fila de processamento",
                "campaignId", campaignId
            ));
        } catch (Exception e) {
            log.error("Erro ao retomar campanha {} na fila: {}", campaignId, e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "campaignId", campaignId
            ));
        }
    }

    @GetMapping("/{campaignId}/messaging-stats")
    public ResponseEntity<Map<String, Object>> getCampaignMessagingStats(@PathVariable UUID campaignId) {
        log.info("Buscando estatísticas de mensagens para campanha: {}", campaignId);
        
        try {
            Map<String, Object> stats = campaignQueueService.getCampaignQueueStats(campaignId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "campaignId", campaignId
            ));
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas para campanha {}: {}", campaignId, e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "campaignId", campaignId
            ));
        }
    }

    @GetMapping("/queue/global-stats")
    public ResponseEntity<Map<String, Object>> getGlobalQueueStats() {
        log.info("Buscando estatísticas globais da fila de campanhas");
        
        try {
            Map<String, Object> stats = campaignQueueService.getGlobalQueueStats();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas globais da fila: {}", e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}