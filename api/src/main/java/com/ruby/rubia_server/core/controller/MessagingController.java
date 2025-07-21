package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.service.MessagingService;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            log.info("Received Z-API webhook: {}", payload);
            
            String fromMe = (String) payload.get("fromMe");
            if ("true".equals(fromMe)) {
                return ResponseEntity.ok("OK");
            }

            if (!messagingService.validateWebhook(payload, authorization)) {
                return ResponseEntity.status(401).body("Unauthorized");
            }
            
            IncomingMessage message = messagingService.parseIncomingMessage(payload);
            messagingService.processIncomingMessage(message);
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing Z-API webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
}