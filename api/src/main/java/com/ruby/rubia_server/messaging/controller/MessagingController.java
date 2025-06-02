package com.ruby.rubia_server.messaging.controller;

import com.ruby.rubia_server.messaging.service.MessagingService;
import com.ruby.rubia_server.messaging.model.MessageResult;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messaging")
public class MessagingController {
    
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
            
            processIncomingMessage(message);
            
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
    
    private void processIncomingMessage(IncomingMessage message) {
        String body = message.getBody().toLowerCase();
        
        if (body.contains("oi") || body.contains("olá")) {
            messagingService.sendMessage(
                message.getFrom(), 
                "Olá! Como posso ajudar você?"
            );
        }
    }
}