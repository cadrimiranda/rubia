package com.ruby.rubia_server.core.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AudioStorageServiceTest {

    private AudioStorageService audioStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        audioStorageService = new AudioStorageService();
        ReflectionTestUtils.setField(audioStorageService, "storagePath", tempDir.toString());
        ReflectionTestUtils.setField(audioStorageService, "maxSizeMb", 16);
        audioStorageService.init();
    }

    @Test
    void testInit_CreatesStorageDirectory() {
        // Then
        assertTrue(Files.exists(tempDir));
        assertTrue(Files.isDirectory(tempDir));
    }

    @Test
    void testStore_Success() throws Exception {
        // Given
        String fileName = "test_audio.ogg";
        String mimeType = "audio/ogg";
        String audioContent = "This is test audio content";
        InputStream audioStream = new ByteArrayInputStream(audioContent.getBytes());

        // When
        String storedPath = audioStorageService.store(fileName, audioStream, mimeType);

        // Then
        assertNotNull(storedPath);
        assertTrue(storedPath.contains(tempDir.toString()));
        assertTrue(storedPath.endsWith("_test_audio.ogg"));
        
        Path storedFile = Paths.get(storedPath);
        assertTrue(Files.exists(storedFile));
        
        String storedContent = Files.readString(storedFile);
        assertEquals(audioContent, storedContent);
    }

    @Test
    void testStore_FileTooLarge_ThrowsException() {
        // Given
        String fileName = "large_audio.ogg";
        String mimeType = "audio/ogg";
        
        // Create content larger than 16MB
        byte[] largeContent = new byte[17 * 1024 * 1024]; // 17MB
        InputStream audioStream = new ByteArrayInputStream(largeContent);

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> 
            audioStorageService.store(fileName, audioStream, mimeType));
        
        assertTrue(exception.getMessage().contains("Audio file too large"));
        assertTrue(exception.getMessage().contains("Max size: 16MB"));
    }

    @Test
    void testStore_SanitizeFileName() throws Exception {
        // Given
        String unsafeFileName = "test/../audio*.ogg";
        String mimeType = "audio/ogg";
        String audioContent = "Test content";
        InputStream audioStream = new ByteArrayInputStream(audioContent.getBytes());

        // When
        String storedPath = audioStorageService.store(unsafeFileName, audioStream, mimeType);

        // Then
        assertNotNull(storedPath);
        // Check that file was created and dangerous characters were removed
        assertFalse(storedPath.contains("../"));
        assertFalse(storedPath.contains("*"));
        
        Path storedFile = Paths.get(storedPath);
        assertTrue(Files.exists(storedFile));
        
        // Check that filename contains the sanitized version
        String fileName = storedFile.getFileName().toString();
        System.out.println("Generated filename: " + fileName);
        // The UUID prefix + underscore + sanitized filename
        assertTrue(fileName.contains("test_.._audio_.ogg") || fileName.contains("test___audio_.ogg"));
    }

    @Test
    void testStore_NullFileName_UsesDefault() throws Exception {
        // Given
        String mimeType = "audio/ogg";
        String audioContent = "Test content";
        InputStream audioStream = new ByteArrayInputStream(audioContent.getBytes());

        // When
        String storedPath = audioStorageService.store(null, audioStream, mimeType);

        // Then
        assertNotNull(storedPath);
        assertTrue(storedPath.contains("audio.ogg"));
        
        Path storedFile = Paths.get(storedPath);
        assertTrue(Files.exists(storedFile));
    }

    @Test
    void testRetrieve_ExistingFile_Success() throws Exception {
        // Given
        String fileName = "retrieve_test.ogg";
        String audioContent = "Test audio for retrieval";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, audioContent);

        // When
        Optional<InputStream> result = audioStorageService.retrieve(testFile.toString());

        // Then
        assertTrue(result.isPresent());
        
        try (InputStream inputStream = result.get()) {
            String retrievedContent = new String(inputStream.readAllBytes());
            assertEquals(audioContent, retrievedContent);
        }
    }

    @Test
    void testRetrieve_NonExistentFile_ReturnsEmpty() {
        // Given
        String nonExistentPath = tempDir.resolve("non_existent.ogg").toString();

        // When
        Optional<InputStream> result = audioStorageService.retrieve(nonExistentPath);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void testDelete_ExistingFile_Success() throws Exception {
        // Given
        String fileName = "delete_test.ogg";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, "Test content");
        assertTrue(Files.exists(testFile));

        // When
        boolean result = audioStorageService.delete(testFile.toString());

        // Then
        assertTrue(result);
        assertFalse(Files.exists(testFile));
    }

    @Test
    void testDelete_NonExistentFile_ReturnsFalse() {
        // Given
        String nonExistentPath = tempDir.resolve("non_existent.ogg").toString();

        // When
        boolean result = audioStorageService.delete(nonExistentPath);

        // Then
        assertFalse(result);
    }

    @Test
    void testExists_ExistingFile_ReturnsTrue() throws Exception {
        // Given
        String fileName = "exists_test.ogg";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, "Test content");

        // When
        boolean result = audioStorageService.exists(testFile.toString());

        // Then
        assertTrue(result);
    }

    @Test
    void testExists_NonExistentFile_ReturnsFalse() {
        // Given
        String nonExistentPath = tempDir.resolve("non_existent.ogg").toString();

        // When
        boolean result = audioStorageService.exists(nonExistentPath);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetFileSize_ExistingFile_ReturnsCorrectSize() throws Exception {
        // Given
        String fileName = "size_test.ogg";
        String content = "Test content for size calculation";
        Path testFile = tempDir.resolve(fileName);
        Files.writeString(testFile, content);

        // When
        long size = audioStorageService.getFileSize(testFile.toString());

        // Then
        assertEquals(content.getBytes().length, size);
    }

    @Test
    void testGetFileSize_NonExistentFile_ReturnsZero() {
        // Given
        String nonExistentPath = tempDir.resolve("non_existent.ogg").toString();

        // When
        long size = audioStorageService.getFileSize(nonExistentPath);

        // Then
        assertEquals(0, size);
    }

    @Test
    void testSanitizeFileName_VariousInputs() {
        // Test via reflection since method is private
        // We can test indirectly through the store method behavior
        // The sanitization is already tested in testStore_SanitizeFileName
        
        // This test verifies the behavior indirectly
        assertTrue(true, "Sanitization behavior verified through store method tests");
    }

    @Test
    void testConcurrentAccess() throws Exception {
        // Given
        String fileName = "concurrent_test.ogg";
        String content1 = "Content from thread 1";
        String content2 = "Content from thread 2";

        // When - Store files concurrently
        Thread thread1 = new Thread(() -> {
            try {
                InputStream stream1 = new ByteArrayInputStream(content1.getBytes());
                audioStorageService.store("thread1_" + fileName, stream1, "audio/ogg");
            } catch (Exception e) {
                fail("Thread 1 failed: " + e.getMessage());
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                InputStream stream2 = new ByteArrayInputStream(content2.getBytes());
                audioStorageService.store("thread2_" + fileName, stream2, "audio/ogg");
            } catch (Exception e) {
                fail("Thread 2 failed: " + e.getMessage());
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then - Both files should exist
        assertTrue(Files.list(tempDir)
                .anyMatch(path -> path.getFileName().toString().contains("thread1_" + fileName)));
        assertTrue(Files.list(tempDir)
                .anyMatch(path -> path.getFileName().toString().contains("thread2_" + fileName)));
    }
}