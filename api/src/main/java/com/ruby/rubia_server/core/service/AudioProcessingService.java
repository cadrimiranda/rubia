package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.adapter.impl.ZApiAdapter;
import com.ruby.rubia_server.core.entity.AudioMessage;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.repository.AudioMessageRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioProcessingService {

    private final AudioMessageRepository audioMessageRepository;
    private final AudioStorageService audioStorageService;
    private final ZApiAdapter zApiAdapter;
    private final ConversationRepository conversationRepository;

    @Transactional
    public AudioMessage processIncomingAudio(String messageId, String fromNumber, 
                                           String audioUrl, String mimeType, Integer durationSeconds) {
        
        if (audioMessageRepository.existsByMessageId(messageId)) {
            log.info("Audio message already processed: {}", messageId);
            return audioMessageRepository.findByMessageId(messageId).orElse(null);
        }

        AudioMessage audioMessage = AudioMessage.builder()
                .messageId(messageId)
                .fromNumber(fromNumber)
                .direction(AudioMessage.MessageDirection.INCOMING)
                .audioUrl(audioUrl)
                .mimeType(mimeType)
                .durationSeconds(durationSeconds)
                .status(AudioMessage.ProcessingStatus.RECEIVED)
                .build();

        audioMessage = audioMessageRepository.save(audioMessage);
        log.info("Audio message saved: {} from {}", messageId, fromNumber);

        processAudioAsync(audioMessage);

        return audioMessage;
    }

    @Async
    public void processAudioAsync(AudioMessage audioMessage) {
        try {
            log.info("Starting async processing for audio: {}", audioMessage.getMessageId());
            
            audioMessage.setStatus(AudioMessage.ProcessingStatus.DOWNLOADING);
            audioMessageRepository.save(audioMessage);

            InputStream audioStream = zApiAdapter.downloadAudio(audioMessage.getAudioUrl());
            
            String fileName = audioMessage.getMessageId() + ".ogg";
            String filePath = audioStorageService.store(fileName, audioStream, audioMessage.getMimeType());
            
            long fileSize = audioStorageService.getFileSize(filePath);
            
            audioMessage.setFilePath(filePath);
            audioMessage.setFileSizeBytes(fileSize);
            audioMessage.setStatus(AudioMessage.ProcessingStatus.COMPLETED);
            audioMessage.setProcessedAt(LocalDateTime.now());
            
            log.info("Audio processing completed: {} ({}bytes)", audioMessage.getMessageId(), fileSize);

        } catch (Exception e) {
            log.error("Error processing audio {}: {}", audioMessage.getMessageId(), e.getMessage());
            audioMessage.setStatus(AudioMessage.ProcessingStatus.FAILED);
            audioMessage.setErrorMessage(e.getMessage());
        }

        audioMessageRepository.save(audioMessage);
    }

    @Transactional
    public String sendAudio(String toNumber, String audioUrl) {
        try {
            var result = zApiAdapter.sendAudio(toNumber, audioUrl);
            
            if (result.isSuccess()) {
                AudioMessage audioMessage = AudioMessage.builder()
                        .messageId(result.getMessageId())
                        .toNumber(toNumber)
                        .direction(AudioMessage.MessageDirection.OUTGOING)
                        .audioUrl(audioUrl)
                        .status(AudioMessage.ProcessingStatus.COMPLETED)
                        .processedAt(LocalDateTime.now())
                        .build();

                audioMessageRepository.save(audioMessage);
                log.info("Outgoing audio message saved: {}", result.getMessageId());
                
                return result.getMessageId();
            } else {
                throw new RuntimeException("Failed to send audio: " + result.getError());
            }

        } catch (Exception e) {
            log.error("Error sending audio to {}: {}", toNumber, e.getMessage());
            throw new RuntimeException("Failed to send audio", e);
        }
    }

    public boolean hasAudioMessage(String messageId) {
        return audioMessageRepository.existsByMessageId(messageId);
    }

    @Transactional(readOnly = true)
    public AudioMessage getAudioMessage(String messageId) {
        return audioMessageRepository.findByMessageId(messageId).orElse(null);
    }
}