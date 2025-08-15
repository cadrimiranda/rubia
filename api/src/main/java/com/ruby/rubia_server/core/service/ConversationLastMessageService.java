package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.ConversationLastMessage;
import com.ruby.rubia_server.core.event.MessageCreatedEvent;
import com.ruby.rubia_server.core.repository.ConversationLastMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationLastMessageService {

    private final ConversationLastMessageRepository conversationLastMessageRepository;
    private final CqrsMetricsService metricsService;

    // DESABILITADO: Migrado para UnifiedMessageListener
    // @Order(1)
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    // @Retryable(
    //     maxAttempts = 3,
    //     backoff = @Backoff(delay = 1000, multiplier = 2),
    //     recover = "recoverFromFailedUpdate"
    // )
    public void handleMessageCreated_DISABLED(MessageCreatedEvent event) {
        long startTime = System.currentTimeMillis();
        String operation = null;
        
        try {
            log.info("Processing MessageCreatedEvent - conversationId: {}, messageId: {}", 
                    event.getConversationId(), event.getMessageId());
            
            var existingRecord = conversationLastMessageRepository
                .findByConversationId(event.getConversationId());
            
            if (existingRecord.isPresent()) {
                operation = "UPDATE";
                log.debug("Updating existing conversation_last_message record for conversation: {}", 
                         event.getConversationId());
                
                conversationLastMessageRepository.updateLastMessage(
                    event.getConversationId(),
                    event.getCreatedAt(),
                    event.getMessageId(),
                    event.getContent()
                );
                
                long duration = System.currentTimeMillis() - startTime;
                metricsService.recordOperationDuration(operation, duration);
                metricsService.incrementOperationCounter(operation, "SUCCESS");
                
                log.info("Successfully updated conversation_last_message for conversation: {} in {}ms", 
                        event.getConversationId(), duration);
            } else {
                operation = "INSERT";
                log.debug("Creating new conversation_last_message record for conversation: {}", 
                         event.getConversationId());
                
                var newRecord = ConversationLastMessage.builder()
                    .conversationId(event.getConversationId())
                    .lastMessageDate(event.getCreatedAt())
                    .lastMessageId(event.getMessageId())
                    .lastMessageContent(event.getContent())
                    .build();
                
                conversationLastMessageRepository.save(newRecord);
                
                long duration = System.currentTimeMillis() - startTime;
                metricsService.recordOperationDuration(operation, duration);
                metricsService.incrementOperationCounter(operation, "SUCCESS");
                
                log.info("Successfully created conversation_last_message for conversation: {} in {}ms", 
                        event.getConversationId(), duration);
            }
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String op = operation != null ? operation : "PROCESS";
            
            metricsService.recordOperationDuration(op, duration);
            metricsService.incrementOperationCounter(op, "FAILURE");
            metricsService.incrementErrorCounter(op, e.getClass().getSimpleName());
            
            log.error("CQRS_ERROR - Failed to {} conversation_last_message for conversation: {} after {}ms. " +
                     "MessageId: {}, Error: {}", 
                     op, 
                     event.getConversationId(), 
                     duration,
                     event.getMessageId(), 
                     e.getMessage(), e);
            
            // In a production system, you might want to publish a failure event or use a dead letter queue
            // For now, we just log the error to prevent the original transaction from failing
            throw e; // Re-throw to trigger retry mechanism
        }
    }
    
    @Recover
    public void recoverFromFailedUpdate(Exception ex, MessageCreatedEvent event) {
        metricsService.incrementOperationCounter("RECOVERY", "FAILED");
        metricsService.incrementErrorCounter("RECOVERY", "MAX_RETRIES_EXCEEDED");
        
        log.error("CQRS_RECOVERY - All retry attempts failed for conversation: {}. " +
                 "MessageId: {}. Will need manual intervention or batch reconciliation. Error: {}", 
                 event.getConversationId(), 
                 event.getMessageId(), 
                 ex.getMessage(), ex);
        
        // In a production system, you might:
        // 1. Send to a Dead Letter Queue (DLQ)
        // 2. Store in a failed_events table for later retry
        // 3. Send an alert to monitoring systems
        // 4. Increment failure metrics (already done above)
    }
}