package com.ruby.rubia_server.config;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.entity.MessageResult;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that provides mock implementations for components
 * that might cause issues in test environments.
 */
@TestConfiguration
public class MockAdapterTestConfiguration {

    /**
     * Mock MessagingAdapter to avoid ZApiAdapter initialization issues in tests
     */
    @Bean
    @Primary
    public MessagingAdapter mockMessagingAdapter() {
        return new MessagingAdapter() {
            @Override
            public MessageResult sendMessage(String to, String message) {
                return MessageResult.success("mock-message-id", "sent", "mock");
            }

            @Override
            public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
                return MessageResult.success("mock-media-id", "sent", "mock");
            }

            @Override
            public IncomingMessage parseIncomingMessage(Object webhookPayload) {
                return null; // Mock implementation
            }

            @Override
            public boolean validateWebhook(Object payload, String signature) {
                return true; // Mock always validates
            }

            @Override
            public String getProviderName() {
                return "mock";
            }
        };
    }
}