package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.ZApiStatus;
import com.ruby.rubia_server.core.entity.QrCodeResult;
import com.ruby.rubia_server.core.entity.PhoneCodeResult;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class ZApiActivationService {
    
    @Value("${zapi.clientToken}")
    private String clientToken;

    private final RestTemplate restTemplate;
    private final WhatsAppInstanceService whatsAppInstanceService;
    private final CompanyContextUtil companyContextUtil;

    public ZApiActivationService(WhatsAppInstanceService whatsAppInstanceService, 
                                CompanyContextUtil companyContextUtil) {
        this.restTemplate = new RestTemplate();
        this.whatsAppInstanceService = whatsAppInstanceService;
        this.companyContextUtil = companyContextUtil;
    }

    /**
     * Gets the active WhatsApp instance for the current company context.
     * During activation, looks for instances in configuration state.
     */
    private WhatsAppInstance getActiveInstance() {
        try {
            // First try to find a connected instance
            var connectedInstance = whatsAppInstanceService.findActiveConnectedInstance(companyContextUtil.getCurrentCompany());
            if (connectedInstance.isPresent()) {
                return connectedInstance.get();
            }
            
            // If no connected instance, look for instances in configuration/activation state
            var instances = whatsAppInstanceService.findByCompany(companyContextUtil.getCurrentCompany());
            return instances.stream()
                .filter(instance -> instance.getInstanceId() != null && instance.getAccessToken() != null)
                .filter(instance -> instance.getStatus().name().matches("CONFIGURING|AWAITING_QR_SCAN|CONNECTING"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No configured WhatsApp instance found for activation"));
        } catch (Exception e) {
            log.error("Error getting active instance: {}", e.getMessage());
            throw new IllegalStateException("Cannot determine active WhatsApp instance", e);
        }
    }

    /**
     * Builds the Z-API URL for the given endpoint using active instance data
     */
    private String buildZApiUrl(String endpoint) {
        WhatsAppInstance instance = getActiveInstance();
        if (instance.getInstanceId() == null || instance.getAccessToken() == null) {
            throw new IllegalStateException("WhatsApp instance is not properly configured (missing instanceId or accessToken)");
        }
        return String.format("https://api.z-api.io/instances/%s/token/%s/%s", 
                           instance.getInstanceId(), instance.getAccessToken(), endpoint);
    }

    public ZApiStatus getInstanceStatus() {
        try {
            String url = buildZApiUrl("status");
            
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
            String url = buildZApiUrl("qr-code");
            
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
            String url = buildZApiUrl("qr-code/image");
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
            String url = buildZApiUrl("phone-code/" + phoneNumber);
            
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
            String url = buildZApiUrl("restart");
            
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
            String url = buildZApiUrl("disconnect");
            
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