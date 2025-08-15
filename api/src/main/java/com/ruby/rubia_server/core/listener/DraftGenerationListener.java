package com.ruby.rubia_server.core.listener;

import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.event.MessageCreatedEvent;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.service.AIDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener que monitora novas mensagens e gera drafts automaticamente
 * quando mensagens de clientes são recebidas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DraftGenerationListener {
    
    private final AIDraftService aiDraftService;
    private final MessageRepository messageRepository;
    
    /**
     * DESABILITADO: Migrado para UnifiedMessageListener
     */
    // @Order(2)
    // @Async
    // @EventListener
    public void handleMessageCreated_DISABLED(MessageCreatedEvent event) {
        try {
            // Busca a mensagem completa pelo ID
            Message message = messageRepository.findById(event.getMessageId())
                .orElse(null);
                
            if (message == null) {
                log.warn("Message not found for draft generation: {}", event.getMessageId());
                return;
            }
            
            // Só gera draft para mensagens de clientes (não operadores)
            if (!SenderType.CUSTOMER.equals(message.getSenderType())) {
                log.debug("Skipping draft generation for non-customer message: {}", message.getId());
                return;
            }
            
            // Só gera draft para mensagens de texto (sem mídia)
            if (message.getMedia() != null) {
                log.debug("Skipping draft generation for media message: {}", message.getId());
                return;
            }
            
            // Só gera draft para mensagens com conteúdo de texto
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                log.debug("Skipping draft generation for message without text content: {}", message.getId());
                return;
            }
            
            log.info("Generating draft for new message: {} in conversation: {}", 
                message.getId(), message.getConversation().getId());
            
            // Gera draft automaticamente
            MessageDTO draft = aiDraftService.generateDraftResponse(
                message.getConversation().getId(), 
                message.getContent()
            );
            
            if (draft != null) {
                log.info("Successfully generated draft: {} for message: {}", draft.getId(), message.getId());
                
                // TODO: Notificar operadores via WebSocket
                // websocketService.notifyNewDraft(draft);
                
            } else {
                log.debug("No draft generated for message: {}", message.getId());
            }
            
        } catch (Exception e) {
            log.error("Error generating draft for message: {}", event.getMessageId(), e);
        }
    }
    
}