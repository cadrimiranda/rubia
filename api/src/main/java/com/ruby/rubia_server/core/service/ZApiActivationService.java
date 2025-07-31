package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.ZApiStatus;
import com.ruby.rubia_server.core.entity.QrCodeResult;
import com.ruby.rubia_server.core.entity.PhoneCodeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class ZApiActivationService {

    @Value("${zapi.instance.url}")
    private String instanceUrl;

    @Value("${zapi.token}")
    private String token;
    
    @Value("${zapi.clientToken}")
    private String clientToken;

    private final RestTemplate restTemplate;

    public ZApiActivationService() {
        this.restTemplate = new RestTemplate();
    }

    public ZApiStatus getInstanceStatus() {
        try {
            String url = instanceUrl + "/token/" + token + "/status";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseStatusResponse(response.getBody());
            } else {
                return ZApiStatus.error("Failed to get status");
            }

        } catch (Exception e) {
            log.error("Error getting Z-API status: {}", e.getMessage(), e);
            return ZApiStatus.error("Error: " + e.getMessage());
        }
    }

    public QrCodeResult getQrCodeBytes() {
        try {
            String url = instanceUrl + "/token/" + token + "/qr-code";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, request, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return QrCodeResult.success(response.getBody(), "bytes");
            } else {
                return QrCodeResult.error("Failed to get QR code bytes");
            }

        } catch (Exception e) {
            log.error("Error getting QR code bytes: {}", e.getMessage(), e);
            return QrCodeResult.error("Error: " + e.getMessage());
        }
    }

    public QrCodeResult getQrCodeImage() {
        try {
            String url = instanceUrl + "/token/" + token + "/qr-code/image";
            log.info("Getting QR code from URL: {}", url);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            log.info("Z-API QR Code response: status={}, body={}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Z-API returns the QR code in the "value" field, not "image"
                String fullValue = (String) response.getBody().get("value");
                log.info("Full value received: {}", fullValue != null ? fullValue.substring(0, Math.min(100, fullValue.length())) + "..." : "null");
                
                // Extract base64 part from data URL format (data:image/png;base64,XXXX)
                String base64Image = null;
                if (fullValue != null && fullValue.startsWith("data:image/png;base64,")) {
                    base64Image = fullValue.substring("data:image/png;base64,".length());
                }
                
                log.info("Base64 image extracted: {}", base64Image != null ? "present (" + base64Image.length() + " chars)" : "null");
                return QrCodeResult.success(base64Image, "base64");
            } else {
                log.warn("Failed to get QR code: status={}, body={}", response.getStatusCode(), response.getBody());
                return QrCodeResult.error("Failed to get QR code image");
            }

        } catch (Exception e) {
            log.error("Error getting QR code image: {}", e.getMessage(), e);
            return QrCodeResult.error("Error: " + e.getMessage());
        }
    }

    public PhoneCodeResult getPhoneCode(String phoneNumber) {
        try {
            String url = instanceUrl + "/token/" + token + "/phone-code/" + phoneNumber;
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String code = (String) response.getBody().get("code");
                return PhoneCodeResult.success(code, phoneNumber);
            } else {
                return PhoneCodeResult.error("Failed to get phone code");
            }

        } catch (Exception e) {
            log.error("Error getting phone code: {}", e.getMessage(), e);
            return PhoneCodeResult.error("Error: " + e.getMessage());
        }
    }

    public boolean restartInstance() {
        try {
            String url = instanceUrl + "/token/" + token + "/restart";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error restarting instance: {}", e.getMessage(), e);
            return false;
        }
    }

    public boolean disconnectInstance() {
        try {
            String url = instanceUrl + "/token/" + token + "/disconnect";
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.error("Error disconnecting instance: {}", e.getMessage(), e);
            return false;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("client-token", clientToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ZApiStatus parseStatusResponse(Map<String, Object> response) {
        Object connectedObj = response.get("connected");
        Object smartphoneConnectedObj = response.get("smartphoneConnected");
        Object sessionObj = response.get("session");
        
        // Handle both boolean and string responses
        boolean connected = parseBoolean(connectedObj);
        boolean smartphoneConnected = parseBoolean(smartphoneConnectedObj);
        String session = parseString(sessionObj);
        
        return ZApiStatus.builder()
            .connected(connected)
            .session(session)
            .smartphoneConnected(smartphoneConnected)
            .needsQrCode(!connected)
            .rawResponse(response)
            .build();
    }
    
    private boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return "true".equalsIgnoreCase((String) value);
        }
        return false;
    }
    
    private String parseString(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }
}