package com.ruby.rubia_server.messaging.adapter.impl;

import com.ruby.rubia_server.messaging.config.WhatsAppProviderConfig;
import com.ruby.rubia_server.messaging.model.MessageResult;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwilioAdapterTest {

    @Mock
    private WhatsAppProviderConfig config;

    @InjectMocks
    private TwilioAdapter twilioAdapter;

    @BeforeEach
    void setUp() {
        when(config.getAccountId()).thenReturn("test_account_id");
        when(config.getAuthToken()).thenReturn("test_auth_token");
        when(config.getPhoneNumber()).thenReturn("whatsapp:+14155238886");
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("twilio", twilioAdapter.getProviderName());
    }

    @Test
    void shouldValidateWebhookWithValidSignature() {
        Map<String, String> payload = Map.of("test", "data");
        String signature = "valid_signature";

        boolean result = twilioAdapter.validateWebhook(payload, signature);

        assertTrue(result);
    }

    @Test
    void shouldNotValidateWebhookWithNullSignature() {
        Map<String, String> payload = Map.of("test", "data");

        boolean result = twilioAdapter.validateWebhook(payload, null);

        assertFalse(result);
    }

    @Test
    void shouldNotValidateWebhookWithEmptySignature() {
        Map<String, String> payload = Map.of("test", "data");

        boolean result = twilioAdapter.validateWebhook(payload, "");

        assertFalse(result);
    }

    @Test
    void shouldParseIncomingMessageFromTwilioWebhook() {
        Map<String, String> payload = Map.of(
            "MessageSid", "SM123456789",
            "From", "whatsapp:+5511999999999",
            "To", "whatsapp:+14155238886",
            "Body", "Hello from Twilio",
            "MediaUrl0", "https://example.com/image.jpg",
            "MediaContentType0", "image/jpeg"
        );

        IncomingMessage result = twilioAdapter.parseIncomingMessage(payload);

        assertEquals("SM123456789", result.getMessageId());
        assertEquals("+5511999999999", result.getFrom());
        assertEquals("+14155238886", result.getTo());
        assertEquals("Hello from Twilio", result.getBody());
        assertEquals("https://example.com/image.jpg", result.getMediaUrl());
        assertEquals("image/jpeg", result.getMediaType());
        assertEquals("twilio", result.getProvider());
        assertEquals(payload, result.getRawPayload());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldParseIncomingMessageWithNullFields() {
        Map<String, String> payload = Map.of(
            "MessageSid", "SM123456789"
        );

        IncomingMessage result = twilioAdapter.parseIncomingMessage(payload);

        assertEquals("SM123456789", result.getMessageId());
        assertNull(result.getFrom());
        assertNull(result.getTo());
        assertNull(result.getBody());
        assertNull(result.getMediaUrl());
        assertNull(result.getMediaType());
        assertEquals("twilio", result.getProvider());
        assertEquals(payload, result.getRawPayload());
    }

    @Test
    void shouldThrowExceptionWhenParsingInvalidWebhook() {
        String invalidPayload = "invalid";

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            twilioAdapter.parseIncomingMessage(invalidPayload);
        });

        assertEquals("Invalid Twilio webhook payload", exception.getMessage());
    }

    @Test
    void shouldFormatPhoneNumberWithWhatsAppPrefix() {
        when(config.getPhoneNumber()).thenReturn("whatsapp:+14155238886");

        String result = twilioAdapter.formatPhoneNumber("+5511999999999");

        assertEquals("whatsapp:+5511999999999", result);
    }

    @Test
    void shouldNotFormatPhoneNumberWhenAlreadyHasPrefix() {
        when(config.getPhoneNumber()).thenReturn("whatsapp:+14155238886");

        String result = twilioAdapter.formatPhoneNumber("whatsapp:+5511999999999");

        assertEquals("whatsapp:+5511999999999", result);
    }

    @Test
    void shouldNotFormatPhoneNumberWhenConfigHasNoPrefix() {
        when(config.getPhoneNumber()).thenReturn("+14155238886");

        String result = twilioAdapter.formatPhoneNumber("+5511999999999");

        assertEquals("+5511999999999", result);
    }

    @Test
    void shouldCleanPhoneNumber() {
        String result = twilioAdapter.cleanPhoneNumber("whatsapp:+5511999999999");

        assertEquals("+5511999999999", result);
    }

    @Test
    void shouldReturnNullWhenCleaningNullPhoneNumber() {
        String result = twilioAdapter.cleanPhoneNumber(null);

        assertNull(result);
    }

    @Test
    void shouldNotCleanPhoneNumberWithoutPrefix() {
        String result = twilioAdapter.cleanPhoneNumber("+5511999999999");

        assertEquals("+5511999999999", result);
    }
}