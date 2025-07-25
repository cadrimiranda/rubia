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
    
    @PostMapping("/webhook/zapi")
    public ResponseEntity<String> handleZApiWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        try {
            log.info("=== Z-API WEBHOOK RECEIVED ===");
            log.info("Payload: {}", payload);
            log.info("Authorization header: {}", authorization);
            
            Object fromMeObj = payload.get("fromMe");
            boolean isFromMe = false;
            if (fromMeObj instanceof Boolean) {
                isFromMe = (Boolean) fromMeObj;
            } else if (fromMeObj instanceof String) {
                isFromMe = "true".equals(fromMeObj);
            }
            log.info("Message fromMe: {}", isFromMe);
            
            if (isFromMe) {
                log.info("Ignoring message from me");
                return ResponseEntity.ok("OK");
            }

            log.info("Validating webhook...");
            boolean isValid = messagingService.validateWebhook(payload, authorization);
            log.info("Webhook validation result: {}", isValid);
            
            if (!isValid) {
                log.warn("Webhook validation failed");
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            log.info("Parsing incoming message...");
            IncomingMessage message = messagingService.parseIncomingMessage(payload);
            log.info("Parsed message: from={}, body={}, messageId={}", 
                message.getFrom(), message.getBody(), message.getMessageId());
            
            log.info("Processing incoming message...");
            messagingService.processIncomingMessage(message);
            log.info("Message processed successfully");
            
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