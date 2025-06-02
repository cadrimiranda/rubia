package com.ruby.rubia_server.messaging.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageResultTest {

    @Test
    void shouldCreateSuccessMessageResult() {
        String messageId = "msg123";
        String status = "sent";
        String provider = "twilio";

        MessageResult result = MessageResult.success(messageId, status, provider);

        assertTrue(result.isSuccess());
        assertEquals(messageId, result.getMessageId());
        assertEquals(status, result.getStatus());
        assertEquals(provider, result.getProvider());
        assertNull(result.getError());
    }

    @Test
    void shouldCreateErrorMessageResult() {
        String error = "Failed to send message";
        String provider = "twilio";

        MessageResult result = MessageResult.error(error, provider);

        assertFalse(result.isSuccess());
        assertEquals(error, result.getError());
        assertEquals(provider, result.getProvider());
        assertNull(result.getMessageId());
        assertNull(result.getStatus());
    }

    @Test
    void shouldBuildMessageResultWithBuilder() {
        MessageResult result = MessageResult.builder()
                .success(true)
                .messageId("test123")
                .status("delivered")
                .provider("mock")
                .build();

        assertTrue(result.isSuccess());
        assertEquals("test123", result.getMessageId());
        assertEquals("delivered", result.getStatus());
        assertEquals("mock", result.getProvider());
    }

    @Test
    void shouldSetAndGetProperties() {
        MessageResult result = new MessageResult();
        
        result.setSuccess(true);
        result.setMessageId("msg456");
        result.setStatus("pending");
        result.setError("test error");
        result.setProvider("test-provider");

        assertTrue(result.isSuccess());
        assertEquals("msg456", result.getMessageId());
        assertEquals("pending", result.getStatus());
        assertEquals("test error", result.getError());
        assertEquals("test-provider", result.getProvider());
    }

    @Test
    void shouldHandleNullValues() {
        MessageResult result = MessageResult.error(null, null);

        assertFalse(result.isSuccess());
        assertNull(result.getError());
        assertNull(result.getProvider());
    }
}