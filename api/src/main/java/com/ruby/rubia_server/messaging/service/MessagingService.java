package com.ruby.rubia_server.messaging.service;

import com.ruby.rubia_server.messaging.adapter.MessagingAdapter;
import com.ruby.rubia_server.messaging.adapter.impl.TwilioAdapter;
import com.ruby.rubia_server.messaging.model.MessageResult;
import com.ruby.rubia_server.messaging.model.IncomingMessage;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class MessagingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    
    private final List<MessagingAdapter> adapters;
    private MessagingAdapter currentAdapter;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    public MessagingService(List<MessagingAdapter> adapters) {
        this.adapters = adapters;
        this.currentAdapter = adapters.isEmpty() ? null : adapters.get(0);
        
        if (currentAdapter != null) {
            logger.info("Initialized with adapter: {}", currentAdapter.getProviderName());
        }
    }
    
    public MessageResult sendMessage(String to, String message) {
        return sendMessage(to, message, null, null);
    }
    
    public MessageResult sendMessage(String to, String message, UUID companyId, UUID userId) {
        if (currentAdapter == null) {
            logger.error("No messaging adapter configured");
            return MessageResult.error("No adapter available", "none");
        }
        
        String fromNumber = null;
        if (userId != null && companyId != null) {
            fromNumber = getUserWhatsappNumber(userId, companyId);
        }
        
        if (currentAdapter instanceof TwilioAdapter twilioAdapter) {
            return twilioAdapter.sendMessage(to, message, fromNumber);
        }
        
        return currentAdapter.sendMessage(to, message);
    }
    
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        return sendMediaMessage(to, mediaUrl, caption, null, null);
    }
    
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption, UUID companyId, UUID userId) {
        if (currentAdapter == null) {
            logger.error("No messaging adapter configured");
            return MessageResult.error("No adapter available", "none");
        }
        
        String fromNumber = null;
        if (userId != null && companyId != null) {
            fromNumber = getUserWhatsappNumber(userId, companyId);
        }
        
        if (currentAdapter instanceof TwilioAdapter twilioAdapter) {
            return twilioAdapter.sendMediaMessage(to, mediaUrl, caption, fromNumber);
        }
        
        return currentAdapter.sendMediaMessage(to, mediaUrl, caption);
    }
    
    public IncomingMessage parseIncomingMessage(Object webhookPayload) {
        if (currentAdapter == null) {
            throw new RuntimeException("No messaging adapter configured");
        }
        
        return currentAdapter.parseIncomingMessage(webhookPayload);
    }
    
    public boolean validateWebhook(Object payload, String signature) {
        if (currentAdapter == null) {
            return false;
        }
        
        return currentAdapter.validateWebhook(payload, signature);
    }
    
    public void switchAdapter(String providerName) {
        MessagingAdapter newAdapter = adapters.stream()
            .filter(adapter -> adapter.getProviderName().equals(providerName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Adapter not found: " + providerName));
        
        this.currentAdapter = newAdapter;
        logger.info("Switched to adapter: {}", providerName);
    }
    
    public String getCurrentProvider() {
        return currentAdapter != null ? currentAdapter.getProviderName() : "none";
    }
    
    public List<String> getAvailableProviders() {
        return adapters.stream()
            .map(MessagingAdapter::getProviderName)
            .toList();
    }
    
    public User findUserByWhatsappNumber(String whatsappNumber, UUID companyId) {
        return userRepository.findByWhatsappNumberAndCompanyId(whatsappNumber, companyId)
                .orElse(null);
    }
    
    private String getUserWhatsappNumber(UUID userId, UUID companyId) {
        return userRepository.findById(userId)
                .filter(user -> user.getCompany().getId().equals(companyId))
                .filter(User::getIsWhatsappActive)
                .map(User::getWhatsappNumber)
                .orElse(null);
    }
}