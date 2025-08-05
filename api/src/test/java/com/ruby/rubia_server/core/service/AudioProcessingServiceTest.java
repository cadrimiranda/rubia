package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.adapter.impl.ZApiAdapter;
import com.ruby.rubia_server.core.entity.AudioMessage;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.repository.AudioMessageRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AudioProcessingServiceTest {

    @Mock
    private AudioMessageRepository audioMessageRepository;

    @Mock
    private AudioStorageService audioStorageService;

    @Mock
    private ZApiAdapter zApiAdapter;

    @Mock
    private ConversationRepository conversationRepository;

    @InjectMocks
    private AudioProcessingService audioProcessingService;

    private String messageId;
    private String fromNumber;
    private String toNumber;
    private String audioUrl;
    private String mimeType;
    private Integer durationSeconds;

    @BeforeEach
    void setUp() {
        messageId = "test_message_id_123";
        fromNumber = "554891095462";
        toNumber = "554891208536";
        audioUrl = "https://example.com/audio.ogg";
        mimeType = "audio/ogg";
        durationSeconds = 10;
    }

    @Test
    void testProcessIncomingAudio_NewMessage_Success() throws Exception {
        // Given
        when(audioMessageRepository.existsByMessageId(messageId)).thenReturn(false);
        
        AudioMessage savedMessage = AudioMessage.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .fromNumber(fromNumber)
                .direction(AudioMessage.MessageDirection.INCOMING)
                .audioUrl(audioUrl)
                .mimeType(mimeType)
                .durationSeconds(durationSeconds)
                .status(AudioMessage.ProcessingStatus.RECEIVED)
                .build();
                
        when(audioMessageRepository.save(any(AudioMessage.class))).thenReturn(savedMessage);
        
        // Mock async processing dependencies
        InputStream mockAudioStream = new ByteArrayInputStream("audio content".getBytes());
        when(zApiAdapter.downloadAudio(audioUrl)).thenReturn(mockAudioStream);
        when(audioStorageService.store(anyString(), any(InputStream.class), eq(mimeType)))
                .thenReturn("/path/to/file.ogg");
        when(audioStorageService.getFileSize(anyString())).thenReturn(1024L);

        // When
        AudioMessage result = audioProcessingService.processIncomingAudio(
                messageId, fromNumber, audioUrl, mimeType, durationSeconds);

        // Allow async processing to complete
        Thread.sleep(200);

        // Then
        assertNotNull(result);
        assertEquals(messageId, result.getMessageId());
        assertEquals(fromNumber, result.getFromNumber());
        assertEquals(AudioMessage.MessageDirection.INCOMING, result.getDirection());
        assertEquals(audioUrl, result.getAudioUrl());
        assertEquals(mimeType, result.getMimeType());
        assertEquals(durationSeconds, result.getDurationSeconds());
        // Status may change due to async processing, so check initial save was called
        
        verify(audioMessageRepository).existsByMessageId(messageId);
        verify(audioMessageRepository, atLeast(1)).save(any(AudioMessage.class));
    }

    @Test
    void testProcessIncomingAudio_DuplicateMessage_ReturnsExisting() {
        // Given
        AudioMessage existingMessage = AudioMessage.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .fromNumber(fromNumber)
                .status(AudioMessage.ProcessingStatus.COMPLETED)
                .build();

        when(audioMessageRepository.existsByMessageId(messageId)).thenReturn(true);
        when(audioMessageRepository.findByMessageId(messageId)).thenReturn(Optional.of(existingMessage));

        // When
        AudioMessage result = audioProcessingService.processIncomingAudio(
                messageId, fromNumber, audioUrl, mimeType, durationSeconds);

        // Then
        assertNotNull(result);
        assertEquals(existingMessage.getId(), result.getId());
        assertEquals(AudioMessage.ProcessingStatus.COMPLETED, result.getStatus());

        verify(audioMessageRepository).existsByMessageId(messageId);
        verify(audioMessageRepository).findByMessageId(messageId);
        verify(audioMessageRepository, never()).save(any(AudioMessage.class));
    }

    @Test
    void testSendAudio_Success() {
        // Given
        MessageResult mockResult = new MessageResult();
        mockResult.setSuccess(true);
        mockResult.setMessageId("sent_message_123");

        when(zApiAdapter.sendAudio(toNumber, audioUrl)).thenReturn(mockResult);
        
        AudioMessage savedMessage = AudioMessage.builder()
                .id(UUID.randomUUID())
                .messageId("sent_message_123")
                .toNumber(toNumber)
                .direction(AudioMessage.MessageDirection.OUTGOING)
                .audioUrl(audioUrl)
                .status(AudioMessage.ProcessingStatus.RECEIVED)
                .build();
                
        when(audioMessageRepository.save(any(AudioMessage.class))).thenReturn(savedMessage);

        // When
        String result = audioProcessingService.sendAudio(toNumber, audioUrl);

        // Then
        assertEquals("sent_message_123", result);

        verify(zApiAdapter).sendAudio(toNumber, audioUrl);
        verify(audioMessageRepository).save(argThat(audioMessage -> 
            audioMessage.getMessageId().equals("sent_message_123") &&
            audioMessage.getToNumber().equals(toNumber) &&
            audioMessage.getDirection() == AudioMessage.MessageDirection.OUTGOING &&
            audioMessage.getAudioUrl().equals(audioUrl) &&
            audioMessage.getStatus() == AudioMessage.ProcessingStatus.RECEIVED
        ));
    }

    @Test
    void testSendAudio_ZApiFailure_ThrowsException() {
        // Given
        MessageResult mockResult = new MessageResult();
        mockResult.setSuccess(false);
        mockResult.setError("Network error");

        when(zApiAdapter.sendAudio(toNumber, audioUrl)).thenReturn(mockResult);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            audioProcessingService.sendAudio(toNumber, audioUrl));

        assertEquals("Failed to send audio", exception.getMessage());
        assertTrue(exception.getCause().getMessage().contains("Failed to send audio: Network error"));

        verify(zApiAdapter).sendAudio(toNumber, audioUrl);
        verify(audioMessageRepository, never()).save(any(AudioMessage.class));
    }

    @Test
    void testSendAudio_ZApiException_ThrowsException() {
        // Given
        when(zApiAdapter.sendAudio(toNumber, audioUrl)).thenThrow(new RuntimeException("Connection timeout"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            audioProcessingService.sendAudio(toNumber, audioUrl));

        assertEquals("Failed to send audio", exception.getMessage());

        verify(zApiAdapter).sendAudio(toNumber, audioUrl);
        verify(audioMessageRepository, never()).save(any(AudioMessage.class));
    }

    @Test
    void testProcessAudioAsync_Success() throws Exception {
        // Given
        AudioMessage audioMessage = AudioMessage.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .audioUrl(audioUrl)
                .mimeType(mimeType)
                .status(AudioMessage.ProcessingStatus.RECEIVED)
                .build();

        InputStream mockAudioStream = new ByteArrayInputStream("audio content".getBytes());
        String filePath = "/path/to/audio/file.ogg";
        long fileSize = 1024L;

        when(zApiAdapter.downloadAudio(audioUrl)).thenReturn(mockAudioStream);
        when(audioStorageService.store(anyString(), any(InputStream.class), eq(mimeType)))
                .thenReturn(filePath);
        when(audioStorageService.getFileSize(filePath)).thenReturn(fileSize);

        // When
        audioProcessingService.processAudioAsync(audioMessage);

        // Allow some time for async processing
        Thread.sleep(100);

        // Then
        verify(zApiAdapter).downloadAudio(audioUrl);
        verify(audioStorageService).store(eq(messageId + ".ogg"), any(InputStream.class), eq(mimeType));
        verify(audioStorageService).getFileSize(filePath);
        verify(audioMessageRepository, atLeast(2)).save(audioMessage);

        // Verify status progression
        assertEquals(AudioMessage.ProcessingStatus.COMPLETED, audioMessage.getStatus());
        assertEquals(filePath, audioMessage.getFilePath());
        assertEquals(fileSize, audioMessage.getFileSizeBytes());
        assertNotNull(audioMessage.getProcessedAt());
    }

    @Test
    void testProcessAudioAsync_Failure() throws Exception {
        // Given
        AudioMessage audioMessage = AudioMessage.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .audioUrl(audioUrl)
                .mimeType(mimeType)
                .status(AudioMessage.ProcessingStatus.RECEIVED)
                .build();

        when(zApiAdapter.downloadAudio(audioUrl)).thenThrow(new RuntimeException("Download failed"));

        // When
        audioProcessingService.processAudioAsync(audioMessage);

        // Allow some time for async processing
        Thread.sleep(100);

        // Then
        verify(zApiAdapter).downloadAudio(audioUrl);
        verify(audioMessageRepository, atLeast(2)).save(audioMessage);

        // Verify failure status
        assertEquals(AudioMessage.ProcessingStatus.FAILED, audioMessage.getStatus());
        assertEquals("Download failed", audioMessage.getErrorMessage());
    }

    @Test
    void testHasAudioMessage() {
        // Given
        when(audioMessageRepository.existsByMessageId(messageId)).thenReturn(true);

        // When
        boolean result = audioProcessingService.hasAudioMessage(messageId);

        // Then
        assertTrue(result);
        verify(audioMessageRepository).existsByMessageId(messageId);
    }

    @Test
    void testGetAudioMessage_Found() {
        // Given
        AudioMessage audioMessage = AudioMessage.builder()
                .id(UUID.randomUUID())
                .messageId(messageId)
                .status(AudioMessage.ProcessingStatus.COMPLETED)
                .build();

        when(audioMessageRepository.findByMessageId(messageId)).thenReturn(Optional.of(audioMessage));

        // When
        AudioMessage result = audioProcessingService.getAudioMessage(messageId);

        // Then
        assertNotNull(result);
        assertEquals(messageId, result.getMessageId());
        verify(audioMessageRepository).findByMessageId(messageId);
    }

    @Test
    void testGetAudioMessage_NotFound() {
        // Given
        when(audioMessageRepository.findByMessageId(messageId)).thenReturn(Optional.empty());

        // When
        AudioMessage result = audioProcessingService.getAudioMessage(messageId);

        // Then
        assertNull(result);
        verify(audioMessageRepository).findByMessageId(messageId);
    }
}