package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.service.ZApiActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/zapi/activation")
@RequiredArgsConstructor
@Slf4j
public class ZApiActivationController {

    private final ZApiActivationService activationService;

    @GetMapping("/status")
    public ResponseEntity<ZApiStatus> getStatus() {
        ZApiStatus status = activationService.getInstanceStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/qr-code/bytes")
    public ResponseEntity<byte[]> getQrCodeBytes() {
        QrCodeResult result = activationService.getQrCodeBytes();
        
        if (result.isSuccess()) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", "qrcode.png");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body((byte[]) result.getData());
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/qr-code/image")
    public ResponseEntity<QrCodeResult> getQrCodeImage() {
        QrCodeResult result = activationService.getQrCodeImage();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/phone-code/{phone}")
    public ResponseEntity<PhoneCodeResult> getPhoneCode(@PathVariable String phone) {
        PhoneCodeResult result = activationService.getPhoneCode(phone);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/restart")
    public ResponseEntity<Map<String, Object>> restartInstance() {
        boolean success = activationService.restartInstance();
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Instance restarted successfully" : "Failed to restart instance"
        ));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Map<String, Object>> disconnectInstance() {
        boolean success = activationService.disconnectInstance();
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Instance disconnected successfully" : "Failed to disconnect instance"
        ));
    }

    @PostMapping("/webhook/connected")
    public ResponseEntity<String> handleConnectedWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Z-API instance connected: {}", payload);
            
            String instanceId = (String) payload.get("instanceId");
            Boolean connected = (Boolean) payload.get("connected");
            
            if (Boolean.TRUE.equals(connected)) {
                log.info("Instance {} successfully connected to WhatsApp", instanceId);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing connected webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/webhook/disconnected")
    public ResponseEntity<String> handleDisconnectedWebhook(@RequestBody Map<String, Object> payload) {
        try {
            log.info("Z-API instance disconnected: {}", payload);
            
            String instanceId = (String) payload.get("instanceId");
            String error = (String) payload.get("error");
            
            log.warn("Instance {} disconnected. Error: {}", instanceId, error);
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing disconnected webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}