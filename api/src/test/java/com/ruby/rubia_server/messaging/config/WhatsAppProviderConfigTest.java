package com.ruby.rubia_server.messaging.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppProviderConfigTest {

    private WhatsAppProviderConfig config;

    @BeforeEach
    void setUp() {
        config = new WhatsAppProviderConfig();
    }

    @Test
    void shouldSetAndGetAccountId() {
        String accountId = "test_account_123";
        
        config.setAccountId(accountId);
        
        assertEquals(accountId, config.getAccountId());
    }

    @Test
    void shouldSetAndGetAuthToken() {
        String authToken = "test_token_456";
        
        config.setAuthToken(authToken);
        
        assertEquals(authToken, config.getAuthToken());
    }

    @Test
    void shouldSetAndGetPhoneNumber() {
        String phoneNumber = "whatsapp:+5511999999999";
        
        config.setPhoneNumber(phoneNumber);
        
        assertEquals(phoneNumber, config.getPhoneNumber());
    }

    @Test
    void shouldSetAndGetApiUrl() {
        String apiUrl = "https://api.example.com";
        
        config.setApiUrl(apiUrl);
        
        assertEquals(apiUrl, config.getApiUrl());
    }

    @Test
    void shouldHandleNullValues() {
        config.setAccountId(null);
        config.setAuthToken(null);
        config.setPhoneNumber(null);
        config.setApiUrl(null);
        
        assertNull(config.getAccountId());
        assertNull(config.getAuthToken());
        assertNull(config.getPhoneNumber());
        assertNull(config.getApiUrl());
    }

    @Test
    void shouldInitializeWithDefaultValues() {
        assertNull(config.getAccountId());
        assertNull(config.getAuthToken());
        assertNull(config.getPhoneNumber());
        assertNull(config.getApiUrl());
    }

    @Test
    void shouldBindPropertiesFromMap() {
        Map<String, Object> properties = Map.of(
            "whatsapp.provider.account-id", "bound_account_123",
            "whatsapp.provider.auth-token", "bound_token_456",
            "whatsapp.provider.phone-number", "whatsapp:+5511888888888",
            "whatsapp.provider.api-url", "https://bound.api.com"
        );

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        
        WhatsAppProviderConfig boundConfig = binder.bind("whatsapp.provider", WhatsAppProviderConfig.class)
                .orElse(new WhatsAppProviderConfig());

        assertEquals("bound_account_123", boundConfig.getAccountId());
        assertEquals("bound_token_456", boundConfig.getAuthToken());
        assertEquals("whatsapp:+5511888888888", boundConfig.getPhoneNumber());
        assertEquals("https://bound.api.com", boundConfig.getApiUrl());
    }

    @Test
    void shouldBindPartialPropertiesFromMap() {
        Map<String, Object> properties = Map.of(
            "whatsapp.provider.account-id", "partial_account",
            "whatsapp.provider.phone-number", "+5511777777777"
        );

        ConfigurationPropertySource source = new MapConfigurationPropertySource(properties);
        Binder binder = new Binder(source);
        
        WhatsAppProviderConfig boundConfig = binder.bind("whatsapp.provider", WhatsAppProviderConfig.class)
                .orElse(new WhatsAppProviderConfig());

        assertEquals("partial_account", boundConfig.getAccountId());
        assertNull(boundConfig.getAuthToken());
        assertEquals("+5511777777777", boundConfig.getPhoneNumber());
        assertNull(boundConfig.getApiUrl());
    }

    @Test
    void shouldHandleEmptyStringValues() {
        config.setAccountId("");
        config.setAuthToken("");
        config.setPhoneNumber("");
        config.setApiUrl("");

        assertEquals("", config.getAccountId());
        assertEquals("", config.getAuthToken());
        assertEquals("", config.getPhoneNumber());
        assertEquals("", config.getApiUrl());
    }

    @Test
    void shouldHandleSpecialCharactersInValues() {
        String accountWithSpecialChars = "acc@ount#123$%^";
        String tokenWithSpecialChars = "t0k3n!@#$%^&*()";
        String phoneWithSpecialChars = "whatsapp:+55(11)99999-9999";
        String urlWithSpecialChars = "https://api.example.com/v1?key=value&token=123";

        config.setAccountId(accountWithSpecialChars);
        config.setAuthToken(tokenWithSpecialChars);
        config.setPhoneNumber(phoneWithSpecialChars);
        config.setApiUrl(urlWithSpecialChars);

        assertEquals(accountWithSpecialChars, config.getAccountId());
        assertEquals(tokenWithSpecialChars, config.getAuthToken());
        assertEquals(phoneWithSpecialChars, config.getPhoneNumber());
        assertEquals(urlWithSpecialChars, config.getApiUrl());
    }

    @Test
    void shouldSupportDifferentProviderConfigurations() {
        // Test Twilio configuration
        config.setAccountId("AC1234567890");
        config.setAuthToken("auth_token_twilio");
        config.setPhoneNumber("whatsapp:+14155238886");
        config.setApiUrl("https://api.twilio.com");

        assertEquals("AC1234567890", config.getAccountId());
        assertEquals("auth_token_twilio", config.getAuthToken());
        assertEquals("whatsapp:+14155238886", config.getPhoneNumber());
        assertEquals("https://api.twilio.com", config.getApiUrl());

        // Test WhatsApp Cloud API configuration
        config.setAccountId("business_account_id");
        config.setAuthToken("permanent_access_token");
        config.setPhoneNumber("+5511999999999");
        config.setApiUrl("https://graph.facebook.com/v18.0");

        assertEquals("business_account_id", config.getAccountId());
        assertEquals("permanent_access_token", config.getAuthToken());
        assertEquals("+5511999999999", config.getPhoneNumber());
        assertEquals("https://graph.facebook.com/v18.0", config.getApiUrl());
    }
}