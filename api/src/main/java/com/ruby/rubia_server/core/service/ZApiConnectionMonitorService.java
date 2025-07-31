package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import com.ruby.rubia_server.core.repository.WhatsAppInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZApiConnectionMonitorService {

    private final WhatsAppInstanceRepository whatsAppInstanceRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final WebSocketNotificationService webSocketNotificationService;

    @Value("${zapi.clientToken}")
    private String clientToken;

    @Value("${z-api.base-url:https://api.z-api.io}")
    private String baseUrl;

    @Async
    public CompletableFuture<Void> handleWebhookDisconnection(String instanceId, Map<String, Object> webhookData) {
        log.info("Processing disconnection webhook for instance: {}", instanceId);
        
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            log.warn("Instance not found in database: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }

        WhatsAppInstance instance = instanceOpt.get();
        
        // Update instance status
        instance.setStatus(WhatsAppInstanceStatus.DISCONNECTED);
        instance.setLastStatusCheck(LocalDateTime.now());
        
        // Extract error message if available
        String errorMessage = (String) webhookData.get("error");
        if (errorMessage != null) {
            instance.setErrorMessage(errorMessage);
        }

        whatsAppInstanceRepository.save(instance);

        // Notify frontend via WebSocket
        notifyStatusChange(instance, "DISCONNECTED");

        log.info("Instance {} marked as disconnected", instanceId);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> handleWebhookConnection(String instanceId, Map<String, Object> webhookData) {
        log.info("Processing connection webhook for instance: {}", instanceId);
        
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            log.warn("Instance not found in database: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }

        WhatsAppInstance instance = instanceOpt.get();
        
        // Update instance status
        instance.setStatus(WhatsAppInstanceStatus.CONNECTED);
        instance.setLastConnectedAt(LocalDateTime.now());
        instance.setLastStatusCheck(LocalDateTime.now());
        instance.setErrorMessage(null); // Clear any previous error

        whatsAppInstanceRepository.save(instance);

        // Notify frontend via WebSocket
        notifyStatusChange(instance, "CONNECTED");

        log.info("Instance {} marked as connected", instanceId);
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> handleStatusUpdate(String instanceId, Map<String, Object> webhookData) {
        log.info("Processing status update webhook for instance: {}", instanceId);
        
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            log.warn("Instance not found in database: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }

        WhatsAppInstance instance = instanceOpt.get();
        instance.setLastStatusCheck(LocalDateTime.now());
        
        // Process different status types from webhook
        String status = (String) webhookData.get("status");
        Boolean connected = (Boolean) webhookData.get("connected");
        
        if (connected != null) {
            if (connected) {
                instance.setStatus(WhatsAppInstanceStatus.CONNECTED);
                instance.setLastConnectedAt(LocalDateTime.now());
                instance.setErrorMessage(null);
            } else {
                instance.setStatus(WhatsAppInstanceStatus.DISCONNECTED);
                String errorMessage = (String) webhookData.get("error");
                if (errorMessage != null) {
                    instance.setErrorMessage(errorMessage);
                }
            }
        }

        whatsAppInstanceRepository.save(instance);
        
        // Notify frontend
        notifyStatusChange(instance, connected ? "CONNECTED" : "DISCONNECTED");

        return CompletableFuture.completedFuture(null);
    }

    public Map<String, Object> checkInstanceStatus(String instanceId, String token) {
        String url = String.format("%s/instances/%s/token/%s/status", baseUrl, instanceId, token);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("client-token", clientToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("🔍 Checking Z-API status for instance {} at URL: {}", instanceId, url);
        log.debug("🔍 Using client token: {}...", clientToken != null ? clientToken.substring(0, Math.min(10, clientToken.length())) : "null");

        try {
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                Map.class
            );

            Map<String, Object> result = response.getBody();
            log.info("✅ Z-API response for instance {}: {}", instanceId, result);
            
            return result;
            
        } catch (Exception e) {
            log.error("❌ Error checking status for instance {}: {}", instanceId, e.getMessage(), e);
            return Map.of(
                "connected", false,
                "error", "Failed to check status: " + e.getMessage(),
                "smartphoneConnected", false
            );
        }
    }

    @Async
    public CompletableFuture<Void> updateInstanceFromStatusCheck(WhatsAppInstance instance) {
        log.info("🔄 Updating instance {} from status check. Current status: {}", 
            instance.getInstanceId(), instance.getStatus());
            
        Map<String, Object> statusResult = checkInstanceStatus(instance.getInstanceId(), instance.getAccessToken());
        
        Boolean connected = (Boolean) statusResult.get("connected");
        String error = (String) statusResult.get("error");
        
        log.info("📊 Z-API status result for instance {}: connected={}, error={}", 
            instance.getInstanceId(), connected, error);
        
        WhatsAppInstanceStatus oldStatus = instance.getStatus();
        WhatsAppInstanceStatus newStatus;
        
        if (Boolean.TRUE.equals(connected)) {
            newStatus = WhatsAppInstanceStatus.CONNECTED;
            instance.setLastConnectedAt(LocalDateTime.now());
            instance.setErrorMessage(null);
        } else {
            newStatus = WhatsAppInstanceStatus.DISCONNECTED;
            instance.setErrorMessage(error);
        }

        log.info("📈 Status comparison for instance {}: {} -> {}", 
            instance.getInstanceId(), oldStatus, newStatus);

        // Only update if status changed
        if (instance.getStatus() != newStatus) {
            instance.setStatus(newStatus);
            whatsAppInstanceRepository.save(instance);
            
            // Notify frontend of status change
            notifyStatusChange(instance, newStatus.name());
            
            log.warn("⚠️  Instance {} status CHANGED: {} -> {}", 
                instance.getInstanceId(), oldStatus, newStatus);
        } else {
            log.debug("✓ Instance {} status unchanged: {}", 
                instance.getInstanceId(), newStatus);
        }

        instance.setLastStatusCheck(LocalDateTime.now());
        whatsAppInstanceRepository.save(instance);

        return CompletableFuture.completedFuture(null);
    }

    // Scheduled task to check all active instances every 2 minutes
    @Scheduled(fixedRate = 120000) // 2 minutes
    public void periodicStatusCheck() {
        List<WhatsAppInstance> activeInstances = whatsAppInstanceRepository
            .findByIsActiveTrueAndInstanceIdIsNotNullAndAccessTokenIsNotNull();

        log.info("🔄 Starting periodic status check for {} active instances", activeInstances.size());
        
        if (activeInstances.isEmpty()) {
            log.info("📭 No active instances found for periodic check");
            return;
        }

        for (WhatsAppInstance instance : activeInstances) {
            // Skip if last check was less than 1 minute ago to avoid too frequent checks
            if (instance.getLastStatusCheck() != null && 
                instance.getLastStatusCheck().isAfter(LocalDateTime.now().minusMinutes(1))) {
                log.debug("⏭️  Skipping instance {} - checked {} minute(s) ago", 
                    instance.getInstanceId(),
                    java.time.Duration.between(instance.getLastStatusCheck(), LocalDateTime.now()).toMinutes());
                continue;
            }

            log.info("🎯 Checking instance: {} ({})", 
                instance.getInstanceId(), instance.getPhoneNumber());
            updateInstanceFromStatusCheck(instance);
        }
        
        log.info("✅ Periodic status check completed");
    }

    private void notifyStatusChange(WhatsAppInstance instance, String status) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "INSTANCE_STATUS_CHANGE",
                "instanceId", instance.getInstanceId(),
                "status", status,
                "phoneNumber", instance.getPhoneNumber(),
                "displayName", instance.getDisplayName() != null ? instance.getDisplayName() : "",
                "timestamp", LocalDateTime.now().toString()
            );

            // Send to company-specific channel
            String channel = "company-" + instance.getCompany().getId();
            webSocketNotificationService.sendToChannel(channel, notification);
            
        } catch (Exception e) {
            log.error("Error sending WebSocket notification for instance status change: {}", e.getMessage());
        }
    }

    public boolean configureDisconnectionWebhook(String instanceId, String token) {
        String webhookUrl = String.format("%s/api/webhooks/z-api/disconnected/%s", 
            getCurrentBaseUrl(), instanceId);
        
        return configureWebhook(instanceId, token, "update-webhook-disconnected", webhookUrl);
    }

    public boolean configureConnectionWebhook(String instanceId, String token) {
        String webhookUrl = String.format("%s/api/webhooks/z-api/connected/%s", 
            getCurrentBaseUrl(), instanceId);
        
        return configureWebhook(instanceId, token, "update-webhook-connected", webhookUrl);
    }

    private boolean configureWebhook(String instanceId, String token, String webhookType, String webhookUrl) {
        String url = String.format("%s/instances/%s/token/%s/%s", baseUrl, instanceId, token, webhookType);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("client-token", clientToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("value", webhookUrl);

        try {
            org.springframework.http.HttpEntity<Map<String, String>> entity = 
                new org.springframework.http.HttpEntity<>(body, headers);
            
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.PUT, 
                entity, 
                Map.class
            );

            log.info("Configured {} webhook for instance {}: {}", webhookType, instanceId, webhookUrl);
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("Error configuring {} webhook for instance {}: {}", webhookType, instanceId, e.getMessage());
            return false;
        }
    }

    @Value("${z-api.webhook.base-url:https://your-domain.com}")
    private String webhookBaseUrl;

    private String getCurrentBaseUrl() {
        return webhookBaseUrl;
    }
}