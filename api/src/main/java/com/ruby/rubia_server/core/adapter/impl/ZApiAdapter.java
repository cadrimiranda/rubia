package com.ruby.rubia_server.core.adapter.impl;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.service.PhoneService;
import com.ruby.rubia_server.core.service.WhatsAppInstanceService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import com.ruby.rubia_server.core.validation.WhatsAppInstanceValidator;
import com.ruby.rubia_server.core.factory.ZApiUrlFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.HashMap;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "zapi")
@Slf4j
public class ZApiAdapter implements MessagingAdapter {
  
    @Value("${zapi.clientToken}")
    private String clientToken;

    @Value("${zapi.webhook.token:}")
    private String webhookToken;

    private final RestTemplate restTemplate;
    private final PhoneService phoneService;
    private final WhatsAppInstanceService whatsAppInstanceService;
    private final CompanyContextUtil companyContextUtil;
    private final WhatsAppInstanceValidator instanceValidator;
    private final ZApiUrlFactory urlFactory;

    public ZApiAdapter(RestTemplate restTemplate,
                      PhoneService phoneService, 
                      WhatsAppInstanceService whatsAppInstanceService,
                      CompanyContextUtil companyContextUtil,
                      WhatsAppInstanceValidator instanceValidator,
                      ZApiUrlFactory urlFactory) {
        this.restTemplate = restTemplate;
        this.phoneService = phoneService;
        this.whatsAppInstanceService = whatsAppInstanceService;
        this.companyContextUtil = companyContextUtil;
        this.instanceValidator = instanceValidator;
        this.urlFactory = urlFactory;
    }

    /**
     * Gets the active WhatsApp instance for the current company context
     * and validates it's ready for messaging operations
     */
    private WhatsAppInstance getActiveInstance() {
        try {
            WhatsAppInstance instance = whatsAppInstanceService.findActiveConnectedInstance(companyContextUtil.getCurrentCompany())
                .orElseThrow(() -> new IllegalStateException("No connected WhatsApp instance found for company"));
            
            // Validate instance is ready for messaging
            instanceValidator.validateInstanceReadyForMessaging(instance);
            
            return instance;
        } catch (IllegalStateException e) {
            // For company context resolution errors, wrap with generic message
            if (e.getMessage() != null && e.getMessage().contains("company context")) {
                log.error("Error getting active instance: {}", e.getMessage());
                throw new IllegalStateException("Cannot determine active WhatsApp instance", e);
            }
            // For other IllegalStateExceptions (instance not found, validation failures), re-throw as-is
            throw e;
        } catch (Exception e) {
            log.error("Error getting active instance: {}", e.getMessage());
            throw new IllegalStateException("Cannot determine active WhatsApp instance", e);
        }
    }

    /**
     * Gets the active WhatsApp instance for the specified company
     * and validates it's ready for messaging operations
     */
    private WhatsAppInstance getActiveInstanceForCompany(com.ruby.rubia_server.core.entity.Company company) {
        try {
            WhatsAppInstance instance = whatsAppInstanceService.findActiveConnectedInstance(company)
                .orElseThrow(() -> new IllegalStateException("No connected WhatsApp instance found for company " + company.getId()));
            
            // Validate instance is ready for messaging
            instanceValidator.validateInstanceReadyForMessaging(instance);
            
            return instance;
        } catch (Exception e) {
            log.error("Error getting active instance for company {}: {}", company.getId(), e.getMessage());
            throw new IllegalStateException("Cannot determine active WhatsApp instance for company " + company.getId(), e);
        }
    }

    /**
     * Builds the Z-API URL for the given endpoint using active instance data
     */
    private String buildZApiUrl(String endpoint) {
        WhatsAppInstance instance = getActiveInstance();
        return urlFactory.buildUrl(instance, endpoint);
    }

    /**
     * Builds the Z-API URL for the given endpoint using company-specific instance data
     */
    private String buildZApiUrlForCompany(String endpoint, com.ruby.rubia_server.core.entity.Company company) {
        WhatsAppInstance instance = getActiveInstanceForCompany(company);
        return urlFactory.buildUrl(instance, endpoint);
    }

