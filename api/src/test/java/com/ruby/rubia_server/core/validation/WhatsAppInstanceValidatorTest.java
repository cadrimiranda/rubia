package com.ruby.rubia_server.core.validation;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppInstanceValidatorTest {

    private WhatsAppInstanceValidator validator;
    private Company mockCompany;

    @BeforeEach
    void setUp() {
        validator = new WhatsAppInstanceValidator();
        mockCompany = Company.builder()
            .id(UUID.randomUUID())
            .name("Test Company")
            .build();
    }

    @Test
    void validateInstanceConfiguration_WithValidInstance_ShouldPass() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateInstanceConfiguration(instance));
    }

    @Test
    void validateInstanceConfiguration_WithNullInstance_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(null)
        );
        assertEquals("WhatsApp instance cannot be null", exception.getMessage());
    }

    @Test
    void validateInstanceConfiguration_WithNullInstanceId_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setInstanceId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(instance)
        );
        assertEquals("Instance ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateInstanceConfiguration_WithShortInstanceId_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setInstanceId("1234"); // Less than 5 characters

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(instance)
        );
        assertEquals("Instance ID must be at least 5 characters long", exception.getMessage());
    }

    @Test
    void validateInstanceConfiguration_WithInvalidInstanceIdCharacters_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setInstanceId("instance@123"); // Contains invalid character @

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(instance)
        );
        assertEquals("Instance ID contains invalid characters. Only alphanumeric, underscore and hyphen are allowed", 
                    exception.getMessage());
    }

    @Test
    void validateInstanceConfiguration_WithShortAccessToken_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setAccessToken("short"); // Less than 10 characters

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(instance)
        );
        assertEquals("Access token must be at least 10 characters long", exception.getMessage());
    }

    @Test
    void validateInstanceConfiguration_WithSuspendedStatus_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setStatus(WhatsAppInstanceStatus.SUSPENDED);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(instance)
        );
        assertEquals("Cannot use suspended WhatsApp instance", exception.getMessage());
    }

    @Test
    void validateInstanceConfiguration_WithNullCompany_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setCompany(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateInstanceConfiguration(instance)
        );
        assertEquals("WhatsApp instance must be associated with a company", exception.getMessage());
    }

    @Test
    void validateInstanceReadyForMessaging_WithConnectedInstance_ShouldPass() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setStatus(WhatsAppInstanceStatus.CONNECTED);
        instance.setIsActive(true);

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateInstanceReadyForMessaging(instance));
    }

    @Test
    void validateInstanceReadyForMessaging_WithDisconnectedInstance_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setStatus(WhatsAppInstanceStatus.DISCONNECTED);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validateInstanceReadyForMessaging(instance)
        );
        assertTrue(exception.getMessage().contains("is not connected"));
    }

    @Test
    void validateInstanceReadyForMessaging_WithInactiveInstance_ShouldThrowException() {
        // Arrange
        WhatsAppInstance instance = createValidInstance();
        instance.setStatus(WhatsAppInstanceStatus.CONNECTED);
        instance.setIsActive(false);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> validator.validateInstanceReadyForMessaging(instance)
        );
        assertTrue(exception.getMessage().contains("is not active"));
    }

    @Test
    void validatePhoneNumber_WithValidPhoneNumber_ShouldPass() {
        // Arrange
        String validPhone = "+5511999999999";

        // Act & Assert
        assertDoesNotThrow(() -> validator.validatePhoneNumber(validPhone));
    }

    @Test
    void validatePhoneNumber_WithFormattedPhoneNumber_ShouldPass() {
        // Arrange
        String formattedPhone = "+55 (11) 99999-9999";

        // Act & Assert
        assertDoesNotThrow(() -> validator.validatePhoneNumber(formattedPhone));
    }

    @Test
    void validatePhoneNumber_WithShortPhoneNumber_ShouldThrowException() {
        // Arrange
        String shortPhone = "123456789"; // 9 digits, less than 10

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validatePhoneNumber(shortPhone)
        );
        assertEquals("Phone number must be between 10 and 15 digits", exception.getMessage());
    }

    @Test
    void validatePhoneNumber_WithLongPhoneNumber_ShouldThrowException() {
        // Arrange
        String longPhone = "1234567890123456"; // 16 digits, more than 15

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validatePhoneNumber(longPhone)
        );
        assertEquals("Phone number must be between 10 and 15 digits", exception.getMessage());
    }

    @Test
    void validatePhoneNumber_WithInvalidCharacters_ShouldThrowException() {
        // Arrange
        String invalidPhone = "11999999999a";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validatePhoneNumber(invalidPhone)
        );
        assertEquals("Phone number must contain only digits (after removing formatting)", exception.getMessage());
    }

    @Test
    void validateMessageContent_WithValidMessage_ShouldPass() {
        // Arrange
        String validMessage = "Hello, this is a test message";

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateMessageContent(validMessage));
    }

    @Test
    void validateMessageContent_WithEmptyMessage_ShouldThrowException() {
        // Arrange
        String emptyMessage = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateMessageContent(emptyMessage)
        );
        assertEquals("Message content cannot be null or empty", exception.getMessage());
    }

    @Test
    void validateMessageContent_WithLongMessage_ShouldThrowException() {
        // Arrange
        String longMessage = "a".repeat(4097); // Exceeds 4096 character limit

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validateMessageContent(longMessage)
        );
        assertEquals("Message content cannot exceed 4096 characters", exception.getMessage());
    }

    @Test
    void validateMessageContent_WithMaxLengthMessage_ShouldPass() {
        // Arrange
        String maxLengthMessage = "a".repeat(4096); // Exactly 4096 characters

        // Act & Assert
        assertDoesNotThrow(() -> validator.validateMessageContent(maxLengthMessage));
    }

    private WhatsAppInstance createValidInstance() {
        return WhatsAppInstance.builder()
            .instanceId("valid-instance-123")
            .accessToken("valid-access-token-123")
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isActive(true)
            .company(mockCompany)
            .build();
    }
}