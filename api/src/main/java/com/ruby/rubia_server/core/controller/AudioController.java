package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.AudioMessage;
import com.ruby.rubia_server.core.repository.AudioMessageRepository;
import com.ruby.rubia_server.core.service.AudioProcessingService;
import com.ruby.rubia_server.core.service.AudioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/audio")
@RequiredArgsConstructor
public class AudioController {

    private final AudioProcessingService audioProcessingService;
    private final AudioMessageRepository audioMessageRepository;
    private final AudioStorageService audioStorageService;

    @PostMapping("/send")
    public ResponseEntity<?> sendAudio(@RequestBody Map<String, String> request) {
        try {
            String toNumber = request.get("toNumber");
            String audioUrl = request.get("audioUrl");

            if (toNumber == null || audioUrl == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required parameters: toNumber and audioUrl"
                ));
            }

            String messageId = audioProcessingService.sendAudio(toNumber, audioUrl);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "messageId", messageId
            ));

        } catch (Exception e) {
            log.error("Error sending audio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<List<AudioMessage>> listMessages(
            @RequestParam(required = false) String fromNumber,
            @RequestParam(required = false) AudioMessage.ProcessingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AudioMessage> messagePage;

        if (fromNumber != null) {
            messagePage = audioMessageRepository.findAll(pageRequest);
            List<AudioMessage> filteredMessages = messagePage.getContent().stream()
                    .filter(msg -> fromNumber.equals(msg.getFromNumber()))
                    .toList();
            return ResponseEntity.ok(filteredMessages);
        } else if (status != null) {
            List<AudioMessage> messages = audioMessageRepository.findByStatus(status);
            return ResponseEntity.ok(messages);
        } else {
            messagePage = audioMessageRepository.findAll(pageRequest);
            return ResponseEntity.ok(messagePage.getContent());
        }
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<?> getMessage(@PathVariable UUID id) {
        return audioMessageRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/messages/{id}/download")
    public ResponseEntity<?> downloadAudio(@PathVariable UUID id) {
        try {
            Optional<AudioMessage> audioMessageOpt = audioMessageRepository.findById(id);
            if (audioMessageOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            AudioMessage audioMessage = audioMessageOpt.get();
            if (audioMessage.getFilePath() == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Audio file not available"
                ));
            }

            Optional<InputStream> audioStream = audioStorageService.retrieve(audioMessage.getFilePath());
            if (audioStream.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String fileName = audioMessage.getMessageId() + ".ogg";
            String mimeType = audioMessage.getMimeType() != null ? 
                audioMessage.getMimeType() : "audio/ogg";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(new InputStreamResource(audioStream.get()));

        } catch (Exception e) {
            log.error("Error downloading audio: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to download audio file"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalMessages = audioMessageRepository.count();
        long receivedCount = audioMessageRepository.countByStatus(AudioMessage.ProcessingStatus.RECEIVED);
        long processingCount = audioMessageRepository.countByStatus(AudioMessage.ProcessingStatus.DOWNLOADING) +
                              audioMessageRepository.countByStatus(AudioMessage.ProcessingStatus.PROCESSING);
        long completedCount = audioMessageRepository.countByStatus(AudioMessage.ProcessingStatus.COMPLETED);
        long failedCount = audioMessageRepository.countByStatus(AudioMessage.ProcessingStatus.FAILED);

        return ResponseEntity.ok(Map.of(
            "total", totalMessages,
            "received", receivedCount,
            "processing", processingCount,
            "completed", completedCount,
            "failed", failedCount
        ));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<?> deleteAudioMessage(@PathVariable UUID id) {
        try {
            Optional<AudioMessage> audioMessageOpt = audioMessageRepository.findById(id);
            if (audioMessageOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            AudioMessage audioMessage = audioMessageOpt.get();
            
            // Delete file if exists
            if (audioMessage.getFilePath() != null) {
                audioStorageService.delete(audioMessage.getFilePath());
            }

            // Delete database record
            audioMessageRepository.delete(audioMessage);

            return ResponseEntity.ok(Map.of("status", "deleted"));

        } catch (Exception e) {
            log.error("Error deleting audio message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete audio message"));
        }
    }
}