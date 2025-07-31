package com.ruby.rubia_server.core.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AudioStorageService {

    @Value("${audio.storage.path:${user.home}/rubia/audio}")
    private String storagePath;

    @Value("${audio.storage.max-size-mb:16}")
    private int maxSizeMb;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storagePath));
            log.info("Audio storage directory created: {}", storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create audio storage directory", e);
        }
    }

    public String store(String fileName, InputStream audioStream, String mimeType) throws Exception {
        String uniqueFileName = UUID.randomUUID() + "_" + sanitizeFileName(fileName);
        Path filePath = Paths.get(storagePath, uniqueFileName);

        try {
            Files.copy(audioStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            long fileSize = Files.size(filePath);
            
            if (fileSize > maxSizeMb * 1024 * 1024) {
                Files.deleteIfExists(filePath);
                throw new Exception("Audio file too large. Max size: " + maxSizeMb + "MB");
            }
            
            log.info("Audio file saved: {} ({}bytes)", filePath, fileSize);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Error saving audio file: {}", e.getMessage());
            throw new Exception("Failed to save audio file", e);
        } finally {
            IOUtils.closeQuietly(audioStream);
        }
    }

    public Optional<InputStream> retrieve(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                return Optional.of(new BufferedInputStream(Files.newInputStream(path)));
            }
        } catch (IOException e) {
            log.error("Error retrieving file: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public boolean delete(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        }
    }

    public boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.error("Error getting file size: {}", e.getMessage());
            return 0;
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "audio.ogg";
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}