package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.service.MessagingService;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/messaging")
public class MessagingController {
    
    private static final Logger log = LoggerFactory.getLogger(MessagingController.class);
    
    @Autowired
    private MessagingService messagingService;
    
    @PostMapping("/send")
    public ResponseEntity<MessageResult> sendMessage(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        String message = request.get("message");
        
        MessageResult result = messagingService.sendMessage(to, message);
        
        return result.isSuccess() ? 
            ResponseEntity.ok(result) : 
            ResponseEntity.badRequest().body(result);
    }
    
    @PostMapping("/send-media")
    public ResponseEntity<MessageResult> sendMediaMessage(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        String mediaUrl = request.get("mediaUrl");
        String caption = request.get("caption");
        
        MessageResult result = messagingService.sendMediaMessage(to, mediaUrl, caption);
        
        return result.isSuccess() ? 
            ResponseEntity.ok(result) : 
            ResponseEntity.badRequest().body(result);
    }
    
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody Map<String, String> payload,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String signature) {
        
        try {
            if (!messagingService.validateWebhook(payload, signature)) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            IncomingMessage message = messagingService.parseIncomingMessage(payload);
            
            messagingService.processIncomingMessage(message);
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/switch-provider")
    public ResponseEntity<Map<String, Object>> switchProvider(@RequestBody Map<String, String> request) {
        try {
            String provider = request.get("provider");
            messagingService.switchAdapter(provider);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "currentProvider", messagingService.getCurrentProvider(),
                "availableProviders", messagingService.getAvailableProviders()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
            "currentProvider", messagingService.getCurrentProvider(),
            "availableProviders", messagingService.getAvailableProviders()
        );
    }
    
    @GetMapping("/webhook/zapi/test")
    public ResponseEntity<String> testZApiWebhook() {
        log.info("🧪 Z-API webhook test endpoint accessed");
        return ResponseEntity.ok("Z-API webhook endpoint is working! Timestamp: " + System.currentTimeMillis());
    }
    
    @PostMapping("/webhook/zapi")
    public ResponseEntity<String> handleZApiWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor) {
        
        try {
            log.info("=== Z-API WEBHOOK DEBUG - RECEIVED ===");
            log.info("🔍 Full payload received: {}", payload);
            log.info("🔑 Authorization header: '{}'", authorization);
            log.info("🤖 User-Agent: '{}'", userAgent);
            log.info("🌐 X-Forwarded-For: '{}'", forwardedFor);
            log.info("📋 Payload keys: {}", payload.keySet());
            log.info("📏 Payload size: {} entries", payload.size());
            
            // Log each field individually for better debugging
            payload.forEach((key, value) -> {
                log.info("🔸 Field '{}' = '{}' (type: {})", 
                    key, value, value != null ? value.getClass().getSimpleName() : "null");
            });
            
            Object fromMeObj = payload.get("fromMe");
            boolean isFromMe = false;
            if (fromMeObj instanceof Boolean) {
                isFromMe = (Boolean) fromMeObj;
            } else if (fromMeObj instanceof String) {
                isFromMe = "true".equals(fromMeObj);
            }
            log.info("📤 Message fromMe: {} (original: {})", isFromMe, fromMeObj);
            
            // Extract key fields for debugging
            String phone = (String) payload.get("phone");
            String connectedPhone = (String) payload.get("connectedPhone");
            String messageId = (String) payload.get("messageId");
            String type = (String) payload.get("type");
            String status = (String) payload.get("status");
            Object textObj = payload.get("text");
            
            log.info("📱 Phone: '{}'", phone);
            log.info("🔗 Connected Phone: '{}'", connectedPhone);
            log.info("🆔 Message ID: '{}'", messageId);
            log.info("📝 Type: '{}'", type);
            log.info("🔄 Status: '{}'", status);
            log.info("💬 Text object: {} (type: {})", textObj, textObj != null ? textObj.getClass().getSimpleName() : "null");
            
            if (textObj instanceof Map) {
                Map<String, Object> textMap = (Map<String, Object>) textObj;
                log.info("💬 Text map contents: {}", textMap);
                textMap.forEach((key, value) -> {
                    log.info("💬   Text field '{}' = '{}'", key, value);
                });
            }
            
            if (isFromMe) {
                log.info("⏭️ IMPORTANTE: Mensagem ignorada porque fromMe=true");
                log.info("⏭️ Isso significa que a mensagem foi enviada PELA sua instância, não recebida");
                log.info("⏭️ Para testar, envie mensagem DE OUTRO WHATSAPP para este número");
                log.info("⏭️ DEBUG MODE COMPLETE");
                return ResponseEntity.ok("OK - DEBUG: Message from me ignored");
            }

            log.info("✅ Webhook validation...");
            boolean isValid = messagingService.validateWebhook(payload, authorization);
            log.info("✅ Webhook validation result: {}", isValid);
            
            if (!isValid) {
                log.warn("❌ Webhook validation failed - DEBUG MODE COMPLETE");
                return ResponseEntity.status(401).body("Unauthorized - DEBUG: Validation failed");
            }
            
            log.info("🔄 Parsing incoming message...");
            IncomingMessage message = messagingService.parseIncomingMessage(payload);
            
            if (message == null) {
                log.info("📋 Webhook ignored (likely status update)");
                return ResponseEntity.ok("OK - Ignored");
            }
            
            log.info("📨 Parsed message: from='{}', body='{}'", message.getFrom(), message.getBody());
            
            log.info("🚀 Processing incoming message...");
            messagingService.processIncomingMessage(message);
            
            log.info("✅ Z-API webhook processed successfully");
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing Z-API webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/send-image")
    public ResponseEntity<MessageResult> sendImage(
            @RequestParam String to,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String caption) {
        
        MessageResult result = messagingService.sendImageByUrl(to, imageUrl, caption);
        return result.isSuccess() ? 
            ResponseEntity.ok(result) : 
            ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/send-document")
    public ResponseEntity<MessageResult> sendDocument(
            @RequestParam String to,
            @RequestParam String documentUrl,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) String fileName) {
        
        MessageResult result = messagingService.sendDocumentByUrl(to, documentUrl, caption, fileName);
        return result.isSuccess() ? 
            ResponseEntity.ok(result) : 
            ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/upload-and-send")
    public ResponseEntity<MessageResult> uploadAndSend(
            @RequestParam String to,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String caption) {
        
        try {
            String fileUrl = messagingService.uploadFile(file);
            MessageResult result = messagingService.sendMediaByUrl(to, fileUrl, caption);
            return result.isSuccess() ? 
                ResponseEntity.ok(result) : 
                ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            MessageResult error = MessageResult.error("Upload failed: " + e.getMessage(), "z-api");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/send-file-base64")
    public ResponseEntity<MessageResult> sendFileBase64(@RequestBody Map<String, String> request) {
        String to = request.get("to");
        String base64Data = request.get("base64");
        String fileName = request.get("fileName");
        String caption = request.get("caption");
        
        MessageResult result = messagingService.sendFileBase64(to, base64Data, fileName, caption);
        return result.isSuccess() ? 
            ResponseEntity.ok(result) : 
            ResponseEntity.badRequest().body(result);
    }
    
}