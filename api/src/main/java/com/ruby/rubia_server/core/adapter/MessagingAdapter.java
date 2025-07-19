package com.ruby.rubia_server.core.adapter;

import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;

public interface MessagingAdapter {
    
    /**
     * Envia mensagem de texto
     */
    MessageResult sendMessage(String to, String message);
    
    /**
     * Envia mensagem com mídia
     */
    MessageResult sendMediaMessage(String to, String mediaUrl, String caption);
    
    /**
     * Parse de mensagem recebida (webhook)
     */
    IncomingMessage parseIncomingMessage(Object webhookPayload);
    
    /**
     * Valida webhook (segurança)
     */
    boolean validateWebhook(Object payload, String signature);
    
    /**
     * Nome do provider
     */
    String getProviderName();
}