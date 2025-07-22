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
            
            HttpHeaders headers = createHeaders();
            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String base64Image = (String) response.getBody().get("image");
                return QrCodeResult.success(base64Image, "base64");
            } else {
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
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ZApiStatus parseStatusResponse(Map<String, Object> response) {
        String connected = (String) response.get("connected");
        String session = (String) response.get("session");
        String smartphoneConnected = (String) response.get("smartphoneConnected");
        
        return ZApiStatus.builder()
            .connected("true".equals(connected))
            .session(session)
            .smartphoneConnected("true".equals(smartphoneConnected))
            .needsQrCode(!(("true".equals(connected))))
            .rawResponse(response)
            .build();
    }
}