package com.ruby.rubia_server.core.listener;

import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.entity.ConversationLastMessage;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.event.MessageCreatedEvent;
import com.ruby.rubia_server.core.repository.ConversationLastMessageRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.service.AIDraftService;
import com.ruby.rubia_server.core.service.CqrsMetricsService;
import com.ruby.rubia_server.core.service.OpenAIService;
import com.ruby.rubia_server.core.service.ConversationService;
import org.springframework.web.client.RestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final ConversationService conversationService;
    
    // Configurações do debounce
    private static final long DEBOUNCE_DELAY_SECONDS = 8; // Aguarda 3 segundos
    private static final String DEBOUNCE_KEY_PREFIX = "debounce:conversation:";
    
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
        // processAIDraft(event);
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
     * Processa geração de draft de IA com debounce para agrupar mensagens sequenciais
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
            
            // Verificar se AI auto-response está habilitada e não atingiu o limite
            Conversation conversation = message.getConversation();
            if (!conversation.getAiAutoResponseEnabled()) {
                log.debug("Skipping draft generation - AI auto-response disabled for conversation: {}", conversation.getId());
                return;
            }
            
            if (conversation.getAiMessagesUsed() >= conversation.getAiMessageLimit()) {
                log.debug("Skipping draft generation - AI message limit reached ({}/{}) for conversation: {}", 
                    conversation.getAiMessagesUsed(), conversation.getAiMessageLimit(), conversation.getId());
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
            
            // DEBOUNCE: Agendar processamento com delay para agrupar mensagens sequenciais
            scheduleDebounceProcessing(message);
            
        } catch (Exception e) {
            log.error("Error processing draft with debounce for message: {}", event.getMessageId(), e);
        }
    }
    
    /**
     * Agenda processamento com debounce para agrupar mensagens do mesmo usuário
     */
    private void scheduleDebounceProcessing(Message message) {
        UUID conversationId = message.getConversation().getId();
        String debounceKey = DEBOUNCE_KEY_PREFIX + conversationId;
        
        log.debug("⏰ Scheduling debounce processing for conversation: {} with {}s delay", 
                conversationId, DEBOUNCE_DELAY_SECONDS);
        
        // Armazenar no Redis com TTL
        redisTemplate.opsForValue().set(debounceKey, "pending", DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS);
        
        // Agendar processamento após o delay
        new Thread(() -> {
            try {
                Thread.sleep(DEBOUNCE_DELAY_SECONDS * 1000);
                
                // Verificar se ainda existe a chave (não foi cancelada por nova mensagem)
                String status = redisTemplate.opsForValue().get(debounceKey);
                if ("pending".equals(status)) {
                    // Remover chave e processar
                    redisTemplate.delete(debounceKey);
                    processGroupedMessages(conversationId);
                } else {
                    log.debug("⏰ Debounce cancelled for conversation: {} (new message received)", conversationId);
                }
                
            } catch (InterruptedException e) {
                log.debug("Debounce thread interrupted for conversation: {}", conversationId);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Incrementa contador de mensagens AI usadas para uma conversa
     */
    private void incrementAiUsageCounter(UUID conversationId, UUID companyId, String context) {
        try {
            conversationService.incrementAiMessageUsage(conversationId, companyId);
            log.debug("AI message usage incremented for {} in conversation: {}", context, conversationId);
        } catch (Exception e) {
            log.error("Failed to increment AI message usage for {} in conversation: {}", context, conversationId, e);
        }
    }
    
    /**
     * Processa mensagens agrupadas da conversa após o debounce
     */
    private void processGroupedMessages(UUID conversationId) {
        try {
            log.info("🔗 Processing grouped messages for conversation: {}", conversationId);
            
            // Buscar últimas mensagens do cliente (últimos 5 minutos)
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            List<Message> recentMessages = messageRepository
                .findRecentCustomerMessages(conversationId, fiveMinutesAgo);
            
            if (recentMessages.isEmpty()) {
                log.debug("No recent customer messages found for conversation: {}", conversationId);
                return;
            }
            
            // Combinar conteúdo das mensagens em ordem cronológica
            String combinedContent = recentMessages.stream()
                .filter(msg -> msg.getContent() != null && !msg.getContent().trim().isEmpty())
                .map(Message::getContent)
                .collect(Collectors.joining(" "));
            
            if (combinedContent.trim().isEmpty()) {
                log.debug("No text content found in recent messages for conversation: {}", conversationId);
                return;
            }
            
            log.info("📝 Combined message content ({} messages): '{}'", 
                    recentMessages.size(), combinedContent);
            
            // Gerar resposta para o conteúdo combinado
            MessageDTO draft = aiDraftService.generateDraftResponse(conversationId, combinedContent);
            
            if (draft != null) {
                log.info("✅ Successfully generated grouped response: {} for conversation: {}", 
                        draft.getId(), conversationId);
                
                // Incrementar contador de mensagens AI usadas
                Conversation conversation = recentMessages.get(0).getConversation();
                UUID companyId = conversation.getCompany().getId();
                incrementAiUsageCounter(conversationId, companyId, "grouped text response");
            } else {
                log.debug("No draft generated for grouped messages in conversation: {}", conversationId);
            }
            
        } catch (Exception e) {
            log.error("Error processing grouped messages for conversation: {}", conversationId, e);
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
            // A normalização será feita dentro do AIDraftService.generateDraftResponse()
            MessageDTO response = aiDraftService.generateDraftResponse(
                audioMessage.getConversation().getId(), 
                transcription
            );
            
            if (response != null) {
                log.info("✅ Blood center response generated automatically from audio transcription for message: {}", 
                    audioMessage.getId());
                
                // Incrementar contador de mensagens AI usadas
                UUID conversationId = audioMessage.getConversation().getId();
                UUID companyId = audioMessage.getConversation().getCompany().getId();
                incrementAiUsageCounter(conversationId, companyId, "audio response");
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