    @Override
    public MessageResult sendMessage(String to, String message) {
        try {
            log.info("Sending Z-API message to: {} with message: {}", to, message.substring(0, Math.min(50, message.length())));
            
            // Validate input parameters
            instanceValidator.validatePhoneNumber(to);
            instanceValidator.validateMessageContent(message);

            String url = buildZApiUrl("send-text");
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

        } catch (IllegalStateException e) {
            // Re-throw IllegalStateException to allow tests to verify proper error handling
            throw e;
        } catch (Exception e) {
            String error = "Error sending message via Z-API: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    /**
     * Sends a message using the specified company context (for background tasks)
     */
    public MessageResult sendMessage(String to, String message, com.ruby.rubia_server.core.entity.Company company) {
        try {
            log.info("Sending Z-API message to: {} for company: {} with message: {}", to, company.getId(), message.substring(0, Math.min(50, message.length())));
            
            // Validate input parameters
            instanceValidator.validatePhoneNumber(to);
            instanceValidator.validateMessageContent(message);

            String url = buildZApiUrlForCompany("send-text", company);
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
                
                log.info("Z-API message sent successfully for company {}. Message ID: {}", company.getId(), messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send message via Z-API for company " + company.getId() + " - Status: " + response.getStatusCode();
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending message via Z-API for company " + company.getId() + ": " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    @Override
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        try {
            log.info("Sending Z-API media message to: {}", to);
          
            // Detect media type from base64 prefix or content
            String endpoint;
            String mediaParam;
            
            if (mediaUrl.contains("audio/") || mediaUrl.contains("wav") || mediaUrl.contains("mp3") || mediaUrl.contains("ogg")) {
                endpoint = "/send-audio";
                mediaParam = "audio";
            } else if (mediaUrl.contains("image/")) {
                endpoint = "/send-image";
                mediaParam = "image";
            } else if (mediaUrl.contains("video/")) {
                endpoint = "/send-video";
                mediaParam = "video";
            } else {
                // Default to document for other file types
                endpoint = "/send-document";
                mediaParam = "document";
            }

            String url = buildZApiUrl(endpoint);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phoneService.formatForZApi(to));
            requestBody.put(mediaParam, mediaUrl);
            
            if (caption != null && !caption.trim().isEmpty()) {
                requestBody.put("caption", caption);
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

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

            Boolean isGroup = (Boolean) payload.get("isGroup");
            if (Boolean.TRUE.equals(isGroup)) {
                log.info("Ignoring group message - not processing group chats");
                return null;
            }

            String messageId = (String) payload.get("messageId");
            String phone = (String) payload.get("phone");
            String connectedPhone = (String) payload.get("connectedPhone");
            String chatLid = (String) payload.get("chatLid");
            String senderName = (String) payload.get("senderName");
            
            log.debug("Z-API message: {} from {} via {}", messageId, phone, connectedPhone);
            
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

            // Check if message was sent from our API
            Object fromApiObj = payload.get("fromApi");
            boolean isFromApi = false;
            if (fromApiObj instanceof Boolean) {
                isFromApi = (Boolean) fromApiObj;
            } else if (fromApiObj instanceof String) {
                isFromApi = "true".equals(fromApiObj);
            }
            
            log.debug("Message flags - fromMe: {}, fromApi: {}", isFromMe, isFromApi);

            String messageBody = null;
            String mediaUrl = null;
            String mediaType = null;
            String fileName = null;
            String mimeType = null;

            // Extract text from root level (Z-API format)
            Map<String, Object> rootText = (Map<String, Object>) payload.get("text");
            if (rootText != null) {
                messageBody = (String) rootText.get("message");
            }

            // Handle audio messages from root level (new format)
            Map<String, Object> audio = (Map<String, Object>) payload.get("audio");
            if (audio != null) {
                mediaUrl = (String) audio.get("audioUrl");
                mediaType = "audio";
                mimeType = (String) audio.get("mimeType");
                // Audio messages don't have captions, messageBody stays null
            }

            // Handle media messages from message object (legacy format)
            Map<String, Object> message = (Map<String, Object>) payload.get("message");
            if (message != null && mediaUrl == null) { // Only process if no audio found above
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

            
            // Determine correct from/to based on message context
            String fromNumber, toNumber;
            
            if (isFromMe && isFromApi) {
                // Message sent via our API - only process if it has media
                boolean hasMedia = (mediaUrl != null && !mediaUrl.trim().isEmpty());
                if (!hasMedia) {
                    // Ignore text messages sent via API to avoid duplication
                    log.debug("Ignoring API-sent text message (fromMe=true, fromApi=true)");
                    return null;
                }
                // Process media messages sent via API
                fromNumber = phone;
                toNumber = connectedPhone;
                log.debug("Processing API-sent media message: from {} to {}", fromNumber, toNumber);
            } else if (isFromMe && !isFromApi) {
                // Message sent from WhatsApp Web/App
                fromNumber = phone;
                toNumber = connectedPhone;
                log.debug("WhatsApp Web/App message: from {} to {}", fromNumber, toNumber);
            } else {
                // Message from customer to us (fromMe=false)
                fromNumber = phone;
                toNumber = connectedPhone;
                log.debug("Message from customer: from {} to {}", fromNumber, toNumber);
            }
            
            return IncomingMessage.builder()
                .messageId(messageId)
                .from(fromNumber)
                .to(toNumber)
                .connectedPhone(connectedPhone)
                .chatLid(chatLid)
                .body(messageBody)
                .mediaUrl(mediaUrl)
                .mediaType(mediaType)
                .fileName(fileName)
                .mimeType(mimeType)
                .timestamp(messageTime)
                .provider("z-api")
                .senderName(isFromMe ? "Eu" : senderName)
                .isFromMe(isFromMe)
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

            String url = buildZApiUrl("send-file-url");
            
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

    public MessageResult sendAudio(String to, String audioUrl) {
        try {
            log.info("Sending Z-API audio to: {}", to);

            String url = buildZApiUrl("send-audio");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("phone", phoneService.formatForZApi(to));
            requestBody.put("audio", audioUrl);

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String messageId = (String) response.getBody().get("messageId");
                log.info("Z-API audio sent successfully. Message ID: {}", messageId);
                return MessageResult.success(messageId, "sent", "z-api");
            } else {
                String error = "Failed to send audio via Z-API";
                log.error(error);
                return MessageResult.error(error, "z-api");
            }

        } catch (Exception e) {
            String error = "Error sending audio: " + e.getMessage();
            log.error(error, e);
            return MessageResult.error(error, "z-api");
        }
    }

    public MessageResult sendFileBase64(String to, String base64Data, String fileName, String caption) {
        try {
            log.info("Sending Z-API base64 file to: {}", to);

            String url = buildZApiUrl("send-file-base64");
            
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

    public InputStream downloadAudio(String audioUrl) throws Exception {
        try {
            log.info("Downloading audio from URL: {}", audioUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                audioUrl, HttpMethod.GET, entity, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new ByteArrayInputStream(response.getBody());
            } else {
                throw new Exception("Failed to download audio file");
            }
            
        } catch (Exception e) {
            log.error("Error downloading audio: {}", e.getMessage());
            throw new Exception("Failed to download audio file", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        try {
            log.info("Converting file to base64: {}", file.getOriginalFilename());

            // Convert file to base64 instead of uploading to Z-API
            byte[] fileBytes = file.getBytes();
            String base64 = java.util.Base64.getEncoder().encodeToString(fileBytes);
            String base64WithPrefix = "data:" + file.getContentType() + ";base64," + base64;
            
            log.info("File converted to base64 successfully");
            return base64WithPrefix;

        } catch (Exception e) {
            log.error("Error converting file to base64: {}", e.getMessage(), e);
            throw new RuntimeException("Error converting file to base64: " + e.getMessage(), e);
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

            String url = buildZApiUrl("send-text");
            
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