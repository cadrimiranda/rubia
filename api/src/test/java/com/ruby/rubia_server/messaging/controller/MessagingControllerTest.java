package com.ruby.rubia_server.messaging.controller;

import com.ruby.rubia_server.messaging.service.MessagingService;
import com.ruby.rubia_server.messaging.model.MessageResult;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MessagingController.class)
class MessagingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessagingService messagingService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSendMessageSuccessfully() throws Exception {
        Map<String, String> request = Map.of(
            "to", "+5511999999999",
            "message", "Hello World"
        );

        MessageResult expectedResult = MessageResult.success("msg123", "sent", "mock");
        when(messagingService.sendMessage("+5511999999999", "Hello World")).thenReturn(expectedResult);

        mockMvc.perform(post("/api/messaging/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messageId").value("msg123"))
                .andExpect(jsonPath("$.status").value("sent"))
                .andExpect(jsonPath("$.provider").value("mock"));

        verify(messagingService).sendMessage("+5511999999999", "Hello World");
    }

    @Test
    void shouldReturnBadRequestWhenSendMessageFails() throws Exception {
        Map<String, String> request = Map.of(
            "to", "+5511999999999",
            "message", "Hello World"
        );

        MessageResult expectedResult = MessageResult.error("Failed to send", "mock");
        when(messagingService.sendMessage("+5511999999999", "Hello World")).thenReturn(expectedResult);

        mockMvc.perform(post("/api/messaging/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Failed to send"))
                .andExpect(jsonPath("$.provider").value("mock"));

        verify(messagingService).sendMessage("+5511999999999", "Hello World");
    }

    @Test
    void shouldSendMediaMessageSuccessfully() throws Exception {
        Map<String, String> request = Map.of(
            "to", "+5511999999999",
            "mediaUrl", "https://example.com/image.jpg",
            "caption", "Test image"
        );

        MessageResult expectedResult = MessageResult.success("msg456", "sent", "mock");
        when(messagingService.sendMediaMessage("+5511999999999", "https://example.com/image.jpg", "Test image"))
                .thenReturn(expectedResult);

        mockMvc.perform(post("/api/messaging/send-media")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.messageId").value("msg456"));

        verify(messagingService).sendMediaMessage("+5511999999999", "https://example.com/image.jpg", "Test image");
    }

    @Test
    void shouldReturnBadRequestWhenSendMediaMessageFails() throws Exception {
        Map<String, String> request = Map.of(
            "to", "+5511999999999",
            "mediaUrl", "https://example.com/image.jpg"
        );

        MessageResult expectedResult = MessageResult.error("Media upload failed", "mock");
        when(messagingService.sendMediaMessage("+5511999999999", "https://example.com/image.jpg", null))
                .thenReturn(expectedResult);

        mockMvc.perform(post("/api/messaging/send-media")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Media upload failed"));
    }

    @Test
    void shouldHandleWebhookSuccessfully() throws Exception {
        Map<String, String> webhookPayload = Map.of(
            "MessageSid", "SM123456789",
            "From", "whatsapp:+5511999999999",
            "Body", "Hello from webhook"
        );

        IncomingMessage incomingMessage = IncomingMessage.builder()
                .messageId("SM123456789")
                .from("+5511999999999")
                .body("Hello from webhook")
                .timestamp(LocalDateTime.now())
                .provider("twilio")
                .build();

        when(messagingService.validateWebhook(any(), eq("valid_signature"))).thenReturn(true);
        when(messagingService.parseIncomingMessage(any())).thenReturn(incomingMessage);

        mockMvc.perform(post("/api/messaging/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload))
                .header("X-Twilio-Signature", "valid_signature"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(messagingService).validateWebhook(any(), eq("valid_signature"));
        verify(messagingService).parseIncomingMessage(any());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidWebhookSignature() throws Exception {
        Map<String, String> webhookPayload = Map.of(
            "MessageSid", "SM123456789"
        );

        when(messagingService.validateWebhook(any(), eq("invalid_signature"))).thenReturn(false);

        mockMvc.perform(post("/api/messaging/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload))
                .header("X-Twilio-Signature", "invalid_signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));

        verify(messagingService).validateWebhook(any(), eq("invalid_signature"));
        verify(messagingService, never()).parseIncomingMessage(any());
    }

    @Test
    void shouldHandleWebhookWithoutSignature() throws Exception {
        Map<String, String> webhookPayload = Map.of(
            "MessageSid", "SM123456789"
        );

        when(messagingService.validateWebhook(any(), isNull())).thenReturn(false);

        mockMvc.perform(post("/api/messaging/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isUnauthorized());

        verify(messagingService).validateWebhook(any(), isNull());
    }

    @Test
    void shouldSwitchProviderSuccessfully() throws Exception {
        Map<String, String> request = Map.of("provider", "twilio");

        when(messagingService.getCurrentProvider()).thenReturn("twilio");
        when(messagingService.getAvailableProviders()).thenReturn(Arrays.asList("mock", "twilio"));

        mockMvc.perform(post("/api/messaging/switch-provider")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.currentProvider").value("twilio"))
                .andExpect(jsonPath("$.availableProviders").isArray())
                .andExpect(jsonPath("$.availableProviders.length()").value(2));

        verify(messagingService).switchAdapter("twilio");
        verify(messagingService).getCurrentProvider();
        verify(messagingService).getAvailableProviders();
    }

    @Test
    void shouldReturnBadRequestWhenSwitchingToInvalidProvider() throws Exception {
        Map<String, String> request = Map.of("provider", "invalid");

        doThrow(new RuntimeException("Adapter not found: invalid"))
                .when(messagingService).switchAdapter("invalid");

        mockMvc.perform(post("/api/messaging/switch-provider")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Adapter not found: invalid"));

        verify(messagingService).switchAdapter("invalid");
    }

    @Test
    void shouldGetStatusSuccessfully() throws Exception {
        when(messagingService.getCurrentProvider()).thenReturn("mock");
        when(messagingService.getAvailableProviders()).thenReturn(Arrays.asList("mock", "twilio"));

        mockMvc.perform(get("/api/messaging/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProvider").value("mock"))
                .andExpect(jsonPath("$.availableProviders").isArray())
                .andExpect(jsonPath("$.availableProviders.length()").value(2));

        verify(messagingService).getCurrentProvider();
        verify(messagingService).getAvailableProviders();
    }

    @Test
    void shouldProcessGreetingInIncomingMessage() throws Exception {
        Map<String, String> webhookPayload = Map.of(
            "MessageSid", "SM123456789",
            "From", "whatsapp:+5511999999999",
            "Body", "Oi, como está?"
        );

        IncomingMessage incomingMessage = IncomingMessage.builder()
                .messageId("SM123456789")
                .from("+5511999999999")
                .body("Oi, como está?")
                .build();

        MessageResult autoReply = MessageResult.success("reply123", "sent", "mock");

        when(messagingService.validateWebhook(any(), any())).thenReturn(true);
        when(messagingService.parseIncomingMessage(any())).thenReturn(incomingMessage);
        when(messagingService.sendMessage("+5511999999999", "Olá! Como posso ajudar você?"))
                .thenReturn(autoReply);

        mockMvc.perform(post("/api/messaging/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload))
                .header("X-Twilio-Signature", "valid"))
                .andExpect(status().isOk());

        verify(messagingService).sendMessage("+5511999999999", "Olá! Como posso ajudar você?");
    }
}