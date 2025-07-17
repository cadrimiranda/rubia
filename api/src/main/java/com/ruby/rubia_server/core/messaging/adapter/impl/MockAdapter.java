package com.ruby.rubia_server.core.messaging.adapter.impl;

import com.ruby.rubia_server.core.messaging.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.messaging.model.MessageResult;
import com.ruby.rubia_server.core.messaging.model.IncomingMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "mock")
public class MockAdapter implements MessagingAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(MockAdapter.class);
    private final List<Object> sentMessages = new ArrayList<>();
    
    @Override
    public MessageResult sendMessage(String to, String message) {
        String messageId = "mock_" + UUID.randomUUID().toString();
        
        sentMessages.add(Map.of(
            "id", messageId,
            "to", to,
            "message", message,
            "timestamp", LocalDateTime.now()
        ));
        
        logger.info("Mock message sent: {} -> {}", to, message);
        
        return MessageResult.success(messageId, "sent", getProviderName());
    }
    
    @Override
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        String messageId = "mock_" + UUID.randomUUID().toString();
        
        sentMessages.add(Map.of(
            "id", messageId,
            "to", to,
            "mediaUrl", mediaUrl,
            "caption", caption != null ? caption : "",
            "timestamp", LocalDateTime.now()
        ));
        
        logger.info("Mock media message sent: {} -> {}", to, mediaUrl);
        
        return MessageResult.success(messageId, "sent", getProviderName());
    }
    
    @Override
    public IncomingMessage parseIncomingMessage(Object webhookPayload) {
        @SuppressWarnings("unchecked")
        Map<String, String> payload = (Map<String, String>) webhookPayload;
        
        return IncomingMessage.builder()
                .messageId("mock_" + UUID.randomUUID().toString())
                .from(payload.getOrDefault("from", "+5511999999999"))
                .to(payload.getOrDefault("to", "+5511888888888"))
                .body(payload.getOrDefault("message", "Mock message"))
                .timestamp(LocalDateTime.now())
                .provider(getProviderName())
                .rawPayload(payload)
                .build();
    }
    
    @Override
    public boolean validateWebhook(Object payload, String signature) {
        return true;
    }
    
    @Override
    public String getProviderName() {
        return "mock";
    }
    
    public List<Object> getSentMessages() {
        return new ArrayList<>(sentMessages);
    }
}