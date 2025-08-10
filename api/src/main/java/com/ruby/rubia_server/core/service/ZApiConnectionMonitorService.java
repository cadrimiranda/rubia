package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
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
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            log.warn("Instance not found in database: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }

        WhatsAppInstance instance = instanceOpt.get();
        
        // Extract error message if available
        String errorMessage = (String) webhookData.get("error");
        
        // Notify frontend via WebSocket with real-time status
        Map<String, Object> statusData = Map.of(
            "connected", false,
            "error", errorMessage != null ? errorMessage : "Device has been disconnected"
        );
        notifyStatusChange(instance, "DISCONNECTED", statusData);

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> handleWebhookConnection(String instanceId, Map<String, Object> webhookData) {
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            log.warn("Instance not found in database: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }

        WhatsAppInstance instance = instanceOpt.get();
        
        // Notify frontend via WebSocket with real-time status
        Map<String, Object> statusData = Map.of(
            "connected", true,
            "error", null,
            "justConnected", true, // Flag to trigger redirection to chat
            "moment", webhookData.get("moment")
        );
        notifyStatusChange(instance, "CONNECTED", statusData);

        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> handleStatusUpdate(String instanceId, Map<String, Object> webhookData) {
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            log.warn("Instance not found in database: {}", instanceId);
            return CompletableFuture.completedFuture(null);
        }

        WhatsAppInstance instance = instanceOpt.get();
        
        // Process different status types from webhook
        Boolean connected = (Boolean) webhookData.get("connected");
        String errorMessage = (String) webhookData.get("error");
        
        // Notify frontend with real-time status
        Map<String, Object> statusData = Map.of(
            "connected", connected != null ? connected : false,
            "error", errorMessage
        );
        
        String status = connected != null && connected ? "CONNECTED" : "DISCONNECTED";
        notifyStatusChange(instance, status, statusData);

        return CompletableFuture.completedFuture(null);
    }

    public Map<String, Object> checkInstanceStatus(String instanceId, String token) {
        String url = String.format("%s/instances/%s/token/%s/status", baseUrl, instanceId, token);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("client-token", clientToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                url, 
                org.springframework.http.HttpMethod.GET, 
                entity, 
                Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            return Map.of(
                "connected", false,
                "error", "Failed to check status: " + e.getMessage(),
                "smartphoneConnected", false
            );
        }
    }

    public Map<String, Object> getInstanceStatus(WhatsAppInstance instance) {
        return checkInstanceStatus(instance.getInstanceId(), instance.getAccessToken());
    }
    
    public Map<String, Object> getInstanceStatus(String instanceId) {
        Optional<WhatsAppInstance> instanceOpt = whatsAppInstanceRepository.findByInstanceId(instanceId);
        
        if (instanceOpt.isEmpty()) {
            return Map.of(
                "connected", false,
                "error", "Instance not found",
                "smartphoneConnected", false
            );
        }
        
        return getInstanceStatus(instanceOpt.get());
    }

    // Optional: Scheduled task to proactively notify frontend of status changes
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void periodicStatusNotification() {
        List<WhatsAppInstance> activeInstances = whatsAppInstanceRepository
            .findByIsActiveTrueAndInstanceIdIsNotNullAndAccessTokenIsNotNull();

        if (activeInstances.isEmpty()) {
            log.info("📭 No active instances found for periodic notification");
            return;
        }

        for (WhatsAppInstance instance : activeInstances) {
            try {
                Map<String, Object> currentStatus = getInstanceStatus(instance);
                Boolean connected = (Boolean) currentStatus.get("connected");
                String status = connected != null && connected ? "CONNECTED" : "DISCONNECTED";
                
                notifyStatusChange(instance, status, currentStatus);
            } catch (Exception e) {
                log.error("Error checking status for instance {}: {}", 
                    instance.getInstanceId(), e.getMessage());
            }
        }
        
    }

    private void notifyStatusChange(WhatsAppInstance instance, String status, Map<String, Object> statusData) {
        try {
            Map<String, Object> notification = Map.of(
                "type", "INSTANCE_STATUS_CHANGE",
                "instanceId", instance.getInstanceId(),
                "status", status,
                "phoneNumber", instance.getPhoneNumber(),
                "displayName", instance.getDisplayName() != null ? instance.getDisplayName() : "",
                "statusData", statusData,
                "timestamp", LocalDateTime.now().toString()
            );

            // Send to company-specific channel
            String channel = "company-" + instance.getCompany().getId();
            webSocketNotificationService.sendToChannel(channel, notification);
        } catch (Exception e) {
            log.error("❌ Error sending WebSocket notification for instance status change: {}", e.getMessage(), e);
        }
    }
    
    // Backward compatibility method
    private void notifyStatusChange(WhatsAppInstance instance, String status) {
        Map<String, Object> statusData = getInstanceStatus(instance);
        notifyStatusChange(instance, status, statusData);
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