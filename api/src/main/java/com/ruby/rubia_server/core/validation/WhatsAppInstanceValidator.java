package com.ruby.rubia_server.core.validation;

import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Validator for WhatsApp instance configurations to ensure data integrity
 * and proper configuration before using instances for messaging operations.
 */
@Component
@Slf4j
public class WhatsAppInstanceValidator {

    private static final Pattern INSTANCE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");
    private static final int MIN_INSTANCE_ID_LENGTH = 5;
    private static final int MIN_ACCESS_TOKEN_LENGTH = 10;

    /**
     * Validates that a WhatsApp instance is properly configured for messaging operations.
     * 
     * @param instance the instance to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateInstanceConfiguration(WhatsAppInstance instance) {
        Assert.notNull(instance, "WhatsApp instance cannot be null");
        
        log.debug("Validating WhatsApp instance configuration for instance: {}", 
                 instance.getInstanceId());

        validateInstanceId(instance.getInstanceId());
        validateAccessToken(instance.getAccessToken());
        validateCompanyAssociation(instance);
        validateInstanceActive(instance);

        log.debug("WhatsApp instance validation successful for instance: {}", 
                 instance.getInstanceId());
    }

    /**
     * Validates that a WhatsApp instance is ready for messaging operations.
     * This includes connectivity checks beyond basic configuration validation.
     * 
     * @param instance the instance to validate
     * @throws IllegalStateException if instance is not ready for messaging
     */
    public void validateInstanceReadyForMessaging(WhatsAppInstance instance) {
        validateInstanceConfiguration(instance);

        if (!instance.getIsActive()) {
            throw new IllegalStateException(
                String.format("WhatsApp instance '%s' is not active", 
                             instance.getInstanceId()));
        }

        log.debug("WhatsApp instance is ready for messaging: {}", instance.getInstanceId());
    }

    /**
     * Validates the instance ID format and requirements.
     */
    private void validateInstanceId(String instanceId) {
        Assert.hasText(instanceId, "Instance ID cannot be null or empty");
        
        if (instanceId.length() < MIN_INSTANCE_ID_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Instance ID must be at least %d characters long", 
                             MIN_INSTANCE_ID_LENGTH));
        }
        
        if (!INSTANCE_ID_PATTERN.matcher(instanceId).matches()) {
            throw new IllegalArgumentException(
                "Instance ID contains invalid characters. Only alphanumeric, underscore and hyphen are allowed");
        }
    }

    /**
     * Validates the access token format and requirements.
     */
    private void validateAccessToken(String accessToken) {
        Assert.hasText(accessToken, "Access token cannot be null or empty");
        
        if (accessToken.length() < MIN_ACCESS_TOKEN_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Access token must be at least %d characters long", 
                             MIN_ACCESS_TOKEN_LENGTH));
        }
        
        if (!ACCESS_TOKEN_PATTERN.matcher(accessToken).matches()) {
            throw new IllegalArgumentException(
                "Access token contains invalid characters. Only alphanumeric, underscore and hyphen are allowed");
        }
    }


    /**
     * Validates that the instance has proper company association.
     */
    private void validateCompanyAssociation(WhatsAppInstance instance) {
        Assert.notNull(instance.getCompany(), "WhatsApp instance must be associated with a company");
        Assert.notNull(instance.getCompany().getId(), "Associated company must have a valid ID");
    }

    /**
     * Validates that the instance is active.
     */
    private void validateInstanceActive(WhatsAppInstance instance) {
        if (!instance.getIsActive()) {
            throw new IllegalArgumentException("Cannot use inactive WhatsApp instance");
        }
    }

    /**
     * Validates phone number format for WhatsApp messaging.
     * 
     * @param phoneNumber the phone number to validate
     * @throws IllegalArgumentException if phone number is invalid
     */
    public void validatePhoneNumber(String phoneNumber) {
        Assert.hasText(phoneNumber, "Phone number cannot be null or empty");
        
        // Remove common formatting characters
        String cleanPhone = phoneNumber.replaceAll("[\\s\\-\\(\\)\\+]", "");
        
        if (cleanPhone.length() < 10 || cleanPhone.length() > 15) {
            throw new IllegalArgumentException(
                "Phone number must be between 10 and 15 digits");
        }
        
        if (!cleanPhone.matches("\\d+")) {
            throw new IllegalArgumentException(
                "Phone number must contain only digits (after removing formatting)");
        }
    }

    /**
     * Validates message content for WhatsApp sending.
     * 
     * @param message the message content to validate
     * @throws IllegalArgumentException if message is invalid
     */
    public void validateMessageContent(String message) {
        Assert.hasText(message, "Message content cannot be null or empty");
        
        // WhatsApp has a 4096 character limit for text messages
        if (message.length() > 4096) {
            throw new IllegalArgumentException(
                "Message content cannot exceed 4096 characters");
        }
    }
}