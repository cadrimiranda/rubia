package com.ruby.rubia_server.core.listener;

import com.ruby.rubia_server.core.entity.ConversationLastMessage;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.event.MessageCreatedEvent;
import com.ruby.rubia_server.core.repository.ConversationLastMessageRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.service.AIDraftService;
import com.ruby.rubia_server.core.service.CqrsMetricsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedMessageListenerTest {

    @Mock
    private AIDraftService aiDraftService;
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private ConversationLastMessageRepository conversationLastMessageRepository;
    
    @Mock
    private CqrsMetricsService metricsService;

    @InjectMocks
    private UnifiedMessageListener unifiedMessageListener;

    @Test
    void handleMessageCreated_shouldUpdateConversationLastMessageAndProcessDraft() {
        // Given
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .content("Test message")
                .createdAt(now)
                .build();

        Message message = new Message();
        message.setId(messageId);
        message.setSenderType(SenderType.CUSTOMER);
        message.setContent("Test message");
        message.setMedia(null);

        ConversationLastMessage existingRecord = new ConversationLastMessage();
        
        when(conversationLastMessageRepository.findByConversationId(conversationId))
                .thenReturn(Optional.of(existingRecord));
        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(message));

        // When
        unifiedMessageListener.handleMessageCreated(event);

        // Then - Verify conversation_last_message was updated
        verify(conversationLastMessageRepository).updateLastMessage(
                eq(conversationId),
                eq(now),
                eq(messageId),
                eq("Test message")
        );
        
        // Then - Verify draft generation was attempted
        verify(aiDraftService).generateDraftResponse(eq(conversationId), eq("Test message"));
        
        // Then - Verify metrics were recorded
        verify(metricsService).recordOperationDuration(eq("UPDATE"), anyLong());
        verify(metricsService).incrementOperationCounter(eq("UPDATE"), eq("SUCCESS"));
    }

    @Test
    void handleMessageCreated_shouldSkipDraftForNonCustomerMessage() {
        // Given
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .content("Test message")
                .createdAt(now)
                .build();

        Message message = new Message();
        message.setId(messageId);
        message.setSenderType(SenderType.AGENT); // Not a customer
        message.setContent("Test message");

        ConversationLastMessage existingRecord = new ConversationLastMessage();
        
        when(conversationLastMessageRepository.findByConversationId(conversationId))
                .thenReturn(Optional.of(existingRecord));
        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(message));

        // When
        unifiedMessageListener.handleMessageCreated(event);

        // Then - Verify conversation_last_message was updated
        verify(conversationLastMessageRepository).updateLastMessage(
                eq(conversationId),
                eq(now),
                eq(messageId),
                eq("Test message")
        );
        
        // Then - Verify draft generation was NOT attempted
        verify(aiDraftService, never()).generateDraftResponse(any(), any());
    }

    @Test
    void handleMessageCreated_shouldCreateNewConversationLastMessage() {
        // Given
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        MessageCreatedEvent event = MessageCreatedEvent.builder()
                .messageId(messageId)
                .conversationId(conversationId)
                .content("Test message")
                .createdAt(now)
                .build();

        Message message = new Message();
        message.setId(messageId);
        message.setSenderType(SenderType.CUSTOMER);
        message.setContent("Test message");
        message.setMedia(null);
        
        when(conversationLastMessageRepository.findByConversationId(conversationId))
                .thenReturn(Optional.empty()); // No existing record
        when(messageRepository.findById(messageId))
                .thenReturn(Optional.of(message));

        // When
        unifiedMessageListener.handleMessageCreated(event);

        // Then - Verify new record was created
        verify(conversationLastMessageRepository).save(any(ConversationLastMessage.class));
        verify(conversationLastMessageRepository, never()).updateLastMessage(any(), any(), any(), any());
        
        // Then - Verify draft generation was attempted
        verify(aiDraftService).generateDraftResponse(eq(conversationId), eq("Test message"));
        
        // Then - Verify metrics were recorded
        verify(metricsService).recordOperationDuration(eq("INSERT"), anyLong());
        verify(metricsService).incrementOperationCounter(eq("INSERT"), eq("SUCCESS"));
    }
}