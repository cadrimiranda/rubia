package com.ruby.rubia_server.core.adapter.impl;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.config.WhatsAppProviderConfig;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "twilio")
public class TwilioAdapter implements MessagingAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(TwilioAdapter.class);
    
    @Autowired
    private WhatsAppProviderConfig config;
    
    @PostConstruct
    public void init() {
        Twilio.init(config.getAccountId(), config.getAuthToken());
        logger.info("Twilio initialized successfully with provider config");
    }
    
    @Override
    public MessageResult sendMessage(String to, String message) {
        return sendMessage(to, message, null);
    }
    
    public MessageResult sendMessage(String to, String message, String fromNumber) {
        try {
            String fromPhoneNumber = fromNumber != null ? fromNumber : config.getPhoneNumber();
            
            // Verificar se temos um número de telefone válido
            if (fromPhoneNumber == null || fromPhoneNumber.trim().isEmpty()) {
                throw new IllegalStateException("WhatsApp phone number not configured. Please set WHATSAPP_PHONE_NUMBER environment variable.");
            }
            
            Message twilioMessage = Message.creator(
                new PhoneNumber(formatPhoneNumber(to)),
                new PhoneNumber(formatPhoneNumber(fromPhoneNumber)),
                message
            ).create();
            
            logger.info("Message sent successfully. SID: {}", twilioMessage.getSid());
            
            return MessageResult.success(
                twilioMessage.getSid(), 
                twilioMessage.getStatus().toString(),
                getProviderName()
            );
            
        } catch (Exception e) {
            logger.error("Failed to send message via Twilio", e);
            return MessageResult.error(e.getMessage(), getProviderName());
        }
    }
    
    @Override
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        return sendMediaMessage(to, mediaUrl, caption, null);
    }
    
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption, String fromNumber) {
        try {
            String fromPhoneNumber = fromNumber != null ? fromNumber : config.getPhoneNumber();
            
            // Verificar se temos um número de telefone válido
            if (fromPhoneNumber == null || fromPhoneNumber.trim().isEmpty()) {
                throw new IllegalStateException("WhatsApp phone number not configured. Please set WHATSAPP_PHONE_NUMBER environment variable.");
            }
            
            var messageCreator = Message.creator(
                new PhoneNumber(formatPhoneNumber(to)),
                new PhoneNumber(formatPhoneNumber(fromPhoneNumber)),
                caption != null ? caption : ""
            );
            
            messageCreator.setMediaUrl(java.util.Arrays.asList(java.net.URI.create(mediaUrl)));
            
            Message twilioMessage = messageCreator.create();
            
            logger.info("Media message sent successfully. SID: {}", twilioMessage.getSid());
            
            return MessageResult.success(
                twilioMessage.getSid(),
                twilioMessage.getStatus().toString(),
                getProviderName()
            );
            
        } catch (Exception e) {
            logger.error("Failed to send media message via Twilio", e);
            return MessageResult.error(e.getMessage(), getProviderName());
        }
    }
    
    @Override
    public IncomingMessage parseIncomingMessage(Object webhookPayload) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> payload = (Map<String, String>) webhookPayload;
            
            return IncomingMessage.builder()
                    .messageId(payload.get("MessageSid"))
                    .from(cleanPhoneNumber(payload.get("From")))
                    .to(cleanPhoneNumber(payload.get("To")))
                    .body(payload.get("Body"))
                    .mediaUrl(payload.get("MediaUrl0"))
                    .mediaType(payload.get("MediaContentType0"))
                    .timestamp(LocalDateTime.now())
                    .provider(getProviderName())
                    .rawPayload(payload)
                    .build();
            
        } catch (Exception e) {
            logger.error("Failed to parse Twilio webhook", e);
            throw new RuntimeException("Invalid Twilio webhook payload", e);
        }
    }
    
    @Override
    public boolean validateWebhook(Object payload, String signature) {
        return signature != null && !signature.isEmpty();
    }
    
    @Override
    public String getProviderName() {
        return "twilio";
    }
    
    String formatPhoneNumber(String phoneNumber) {
        String configPhoneNumber = config.getPhoneNumber();
        if (configPhoneNumber != null && configPhoneNumber.startsWith("whatsapp:")) {
            return phoneNumber.startsWith("whatsapp:") ? phoneNumber : "whatsapp:" + phoneNumber;
        }
        return phoneNumber;
    }
    
    String cleanPhoneNumber(String phoneNumber) {
        return phoneNumber != null ? phoneNumber.replace("whatsapp:", "") : null;
    }
}