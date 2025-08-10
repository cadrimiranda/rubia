package com.ruby.rubia_server.core.factory;

import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Factory class responsible for building Z-API URLs using the factory pattern.
 * Centralizes URL construction logic and provides consistent URL formatting
 * across the application.
 */
@Component
@Slf4j
public class ZApiUrlFactory {

    private final String baseUrl;

    public ZApiUrlFactory(@Value("${z-api.base-url:https://api.z-api.io}") String baseUrl) {
        this.baseUrl = baseUrl;
        log.info("ZApiUrlFactory initialized with base URL: {}", baseUrl);
    }

    /**
     * Builds a complete Z-API URL for the specified endpoint using instance data.
     * 
     * @param instance the WhatsApp instance containing instanceId and accessToken
     * @param endpoint the API endpoint to call (e.g., "send-text", "status")
     * @return the complete URL string for the Z-API call
     * @throws IllegalArgumentException if instance or endpoint is invalid
     */
    public String buildUrl(WhatsAppInstance instance, String endpoint) {
        validateInput(instance, endpoint);
        
        String url = UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .pathSegment("instances", instance.getInstanceId())
            .pathSegment("token", instance.getAccessToken())
            .pathSegment(endpoint)
            .build()
            .toUriString();
            
        log.debug("Built Z-API URL for endpoint '{}': {}", endpoint, maskSensitiveInfo(url));
        return url;
    }

    /**
     * Builds a Z-API URL with additional path parameters.
     * 
     * @param instance the WhatsApp instance
     * @param endpoint the base endpoint
     * @param pathParams additional path parameters to append
     * @return the complete URL string
     */
    public String buildUrlWithPathParams(WhatsAppInstance instance, String endpoint, String... pathParams) {
        validateInput(instance, endpoint);
        
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .pathSegment("instances", instance.getInstanceId())
            .pathSegment("token", instance.getAccessToken())
            .pathSegment(endpoint);
            
        // Add additional path parameters
        for (String param : pathParams) {
            if (param != null && !param.trim().isEmpty()) {
                builder.pathSegment(param);
            }
        }
        
        String url = builder.build().toUriString();
        log.debug("Built Z-API URL with path params for endpoint '{}': {}", 
                 endpoint, maskSensitiveInfo(url));
        return url;
    }

    /**
     * Builds a Z-API URL with query parameters.
     * 
     * @param instance the WhatsApp instance
     * @param endpoint the API endpoint
     * @param queryParams query parameters as key-value pairs
     * @return the complete URL string with query parameters
     */
    public String buildUrlWithQueryParams(WhatsAppInstance instance, String endpoint, Object... queryParams) {
        validateInput(instance, endpoint);
        
        if (queryParams.length % 2 != 0) {
            throw new IllegalArgumentException("Query parameters must be provided as key-value pairs");
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .pathSegment("instances", instance.getInstanceId())
            .pathSegment("token", instance.getAccessToken())
            .pathSegment(endpoint);
            
        // Add query parameters
        for (int i = 0; i < queryParams.length; i += 2) {
            String key = String.valueOf(queryParams[i]);
            String value = String.valueOf(queryParams[i + 1]);
            builder.queryParam(key, value);
        }
        
        String url = builder.build().toUriString();
        log.debug("Built Z-API URL with query params for endpoint '{}': {}", 
                 endpoint, maskSensitiveInfo(url));
        return url;
    }

    /**
     * Builds a URI object instead of a string for more advanced URL manipulation.
     * 
     * @param instance the WhatsApp instance
     * @param endpoint the API endpoint
     * @return URI object for the Z-API call
     */
    public URI buildUri(WhatsAppInstance instance, String endpoint) {
        validateInput(instance, endpoint);
        
        return UriComponentsBuilder
            .fromHttpUrl(baseUrl)
            .pathSegment("instances", instance.getInstanceId())
            .pathSegment("token", instance.getAccessToken())
            .pathSegment(endpoint)
            .build()
            .toUri();
    }

    /**
     * Builds URLs for common Z-API endpoints with predefined configurations.
     */
    public static class CommonEndpoints {
        
        public static String sendText(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "send-text");
        }
        
        public static String sendFileUrl(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "send-file-url");
        }
        
        public static String sendAudio(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "send-audio");
        }
        
        public static String sendFileBase64(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "send-file-base64");
        }
        
        public static String uploadFile(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "upload-file");
        }
        
        public static String status(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "status");
        }
        
        public static String qrCode(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "qr-code");
        }
        
        public static String qrCodeImage(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "qr-code/image");
        }
        
        public static String phoneCode(ZApiUrlFactory factory, WhatsAppInstance instance, String phoneNumber) {
            return factory.buildUrlWithPathParams(instance, "phone-code", phoneNumber);
        }
        
        public static String restart(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "restart");
        }
        
        public static String disconnect(ZApiUrlFactory factory, WhatsAppInstance instance) {
            return factory.buildUrl(instance, "disconnect");
        }
    }

    /**
     * Validates input parameters for URL building.
     */
    private void validateInput(WhatsAppInstance instance, String endpoint) {
        Assert.notNull(instance, "WhatsApp instance cannot be null");
        Assert.hasText(instance.getInstanceId(), "Instance ID cannot be null or empty");
        Assert.hasText(instance.getAccessToken(), "Access token cannot be null or empty");
        Assert.hasText(endpoint, "Endpoint cannot be null or empty");
    }

    /**
     * Masks sensitive information in URLs for logging purposes.
     * 
     * @param url the URL to mask
     * @return masked URL with sensitive parts replaced
     */
    private String maskSensitiveInfo(String url) {
        if (url == null) {
            return null;
        }
        
        // Mask the access token in the URL for security
        return url.replaceAll("/token/[^/]+/", "/token/***masked***/");
    }

    /**
     * Gets the base URL used by this factory.
     * 
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}