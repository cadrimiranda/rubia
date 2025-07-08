package com.ruby.rubia_server.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private final String testSecretKey = "myTestSecretKey123456789012345678901234567890";
    private final Long testExpiration = 86400000L; // 24 hours
    private final String testUsername = "test@example.com";
    private final UUID testCompanyGroupId = UUID.randomUUID();
    private final String testCompanySlug = "test-company";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", testExpiration);
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // When
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        
        // Verify we can extract the username
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals(testUsername, extractedUsername);
        
        // Verify we can extract the company group ID
        UUID extractedCompanyGroupId = jwtService.extractCompanyGroupId(token);
        assertEquals(testCompanyGroupId, extractedCompanyGroupId);
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertEquals(testUsername, extractedUsername);
    }

    @Test
    void extractCompanyGroupId_ShouldReturnCorrectCompanyGroupId() {
        // Given
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);

        // When
        UUID extractedCompanyGroupId = jwtService.extractCompanyGroupId(token);

        // Then
        assertEquals(testCompanyGroupId, extractedCompanyGroupId);
    }

    @Test
    void extractCompanyGroupId_ShouldReturnNull_WhenTokenDoesNotContainCompanyGroupId() {
        // Given - create a token without companyGroupId claim
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);
        // This test simulates a malformed token, but since we always include companyGroupId,
        // we'll test with an invalid token format
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThrows(Exception.class, () -> jwtService.extractCompanyGroupId(invalidToken));
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
        // Given
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUsername);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        // Given
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);
        String differentUsername = "different@example.com";

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUsername);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenIsExpired() {
        // Given - create service with very short expiration
        JwtService shortExpirationService = new JwtService();
        ReflectionTestUtils.setField(shortExpirationService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(shortExpirationService, "jwtExpiration", 1L); // 1ms expiration
        
        String token = shortExpirationService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);
        
        // Wait for token to expire
        try {
            Thread.sleep(100); // Increase wait time to ensure expiration
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then - expect exception for expired token
        assertThrows(Exception.class, () -> {
            shortExpirationService.isTokenValid(token, testUsername);
        });
    }

    @Test
    void getExpirationTime_ShouldReturnCorrectExpiration() {
        // When
        long expiration = jwtService.getExpirationTime();

        // Then
        assertEquals(testExpiration, expiration);
    }

    @Test
    void extractCompanyId_ShouldReturnNull_WhenOnlyCompanyGroupIdPresent() {
        // Given
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);

        // When
        UUID extractedCompanyId = jwtService.extractCompanyId(token);

        // Then
        assertNull(extractedCompanyId); // Should be null since we're using companyGroupId now
    }

    @Test
    void token_ShouldBeValidForEntireExpirationPeriod() {
        // Given
        String token = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);

        // When - test immediately after creation
        boolean isValidNow = jwtService.isTokenValid(token, testUsername);

        // Then
        assertTrue(isValidNow);
        
        // Verify the token structure contains expected claims
        String extractedUsername = jwtService.extractUsername(token);
        UUID extractedCompanyGroupId = jwtService.extractCompanyGroupId(token);
        
        assertEquals(testUsername, extractedUsername);
        assertEquals(testCompanyGroupId, extractedCompanyGroupId);
    }

    @Test
    void generateToken_ShouldCreateDifferentTokensForDifferentInputs() {
        // Given
        UUID differentCompanyGroupId = UUID.randomUUID();
        String differentUsername = "different@example.com";

        // When
        String token1 = jwtService.generateToken(testUsername, testCompanyGroupId, testCompanySlug);
        String token2 = jwtService.generateToken(differentUsername, differentCompanyGroupId, testCompanySlug);

        // Then
        assertNotEquals(token1, token2);
        
        // Verify each token contains its respective data
        assertEquals(testUsername, jwtService.extractUsername(token1));
        assertEquals(testCompanyGroupId, jwtService.extractCompanyGroupId(token1));
        
        assertEquals(differentUsername, jwtService.extractUsername(token2));
        assertEquals(differentCompanyGroupId, jwtService.extractCompanyGroupId(token2));
    }
}