package com.ruby.rubia_server.messaging.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class IncomingMessageTest {

    @Test
    void shouldBuildIncomingMessageWithBuilder() {
        LocalDateTime timestamp = LocalDateTime.now();
        Map<String, String> payload = Map.of("key", "value");

        IncomingMessage message = IncomingMessage.builder()
                .messageId("msg123")
                .from("+5511999999999")
                .to("+5511888888888")
                .body("Hello World")
                .mediaUrl("https://example.com/image.jpg")
                .mediaType("image/jpeg")
                .timestamp(timestamp)
                .provider("twilio")
                .rawPayload(payload)
                .build();

        assertEquals("msg123", message.getMessageId());
        assertEquals("+5511999999999", message.getFrom());
        assertEquals("+5511888888888", message.getTo());
        assertEquals("Hello World", message.getBody());
        assertEquals("https://example.com/image.jpg", message.getMediaUrl());
        assertEquals("image/jpeg", message.getMediaType());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals("twilio", message.getProvider());
        assertEquals(payload, message.getRawPayload());
    }

    @Test
    void shouldSetAndGetProperties() {
        IncomingMessage message = new IncomingMessage();
        LocalDateTime timestamp = LocalDateTime.now();
        Map<String, String> payload = Map.of("test", "data");

        message.setMessageId("test456");
        message.setFrom("+5511111111111");
        message.setTo("+5511222222222");
        message.setBody("Test message");
        message.setMediaUrl("https://test.com/file.pdf");
        message.setMediaType("application/pdf");
        message.setTimestamp(timestamp);
        message.setProvider("mock");
        message.setRawPayload(payload);

        assertEquals("test456", message.getMessageId());
        assertEquals("+5511111111111", message.getFrom());
        assertEquals("+5511222222222", message.getTo());
        assertEquals("Test message", message.getBody());
        assertEquals("https://test.com/file.pdf", message.getMediaUrl());
        assertEquals("application/pdf", message.getMediaType());
        assertEquals(timestamp, message.getTimestamp());
        assertEquals("mock", message.getProvider());
        assertEquals(payload, message.getRawPayload());
    }

    @Test
    void shouldHandleNullValues() {
        IncomingMessage message = IncomingMessage.builder()
                .messageId(null)
                .from(null)
                .to(null)
                .body(null)
                .mediaUrl(null)
                .mediaType(null)
                .timestamp(null)
                .provider(null)
                .rawPayload(null)
                .build();

        assertNull(message.getMessageId());
        assertNull(message.getFrom());
        assertNull(message.getTo());
        assertNull(message.getBody());
        assertNull(message.getMediaUrl());
        assertNull(message.getMediaType());
        assertNull(message.getTimestamp());
        assertNull(message.getProvider());
        assertNull(message.getRawPayload());
    }

    @Test
    void shouldCreateEmptyIncomingMessage() {
        IncomingMessage message = new IncomingMessage();

        assertNull(message.getMessageId());
        assertNull(message.getFrom());
        assertNull(message.getTo());
        assertNull(message.getBody());
        assertNull(message.getMediaUrl());
        assertNull(message.getMediaType());
        assertNull(message.getTimestamp());
        assertNull(message.getProvider());
        assertNull(message.getRawPayload());
    }

    @Test
    void shouldHandleComplexRawPayload() {
        Map<String, Object> complexPayload = Map.of(
            "nested", Map.of("key", "value"),
            "array", java.util.Arrays.asList("item1", "item2"),
            "number", 42
        );

        IncomingMessage message = IncomingMessage.builder()
                .messageId("complex123")
                .rawPayload(complexPayload)
                .build();

        assertEquals("complex123", message.getMessageId());
        assertEquals(complexPayload, message.getRawPayload());
    }
}