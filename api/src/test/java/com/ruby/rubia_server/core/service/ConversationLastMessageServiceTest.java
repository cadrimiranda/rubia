package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.ConversationLastMessage;
import com.ruby.rubia_server.core.event.MessageCreatedEvent;
import com.ruby.rubia_server.core.repository.ConversationLastMessageRepository;
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
class ConversationLastMessageServiceTest {

    @Mock
    private ConversationLastMessageRepository conversationLastMessageRepository;

    @InjectMocks
    private ConversationLastMessageService conversationLastMessageService;

    @Test
    void handleMessageCreated_shouldUpdateExistingRecord() {
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

        ConversationLastMessage existingRecord = new ConversationLastMessage();
        when(conversationLastMessageRepository.findByConversationId(conversationId))
                .thenReturn(Optional.of(existingRecord));

        // When
        conversationLastMessageService.handleMessageCreated(event);

        // Then
        verify(conversationLastMessageRepository).updateLastMessage(
                eq(conversationId),
                eq(now),
                eq(messageId),
                eq("Test message")
        );
        verify(conversationLastMessageRepository, never()).save(any());
    }

    @Test
    void handleMessageCreated_shouldCreateNewRecord() {
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

        when(conversationLastMessageRepository.findByConversationId(conversationId))
                .thenReturn(Optional.empty());

        // When
        conversationLastMessageService.handleMessageCreated(event);

        // Then
        verify(conversationLastMessageRepository).save(any(ConversationLastMessage.class));
        verify(conversationLastMessageRepository, never()).updateLastMessage(any(), any(), any(), any());
    }

    @Test
    void handleMessageCreated_shouldHandleExceptionGracefully() {
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

        when(conversationLastMessageRepository.findByConversationId(conversationId))
                .thenThrow(new RuntimeException("Database error"));

        // When/Then - Should not throw exception
        conversationLastMessageService.handleMessageCreated(event);
        
        verify(conversationLastMessageRepository).findByConversationId(conversationId);
    }
}