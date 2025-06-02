package com.ruby.rubia_server.messaging.adapter.impl;

import com.ruby.rubia_server.messaging.model.MessageResult;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MockAdapterTest {

    private MockAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockAdapter();
    }

    @Test
    void shouldReturnCorrectProviderName() {
        assertEquals("mock", mockAdapter.getProviderName());
    }

    @Test
    void shouldSendMessageSuccessfully() {
        String to = "+5511999999999";
        String message = "Test message";

        MessageResult result = mockAdapter.sendMessage(to, message);

        assertTrue(result.isSuccess());
        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("mock_"));
        assertEquals("sent", result.getStatus());
        assertEquals("mock", result.getProvider());
        assertNull(result.getError());
    }

    @Test
    void shouldSendMediaMessageSuccessfully() {
        String to = "+5511999999999";
        String mediaUrl = "https://example.com/image.jpg";
        String caption = "Test image";

        MessageResult result = mockAdapter.sendMediaMessage(to, mediaUrl, caption);

        assertTrue(result.isSuccess());
        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("mock_"));
        assertEquals("sent", result.getStatus());
        assertEquals("mock", result.getProvider());
        assertNull(result.getError());
    }

    @Test
    void shouldSendMediaMessageWithNullCaption() {
        String to = "+5511999999999";
        String mediaUrl = "https://example.com/video.mp4";

        MessageResult result = mockAdapter.sendMediaMessage(to, mediaUrl, null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getMessageId());
        assertEquals("sent", result.getStatus());
        assertEquals("mock", result.getProvider());
    }

    @Test
    void shouldTrackSentMessages() {
        String to1 = "+5511111111111";
        String to2 = "+5511222222222";
        String message1 = "First message";
        String message2 = "Second message";

        mockAdapter.sendMessage(to1, message1);
        mockAdapter.sendMessage(to2, message2);

        List<Object> sentMessages = mockAdapter.getSentMessages();

        assertEquals(2, sentMessages.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> firstMessage = (Map<String, Object>) sentMessages.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> secondMessage = (Map<String, Object>) sentMessages.get(1);

        assertEquals(to1, firstMessage.get("to"));
        assertEquals(message1, firstMessage.get("message"));
        assertEquals(to2, secondMessage.get("to"));
        assertEquals(message2, secondMessage.get("message"));
    }

    @Test
    void shouldTrackSentMediaMessages() {
        String to = "+5511999999999";
        String mediaUrl = "https://example.com/file.pdf";
        String caption = "Important document";

        mockAdapter.sendMediaMessage(to, mediaUrl, caption);

        List<Object> sentMessages = mockAdapter.getSentMessages();

        assertEquals(1, sentMessages.size());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) sentMessages.get(0);

        assertEquals(to, message.get("to"));
        assertEquals(mediaUrl, message.get("mediaUrl"));
        assertEquals(caption, message.get("caption"));
        assertNotNull(message.get("timestamp"));
    }

    @Test
    void shouldAlwaysValidateWebhook() {
        Map<String, String> payload = Map.of("test", "data");
        String signature = "any_signature";

        boolean result = mockAdapter.validateWebhook(payload, signature);

        assertTrue(result);
    }

    @Test
    void shouldValidateWebhookEvenWithNullSignature() {
        Map<String, String> payload = Map.of("test", "data");

        boolean result = mockAdapter.validateWebhook(payload, null);

        assertTrue(result);
    }

    @Test
    void shouldParseIncomingMessageWithDefaults() {
        Map<String, String> payload = Map.of();

        IncomingMessage result = mockAdapter.parseIncomingMessage(payload);

        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("mock_"));
        assertEquals("+5511999999999", result.getFrom());
        assertEquals("+5511888888888", result.getTo());
        assertEquals("Mock message", result.getBody());
        assertEquals("mock", result.getProvider());
        assertEquals(payload, result.getRawPayload());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldParseIncomingMessageWithProvidedValues() {
        Map<String, String> payload = Map.of(
            "from", "+5511111111111",
            "to", "+5511222222222",
            "message", "Custom message"
        );

        IncomingMessage result = mockAdapter.parseIncomingMessage(payload);

        assertNotNull(result.getMessageId());
        assertTrue(result.getMessageId().startsWith("mock_"));
        assertEquals("+5511111111111", result.getFrom());
        assertEquals("+5511222222222", result.getTo());
        assertEquals("Custom message", result.getBody());
        assertEquals("mock", result.getProvider());
        assertEquals(payload, result.getRawPayload());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void shouldGenerateUniqueMessageIds() {
        MessageResult result1 = mockAdapter.sendMessage("+5511111111111", "Message 1");
        MessageResult result2 = mockAdapter.sendMessage("+5511222222222", "Message 2");

        assertNotEquals(result1.getMessageId(), result2.getMessageId());
        assertTrue(result1.getMessageId().startsWith("mock_"));
        assertTrue(result2.getMessageId().startsWith("mock_"));
    }

    @Test
    void shouldHandleEmptyPayloadInParseIncomingMessage() {
        Map<String, String> emptyPayload = Map.of();

        IncomingMessage result = mockAdapter.parseIncomingMessage(emptyPayload);

        assertNotNull(result);
        assertEquals("Mock message", result.getBody());
        assertEquals("+5511999999999", result.getFrom());
        assertEquals("+5511888888888", result.getTo());
    }
}