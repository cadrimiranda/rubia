package com.ruby.rubia_server.core.listener;

import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.entity.ConversationLastMessage;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.event.MessageCreatedEvent;
import com.ruby.rubia_server.core.repository.ConversationLastMessageRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.service.AIDraftService;
import com.ruby.rubia_server.core.service.CqrsMetricsService;
import com.ruby.rubia_server.core.service.OpenAIService;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener unificado que:
 * 1. Atualiza conversation_last_message (CQRS)
 * 2. Gera drafts de IA para mensagens de clientes
 * Elimina race condition processando tudo em sequência
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedMessageListener {
    
    private final AIDraftService aiDraftService;
    private final MessageRepository messageRepository;
    private final ConversationLastMessageRepository conversationLastMessageRepository;
    private final CqrsMetricsService metricsService;
    private final OpenAIService openAIService;
    private final RestTemplate restTemplate;
    
    /**
     * Processa MessageCreatedEvent de forma unificada:
     * 1. Atualiza conversation_last_message 
     * 2. Gera draft de IA se aplicável
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2),
        recover = "recoverFromFailedProcessing"
    )
    public void handleMessageCreated(MessageCreatedEvent event) {
        log.info("Processing MessageCreatedEvent (UNIFIED) - conversationId: {}, messageId: {}", 
                event.getConversationId(), event.getMessageId());
        
        // 1. PRIMEIRO: Atualizar conversation_last_message (CQRS)
        updateConversationLastMessage(event);
        
        // 2. SEGUNDO: Processar draft de IA se aplicável
        processAIDraft(event);
    }
    
    /**
     * Atualiza tabela conversation_last_message (CQRS view) usando UPSERT atômico
     */
    private void updateConversationLastMessage(MessageCreatedEvent event) {
        long startTime = System.currentTimeMillis();
        String operation = "UPSERT";
        
        try {
            log.debug("Upserting conversation_last_message - conversationId: {}, messageId: {}", 
                    event.getConversationId(), event.getMessageId());
            
            // UPSERT atômico - elimina race condition
            conversationLastMessageRepository.upsertLastMessage(
                event.getConversationId(),
                event.getCreatedAt(),
                event.getMessageId(),
                event.getContent()
            );
            
            long duration = System.currentTimeMillis() - startTime;
            metricsService.recordOperationDuration(operation, duration);
            metricsService.incrementOperationCounter(operation, "SUCCESS");
            
            log.debug("Successfully upserted conversation_last_message for conversation: {} in {}ms", 
                    event.getConversationId(), duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            
            metricsService.recordOperationDuration(operation, duration);
            metricsService.incrementOperationCounter(operation, "FAILURE");
            metricsService.incrementErrorCounter(operation, e.getClass().getSimpleName());
            
            log.error("CQRS_ERROR - Failed to {} conversation_last_message for conversation: {} after {}ms. " +
                     "MessageId: {}, Error: {}", 
                     operation, 
                     event.getConversationId(), 
                     duration,
                     event.getMessageId(), 
                     e.getMessage(), e);
            
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    /**
     * Processa geração de draft de IA
     */
    private void processAIDraft(MessageCreatedEvent event) {
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
            
            // NOVO: Detectar se é mensagem de áudio
            if (message.getMedia() != null && isAudioMessage(message)) {
                log.info("🎤 Processing audio message for blood center: {}", message.getId());
                processAudioForBloodCenter(message);
                return;
            }
            
            // Só gera draft para mensagens de texto (sem mídia)
            if (message.getMedia() != null) {
                log.debug("Skipping draft generation for non-audio media message: {}", message.getId());
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
                
            } else {
                log.debug("No draft generated for message: {}", message.getId());
            }
            
        } catch (Exception e) {
            log.error("Error generating draft for message: {}", event.getMessageId(), e);
        }
    }
    
    /**
     * Detecta se a mensagem contém áudio
     */
    private boolean isAudioMessage(Message message) {
        if (message.getMedia() == null || message.getMedia().getMimeType() == null) {
            return false;
        }
        
        String mimeType = message.getMedia().getMimeType().toLowerCase();
        return mimeType.startsWith("audio/") || 
               mimeType.equals("application/ogg") || 
               mimeType.contains("voice") ||
               mimeType.contains("opus");
    }
    
    /**
     * Processa áudio para gerar resposta automática do hemocentro
     */
    private void processAudioForBloodCenter(Message audioMessage) {
        try {
            log.info("🎤 Processing audio message for blood center: {}", audioMessage.getId());
            
            // 1. Baixar áudio da URL
            String audioUrl = audioMessage.getMedia().getFileUrl();
            if (audioUrl == null || audioUrl.trim().isEmpty()) {
                log.warn("Audio URL is empty for message: {}", audioMessage.getId());
                return;
            }
            
            byte[] audioData = downloadAudio(audioUrl);
            if (audioData == null || audioData.length == 0) {
                log.warn("Failed to download audio for message: {}", audioMessage.getId());
                return;
            }
            
            // 2. Transcrever com OpenAI Whisper
            String transcription = openAIService.transcribeAudio(audioData, "pt");
            if (transcription == null || transcription.trim().isEmpty()) {
                log.warn("Failed to transcribe audio for message: {}", audioMessage.getId());
                return;
            }
            
            log.info("🗣️ Audio transcribed: '{}' for message: {}", transcription, audioMessage.getId());
            
            // 3. Processar texto transcrito com IA do hemocentro
            MessageDTO response = aiDraftService.generateDraftResponse(
                audioMessage.getConversation().getId(), 
                transcription
            );
            
            if (response != null) {
                log.info("✅ Blood center response generated automatically from audio transcription for message: {}", 
                    audioMessage.getId());
            } else {
                log.debug("No blood center response generated from audio for message: {}", audioMessage.getId());
            }
            
        } catch (Exception e) {
            log.error("Error processing audio for blood center: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Baixa o arquivo de áudio da URL
     */
    private byte[] downloadAudio(String audioUrl) {
        try {
            log.debug("Downloading audio from URL: {}", audioUrl);
            
            byte[] audioData = restTemplate.getForObject(audioUrl, byte[].class);
            
            if (audioData != null) {
                log.debug("Audio downloaded successfully: {} bytes", audioData.length);
                return audioData;
            } else {
                log.warn("Audio download returned null data");
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error downloading audio from URL: {}: {}", audioUrl, e.getMessage(), e);
            return null;
        }
    }
    
    @Recover
    public void recoverFromFailedProcessing(Exception ex, MessageCreatedEvent event) {
        metricsService.incrementOperationCounter("RECOVERY", "FAILED");
        metricsService.incrementErrorCounter("RECOVERY", "MAX_RETRIES_EXCEEDED");
        
        log.error("UNIFIED_LISTENER_RECOVERY - All retry attempts failed for conversation: {}. " +
                 "MessageId: {}. Will need manual intervention or batch reconciliation. Error: {}", 
                 event.getConversationId(), 
                 event.getMessageId(), 
                 ex.getMessage(), ex);
    }
}