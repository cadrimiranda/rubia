package com.ruby.rubia_server.messaging.service;

import com.ruby.rubia_server.messaging.adapter.MessagingAdapter;
import com.ruby.rubia_server.messaging.model.MessageResult;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock
    private MessagingAdapter mockAdapter;

    @Mock
    private MessagingAdapter twilioAdapter;

    private MessagingService messagingService;

    @BeforeEach
    void setUp() {
        lenient().when(mockAdapter.getProviderName()).thenReturn("mock");
        lenient().when(twilioAdapter.getProviderName()).thenReturn("twilio");
    }

    @Test
    void shouldInitializeWithFirstAvailableAdapter() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter, twilioAdapter);
        messagingService = new MessagingService(adapters);

        assertEquals("mock", messagingService.getCurrentProvider());
    }

    @Test
    void shouldInitializeWithEmptyAdapterList() {
        List<MessagingAdapter> adapters = Collections.emptyList();
        messagingService = new MessagingService(adapters);

        assertEquals("none", messagingService.getCurrentProvider());
    }

    @Test
    void shouldSendMessageSuccessfully() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter);
        messagingService = new MessagingService(adapters);

        MessageResult expectedResult = MessageResult.success("msg123", "sent", "mock");
        when(mockAdapter.sendMessage("to", "message")).thenReturn(expectedResult);

        MessageResult result = messagingService.sendMessage("to", "message");

        assertEquals(expectedResult, result);
        verify(mockAdapter).sendMessage("to", "message");
    }

    @Test
    void shouldReturnErrorWhenNoAdapterAvailable() {
        List<MessagingAdapter> adapters = Collections.emptyList();
        messagingService = new MessagingService(adapters);

        MessageResult result = messagingService.sendMessage("to", "message");

        assertFalse(result.isSuccess());
        assertEquals("No adapter available", result.getError());
        assertEquals("none", result.getProvider());
    }

    @Test
    void shouldSendMediaMessageSuccessfully() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter);
        messagingService = new MessagingService(adapters);

        MessageResult expectedResult = MessageResult.success("msg456", "sent", "mock");
        when(mockAdapter.sendMediaMessage("to", "mediaUrl", "caption")).thenReturn(expectedResult);

        MessageResult result = messagingService.sendMediaMessage("to", "mediaUrl", "caption");

        assertEquals(expectedResult, result);
        verify(mockAdapter).sendMediaMessage("to", "mediaUrl", "caption");
    }

    @Test
    void shouldReturnErrorWhenSendingMediaMessageWithNoAdapter() {
        List<MessagingAdapter> adapters = Collections.emptyList();
        messagingService = new MessagingService(adapters);

        MessageResult result = messagingService.sendMediaMessage("to", "mediaUrl", "caption");

        assertFalse(result.isSuccess());
        assertEquals("No adapter available", result.getError());
        assertEquals("none", result.getProvider());
    }

    @Test
    void shouldParseIncomingMessageSuccessfully() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter);
        messagingService = new MessagingService(adapters);

        Map<String, String> payload = Map.of("test", "data");
        IncomingMessage expectedMessage = IncomingMessage.builder()
                .messageId("msg789")
                .body("test message")
                .build();

        when(mockAdapter.parseIncomingMessage(payload)).thenReturn(expectedMessage);

        IncomingMessage result = messagingService.parseIncomingMessage(payload);

        assertEquals(expectedMessage, result);
        verify(mockAdapter).parseIncomingMessage(payload);
    }

    @Test
    void shouldThrowExceptionWhenParsingWithNoAdapter() {
        List<MessagingAdapter> adapters = Collections.emptyList();
        messagingService = new MessagingService(adapters);

        Map<String, String> payload = Map.of("test", "data");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messagingService.parseIncomingMessage(payload);
        });

        assertEquals("No messaging adapter configured", exception.getMessage());
    }

    @Test
    void shouldValidateWebhookSuccessfully() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter);
        messagingService = new MessagingService(adapters);

        Map<String, String> payload = Map.of("test", "data");
        when(mockAdapter.validateWebhook(payload, "signature")).thenReturn(true);

        boolean result = messagingService.validateWebhook(payload, "signature");

        assertTrue(result);
        verify(mockAdapter).validateWebhook(payload, "signature");
    }

    @Test
    void shouldReturnFalseWhenValidatingWebhookWithNoAdapter() {
        List<MessagingAdapter> adapters = Collections.emptyList();
        messagingService = new MessagingService(adapters);

        Map<String, String> payload = Map.of("test", "data");

        boolean result = messagingService.validateWebhook(payload, "signature");

        assertFalse(result);
    }

    @Test
    void shouldSwitchAdapterSuccessfully() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter, twilioAdapter);
        messagingService = new MessagingService(adapters);

        assertEquals("mock", messagingService.getCurrentProvider());

        messagingService.switchAdapter("twilio");

        assertEquals("twilio", messagingService.getCurrentProvider());
    }

    @Test
    void shouldThrowExceptionWhenSwitchingToNonExistentAdapter() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter);
        messagingService = new MessagingService(adapters);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            messagingService.switchAdapter("nonexistent");
        });

        assertEquals("Adapter not found: nonexistent", exception.getMessage());
        assertEquals("mock", messagingService.getCurrentProvider()); // Should remain unchanged
    }

    @Test
    void shouldReturnAvailableProviders() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter, twilioAdapter);
        messagingService = new MessagingService(adapters);

        List<String> providers = messagingService.getAvailableProviders();

        assertEquals(2, providers.size());
        assertTrue(providers.contains("mock"));
        assertTrue(providers.contains("twilio"));
    }

    @Test
    void shouldReturnEmptyListWhenNoAdaptersAvailable() {
        List<MessagingAdapter> adapters = Collections.emptyList();
        messagingService = new MessagingService(adapters);

        List<String> providers = messagingService.getAvailableProviders();

        assertTrue(providers.isEmpty());
    }

    @Test
    void shouldUseSwitchedAdapterForOperations() {
        List<MessagingAdapter> adapters = Arrays.asList(mockAdapter, twilioAdapter);
        messagingService = new MessagingService(adapters);

        messagingService.switchAdapter("twilio");

        MessageResult expectedResult = MessageResult.success("twilio123", "sent", "twilio");
        when(twilioAdapter.sendMessage("to", "message")).thenReturn(expectedResult);

        MessageResult result = messagingService.sendMessage("to", "message");

        assertEquals(expectedResult, result);
        verify(twilioAdapter).sendMessage("to", "message");
        verify(mockAdapter, never()).sendMessage(anyString(), anyString());
    }
}