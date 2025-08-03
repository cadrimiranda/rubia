package com.ruby.rubia_server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.adapter.impl.ZApiAdapter;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
    "messaging.provider=zapi"
})
class WebhookSimpleTest extends AbstractIntegrationTest {

    @Autowired
    private ZApiAdapter zApiAdapter;

    @Test
    void shouldParseIncomingTextMessageFromCustomer() {
        // Arrange - Create webhook payload from customer
        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG123", 
            "5511888888888", // from customer
            "5511999999999", // to our number
            false, // fromMe = false
            "Olá, preciso de ajuda!"
        );

        // Act - Parse message
        IncomingMessage result = zApiAdapter.parseIncomingMessage(webhookPayload);

        // Assert - Message parsed correctly
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("MSG123");
        assertThat(result.getFrom()).isEqualTo("5511888888888");
        assertThat(result.getTo()).isEqualTo("5511999999999");
        assertThat(result.getBody()).isEqualTo("Olá, preciso de ajuda!");
        assertThat(result.isFromMe()).isFalse();
        assertThat(result.getSenderName()).isEqualTo("Customer Name");
        assertThat(result.getProvider()).isEqualTo("z-api");
    }

    @Test 
    void shouldParseOutgoingTextMessageFromAgent() {
        // Arrange - Create webhook payload from agent (us)
        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG456",
            "5511999999999", // from our number  
            "5511888888888", // to customer
            true, // fromMe = true
            "Olá! Como posso ajudar?"
        );

        // Act - Parse message
        IncomingMessage result = zApiAdapter.parseIncomingMessage(webhookPayload);

        // Assert - Message parsed correctly (from/to as provided in payload)
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("MSG456");
        assertThat(result.getFrom()).isEqualTo("5511999999999"); // Phone as provided
        assertThat(result.getTo()).isEqualTo("5511888888888"); // ConnectedPhone as provided
        assertThat(result.getBody()).isEqualTo("Olá! Como posso ajudar?");
        assertThat(result.isFromMe()).isTrue();
        assertThat(result.getSenderName()).isEqualTo("Eu");
        assertThat(result.getProvider()).isEqualTo("z-api");
    }

    @Test
    void shouldIgnoreGroupMessages() {
        // Arrange - Create group message payload
        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG789",
            "5511777777777",
            "5511999999999", 
            false,
            "Mensagem do grupo"
        );
        webhookPayload.put("isGroup", true);

        // Act - Parse message
        IncomingMessage result = zApiAdapter.parseIncomingMessage(webhookPayload);

        // Assert - Group message ignored
        assertThat(result).isNull();
    }

    @Test
    void shouldIgnoreNewsletterMessages() {
        // Arrange - Create newsletter message payload
        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG999",
            "5511666666666@newsletter",
            "5511999999999",
            false, 
            "Newsletter content"
        );

        // Act - Parse message
        IncomingMessage result = zApiAdapter.parseIncomingMessage(webhookPayload);

        // Assert - Newsletter message ignored
        assertThat(result).isNull();
    }

    @Test
    void shouldParseAudioMessage() {
        // Arrange - Create audio message payload
        Map<String, Object> webhookPayload = createWebhookPayload(
            "AUDIO123",
            "5511888888888",
            "5511999999999",
            false,
            null // No text content for audio
        );
        
        // Add audio data
        Map<String, Object> audioMessage = new HashMap<>();
        Map<String, Object> audioData = new HashMap<>();
        audioData.put("url", "https://cdn.z-api.io/audio/test.ogg");
        audioData.put("mimeType", "audio/ogg");
        audioMessage.put("audioMessage", audioData);
        webhookPayload.put("message", audioMessage);

        // Act - Parse message
        IncomingMessage result = zApiAdapter.parseIncomingMessage(webhookPayload);

        // Assert - Audio message parsed correctly
        assertThat(result).isNotNull();
        assertThat(result.getMessageId()).isEqualTo("AUDIO123");
        assertThat(result.getMediaUrl()).isEqualTo("https://cdn.z-api.io/audio/test.ogg");
        assertThat(result.getMediaType()).isEqualTo("audio");
        assertThat(result.getMimeType()).isEqualTo("audio/ogg");
        assertThat(result.isFromMe()).isFalse();
    }

    private Map<String, Object> createWebhookPayload(String messageId, String phone, String connectedPhone, 
                                                    boolean fromMe, String messageText) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", "TEST_INSTANCE");
        payload.put("messageId", messageId);
        payload.put("phone", phone);
        payload.put("connectedPhone", connectedPhone);
        payload.put("fromMe", fromMe);
        payload.put("momment", System.currentTimeMillis() / 1000);
        payload.put("type", "ReceivedCallback");
        payload.put("senderName", fromMe ? "Eu" : "Customer Name");
        payload.put("isGroup", false);
        payload.put("messageExisting", true);

        // Add text message if provided
        if (messageText != null) {
            Map<String, Object> textData = new HashMap<>();
            textData.put("message", messageText);
            payload.put("text", textData);
        }

        return payload;
    }
}