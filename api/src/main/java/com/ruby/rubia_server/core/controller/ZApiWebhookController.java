package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.service.ZApiConnectionMonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/z-api")
@RequiredArgsConstructor
@Slf4j
public class ZApiWebhookController {

    private final ZApiConnectionMonitorService connectionMonitorService;

    @PostMapping("/disconnected/{instanceId}")
    public ResponseEntity<Map<String, Object>> handleDisconnectedWebhook(
            @PathVariable String instanceId,
            @RequestBody Map<String, Object> webhookData) {
        
        try {
            log.info("Received disconnection webhook for instance: {} with data: {}", instanceId, webhookData);
            
            String type = (String) webhookData.get("type");
            Boolean disconnected = (Boolean) webhookData.get("disconnected");
            
            if ("DisconnectedCallback".equals(type) && Boolean.TRUE.equals(disconnected)) {
                connectionMonitorService.handleWebhookDisconnection(instanceId, webhookData);
            }
            
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            log.error("Error processing disconnection webhook for instance {}: {}", instanceId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", true)); // Always return success to Z-API
        }
    }

    @PostMapping("/connected/{instanceId}")
    public ResponseEntity<Map<String, Object>> handleConnectedWebhook(
            @PathVariable String instanceId,
            @RequestBody Map<String, Object> webhookData) {
        
        try {
            log.info("Received connection webhook for instance: {} with data: {}", instanceId, webhookData);
            
            connectionMonitorService.handleWebhookConnection(instanceId, webhookData);
            
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            log.error("Error processing connection webhook for instance {}: {}", instanceId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", true)); // Always return success to Z-API
        }
    }

    @PostMapping("/status/{instanceId}")
    public ResponseEntity<Map<String, Object>> handleStatusWebhook(
            @PathVariable String instanceId,
            @RequestBody Map<String, Object> webhookData) {
        
        try {
            log.info("Received status webhook for instance: {} with data: {}", instanceId, webhookData);
            
            connectionMonitorService.handleStatusUpdate(instanceId, webhookData);
            
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            log.error("Error processing status webhook for instance {}: {}", instanceId, e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", true)); // Always return success to Z-API
        }
    }
}