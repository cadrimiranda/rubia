package com.ruby.rubia_server.core.adapter.impl;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.service.PhoneService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

    @Value("${zapi.clientToken}")
    private String clientToken;

    @Value("${zapi.webhook.token:}")
    private String webhookToken;

    private final RestTemplate restTemplate;
    private final PhoneService phoneService;

    public ZApiAdapter(PhoneService phoneService) {
        this.restTemplate = new RestTemplate();
        this.phoneService = phoneService;
    }

    @Override
    public MessageResult sendMessage(String to, String message) {
        try {
            log.info("Sending Z-API message to: {} with message: {}", to, message.substring(0, Math.min(50, message.length())));

            String url = instanceUrl + "/token/" + token + "/send-text";
            log.info("Z-API URL: {}", url);
            
            String formattedPhone = phoneService.formatForZApi(to);
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", formattedPhone);
            requestBody.put("message", message);
            
            log.info("Request body: phone={}, message length={}", formattedPhone, message.length());

            HttpHeaders headers = createHeaders();
            log.info("Headers: {}", headers.toSingleValueMap());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, request, Map.class);

            log.info("Z-API Response: status={}, body={}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String messageId = (String) responseBody.get("messageId");
                
                log.info("Z-API message sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send message via Z-API - Status: " + response.getStatusCode();
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

            String url = instanceUrl + "/token/" + token + "/send-file-url";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phoneService.formatForZApi(to));
            requestBody.put("url", mediaUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }

            HttpHeaders headers = createHeaders();

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
            if (!(webhookPayload instanceof Map)) {
                throw new IllegalArgumentException("Invalid payload format");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) webhookPayload;

            log.info("Z-API webhook payload: {}", payload);

            String type = (String) payload.get("type");
            if ("DeliveryCallback".equals(type)) {
                log.info("Ignoring DeliveryCallback webhook - not a message");
                return null;
            }

            String messageId = (String) payload.get("messageId");
            String phone = (String) payload.get("phone");
            String connectedPhone = (String) payload.get("connectedPhone");
            
            log.info("Extracted from payload - messageId: {}, phone: {}, connectedPhone: {}", 
                messageId, phone, connectedPhone);
            
            if (phone != null && phone.contains("@newsletter")) {
                log.info("Ignoring newsletter message from: {}", phone);
                return null;
            }
            
            // Handle fromMe as either boolean or string for compatibility
            Object fromMeObj = payload.get("fromMe");
            boolean isFromMe = false;
            if (fromMeObj instanceof Boolean) {
                isFromMe = (Boolean) fromMeObj;
            } else if (fromMeObj instanceof String) {
                isFromMe = "true".equals(fromMeObj);
            }
            
            if (isFromMe) {
                return null;
            }

            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            String messageBody = null;
            String mediaUrl = null;
            String mediaType = null;
            String fileName = null;
            String mimeType = null;

            if (message != null) {
                // Try official Z-API format first (text.message)
                Map<String, Object> textMessage = (Map<String, Object>) message.get("text");
                if (textMessage != null) {
                    messageBody = (String) textMessage.get("message");
                } else {
                    // Fallback to legacy format
                    messageBody = (String) message.get("conversation");
                }
                
                Map<String, Object> imageMessage = (Map<String, Object>) message.get("imageMessage");
                if (imageMessage != null) {
                    mediaUrl = (String) imageMessage.get("url");
                    mediaType = "image";
                    mimeType = (String) imageMessage.get("mimeType");
                    messageBody = (String) imageMessage.get("caption");
                }
                
                Map<String, Object> videoMessage = (Map<String, Object>) message.get("videoMessage");
                if (videoMessage != null) {
                    mediaUrl = (String) videoMessage.get("url");
                    mediaType = "video";
                    mimeType = (String) videoMessage.get("mimeType");
                    messageBody = (String) videoMessage.get("caption");
                }
                
                Map<String, Object> documentMessage = (Map<String, Object>) message.get("documentMessage");
                if (documentMessage != null) {
                    mediaUrl = (String) documentMessage.get("url");
                    mediaType = "document";
                    mimeType = (String) documentMessage.get("mimeType");
                    fileName = (String) documentMessage.get("fileName");
                    messageBody = (String) documentMessage.get("caption");
                }
                
                Map<String, Object> audioMessage = (Map<String, Object>) message.get("audioMessage");
                if (audioMessage != null) {
                    mediaUrl = (String) audioMessage.get("url");
                    mediaType = "audio";
                    mimeType = (String) audioMessage.get("mimeType");
                }
            }

            Object timestampObj = payload.get("momment");
            LocalDateTime messageTime;
            if (timestampObj instanceof Long) {
                messageTime = LocalDateTime.ofEpochSecond((Long) timestampObj, 0, ZoneOffset.UTC);
            } else if (timestampObj instanceof Integer) {
                messageTime = LocalDateTime.ofEpochSecond(((Integer) timestampObj).longValue(), 0, ZoneOffset.UTC);
            } else {
                messageTime = LocalDateTime.now();
            }

            return IncomingMessage.builder()
                .messageId(messageId)
                .from(phone)
                .to(null)
                .connectedPhone(connectedPhone)
                .body(messageBody)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .fileName(fileName)
                .mimeType(mimeType)
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


    public MessageResult sendImageByUrl(String to, String imageUrl, String caption) {
        return sendMediaByUrl(to, imageUrl, caption, "image");
    }

    public MessageResult sendDocumentByUrl(String to, String documentUrl, String caption, String fileName) {
        try {
            log.info("Sending Z-API document to: {}", to);

            String url = instanceUrl + "/token/" + token + "/send-file-url";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phoneService.formatForZApi(to));
            requestBody.put("url", documentUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }
            if (fileName != null && !fileName.trim().isEmpty()) {
                requestBody.put("fileName", fileName);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = (String) response.getBody().get("messageId");
                log.info("Z-API document sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send document via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending document: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    public MessageResult sendFileBase64(String to, String base64Data, String fileName, String caption) {
        try {
            log.info("Sending Z-API base64 file to: {}", to);

            String url = instanceUrl + "/token/" + token + "/send-file-base64";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phoneService.formatForZApi(to));
            requestBody.put("base64", base64Data);
            requestBody.put("fileName", fileName);
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = (String) response.getBody().get("messageId");
                log.info("Z-API base64 file sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send file via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending file: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            log.info("Uploading file to Z-API: {}", file.getOriginalFilename());

            String url = instanceUrl + "/token/" + token + "/upload-file";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("client-token", clientToken);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String fileUrl = (String) response.getBody().get("url");
                log.info("File uploaded successfully to Z-API: {}", fileUrl);
                return fileUrl;
            } else {
                throw new RuntimeException("Failed to upload file to Z-API");
            }

        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Error uploading file: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client-token", clientToken);
        return headers;
    }

    private MessageResult sendMediaByUrl(String to, String mediaUrl, String caption, String mediaType) {
        try {
            log.info("Sending Z-API {} to: {}", mediaType, to);

            String url = instanceUrl + "/token/" + token + "/send-file-url";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phoneService.formatForZApi(to));
            requestBody.put("url", mediaUrl);
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = (String) response.getBody().get("messageId");
                log.info("Z-API {} sent successfully. Message ID: {}", mediaType, messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send " + mediaType + " via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending " + mediaType + ": " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }
}