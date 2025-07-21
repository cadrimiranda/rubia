package com.ruby.rubia_server.core.adapter.impl;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "zapi")
@Slf4j
public class ZApiAdapter implements MessagingAdapter {

    @Value("${zapi.instance.url}")
    private String instanceUrl;

    @Value("${zapi.token}")
    private String token;

    @Value("${zapi.webhook.token:}")
    private String webhookToken;

    private final RestTemplate restTemplate;

    public ZApiAdapter() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public MessageResult sendMessage(String to, String message) {
        try {
            log.info("Sending Z-API message to: {}", to);

            String url = instanceUrl + "/send-text";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", formatPhoneNumber(to));
            requestBody.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("messageId");
                
                log.info("Z-API message sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send message via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending message via Z-API: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    @Override
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        try {
            log.info("Sending Z-API media message to: {}", to);

            String url = instanceUrl + "/send-file-url";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", formatPhoneNumber(to));
            requestBody.put("url", mediaUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("messageId");
                
                log.info("Z-API media message sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send media message via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending media message via Z-API: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    @Override
    public IncomingMessage parseIncomingMessage(Object webhookPayload) {
        try {
            log.info("Parsing Z-API incoming message");

            if (!(webhookPayload instanceof Map)) {
                throw new IllegalArgumentException("Invalid payload format");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) webhookPayload;

            String messageId = (String) payload.get("messageId");
            String phone = (String) payload.get("phone");
            String fromMe = (String) payload.get("fromMe");
            
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            String messageBody = null;
            String mediaUrl = null;
            String mediaType = null;

            if (message != null) {
                messageBody = (String) message.get("conversation");
                
                Map<String, Object> imageMessage = (Map<String, Object>) message.get("imageMessage");
                Map<String, Object> videoMessage = (Map<String, Object>) message.get("videoMessage");
                Map<String, Object> documentMessage = (Map<String, Object>) message.get("documentMessage");

                if (imageMessage != null) {
                    mediaUrl = (String) imageMessage.get("url");
                    mediaType = "image";
                    messageBody = (String) imageMessage.get("caption");
                } else if (videoMessage != null) {
                    mediaUrl = (String) videoMessage.get("url");
                    mediaType = "video";
                    messageBody = (String) videoMessage.get("caption");
                } else if (documentMessage != null) {
                    mediaUrl = (String) documentMessage.get("url");
                    mediaType = "document";
                    messageBody = (String) documentMessage.get("caption");
                }
            }

            Long timestamp = (Long) payload.get("timestamp");
            LocalDateTime messageTime = LocalDateTime.now();

            return IncomingMessage.builder()
                .messageId(messageId)
                .from(phone)
                .to(null)
                .body(messageBody)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .timestamp(messageTime)
                .provider("z-api")
                .rawPayload(payload)
                .build();

        } catch (Exception e) {
            log.error("Error parsing Z-API incoming message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse Z-API incoming message", e);
        }
    }

    @Override
    public boolean validateWebhook(Object payload, String signature) {
        if (webhookToken != null && !webhookToken.isEmpty()) {
            return true;
        }
        return true;
    }

    @Override
    public String getProviderName() {
        return "z-api";
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }

        String digitsOnly = phoneNumber.replaceAll("\\D", "");
        
        if (digitsOnly.startsWith("55")) {
            return digitsOnly;
        } else if (digitsOnly.startsWith("+55")) {
            return digitsOnly.substring(1);
        } else {
            return "55" + digitsOnly;
        }
    }
}