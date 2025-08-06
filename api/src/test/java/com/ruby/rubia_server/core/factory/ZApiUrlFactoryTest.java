package com.ruby.rubia_server.core.factory;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ZApiUrlFactoryTest {

    private ZApiUrlFactory urlFactory;
    private WhatsAppInstance mockInstance;
    private final String baseUrl = "https://api.z-api.io";

    @BeforeEach
    void setUp() {
        urlFactory = new ZApiUrlFactory(baseUrl);
        
        Company mockCompany = Company.builder()
            .id(UUID.randomUUID())
            .name("Test Company")
            .build();
            
        mockInstance = WhatsAppInstance.builder()
            .instanceId("test-instance-123")
            .accessToken("test-token-456")
            .company(mockCompany)
            .build();
    }

    @Test
    void buildUrl_WithValidInstanceAndEndpoint_ShouldReturnCorrectUrl() {
        // Arrange
        String endpoint = "send-text";
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/send-text";

        // Act
        String actualUrl = urlFactory.buildUrl(mockInstance, endpoint);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void buildUrl_WithNullInstance_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> urlFactory.buildUrl(null, "send-text")
        );
        assertEquals("WhatsApp instance cannot be null", exception.getMessage());
    }

    @Test
    void buildUrl_WithNullInstanceId_ShouldThrowException() {
        // Arrange
        mockInstance.setInstanceId(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> urlFactory.buildUrl(mockInstance, "send-text")
        );
        assertEquals("Instance ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void buildUrl_WithEmptyInstanceId_ShouldThrowException() {
        // Arrange
        mockInstance.setInstanceId("");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> urlFactory.buildUrl(mockInstance, "send-text")
        );
        assertEquals("Instance ID cannot be null or empty", exception.getMessage());
    }

    @Test
    void buildUrl_WithNullAccessToken_ShouldThrowException() {
        // Arrange
        mockInstance.setAccessToken(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> urlFactory.buildUrl(mockInstance, "send-text")
        );
        assertEquals("Access token cannot be null or empty", exception.getMessage());
    }

    @Test
    void buildUrl_WithEmptyEndpoint_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> urlFactory.buildUrl(mockInstance, "")
        );
        assertEquals("Endpoint cannot be null or empty", exception.getMessage());
    }

    @Test
    void buildUrlWithPathParams_WithValidParams_ShouldReturnCorrectUrl() {
        // Arrange
        String endpoint = "phone-code";
        String phoneNumber = "5511999999999";
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/phone-code/5511999999999";

        // Act
        String actualUrl = urlFactory.buildUrlWithPathParams(mockInstance, endpoint, phoneNumber);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void buildUrlWithPathParams_WithMultipleParams_ShouldReturnCorrectUrl() {
        // Arrange
        String endpoint = "files";
        String param1 = "folder1";
        String param2 = "folder2";
        String param3 = "document.pdf";
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/files/folder1/folder2/document.pdf";

        // Act
        String actualUrl = urlFactory.buildUrlWithPathParams(mockInstance, endpoint, param1, param2, param3);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void buildUrlWithPathParams_WithNullParam_ShouldSkipNullParam() {
        // Arrange
        String endpoint = "phone-code";
        String validParam = "5511999999999";
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/phone-code/5511999999999";

        // Act
        String actualUrl = urlFactory.buildUrlWithPathParams(mockInstance, endpoint, validParam, null);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void buildUrlWithQueryParams_WithValidParams_ShouldReturnCorrectUrl() {
        // Arrange
        String endpoint = "messages";
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/messages?page=1&limit=10";

        // Act
        String actualUrl = urlFactory.buildUrlWithQueryParams(mockInstance, endpoint, "page", "1", "limit", "10");

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void buildUrlWithQueryParams_WithOddNumberOfParams_ShouldThrowException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> urlFactory.buildUrlWithQueryParams(mockInstance, "messages", "page", "1", "limit")
        );
        assertEquals("Query parameters must be provided as key-value pairs", exception.getMessage());
    }

    @Test
    void buildUri_WithValidParams_ShouldReturnCorrectUri() {
        // Arrange
        String endpoint = "status";
        String expectedUriString = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/status";

        // Act
        URI actualUri = urlFactory.buildUri(mockInstance, endpoint);

        // Assert
        assertEquals(expectedUriString, actualUri.toString());
    }

    @Test
    void getBaseUrl_ShouldReturnConfiguredBaseUrl() {
        // Act
        String actualBaseUrl = urlFactory.getBaseUrl();

        // Assert
        assertEquals(baseUrl, actualBaseUrl);
    }

    // Tests for CommonEndpoints static methods
    @Test
    void commonEndpoints_SendText_ShouldReturnCorrectUrl() {
        // Arrange
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/send-text";

        // Act
        String actualUrl = ZApiUrlFactory.CommonEndpoints.sendText(urlFactory, mockInstance);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void commonEndpoints_SendFileUrl_ShouldReturnCorrectUrl() {
        // Arrange
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/send-file-url";

        // Act
        String actualUrl = ZApiUrlFactory.CommonEndpoints.sendFileUrl(urlFactory, mockInstance);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void commonEndpoints_SendAudio_ShouldReturnCorrectUrl() {
        // Arrange
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/send-audio";

        // Act
        String actualUrl = ZApiUrlFactory.CommonEndpoints.sendAudio(urlFactory, mockInstance);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void commonEndpoints_Status_ShouldReturnCorrectUrl() {
        // Arrange
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/status";

        // Act
        String actualUrl = ZApiUrlFactory.CommonEndpoints.status(urlFactory, mockInstance);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void commonEndpoints_QrCode_ShouldReturnCorrectUrl() {
        // Arrange
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/qr-code";

        // Act
        String actualUrl = ZApiUrlFactory.CommonEndpoints.qrCode(urlFactory, mockInstance);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void commonEndpoints_PhoneCode_ShouldReturnCorrectUrl() {
        // Arrange
        String phoneNumber = "5511999999999";
        String expectedUrl = "https://api.z-api.io/instances/test-instance-123/token/test-token-456/phone-code/5511999999999";

        // Act
        String actualUrl = ZApiUrlFactory.CommonEndpoints.phoneCode(urlFactory, mockInstance, phoneNumber);

        // Assert
        assertEquals(expectedUrl, actualUrl);
    }

    @Test
    void constructor_WithCustomBaseUrl_ShouldUseCustomUrl() {
        // Arrange
        String customBaseUrl = "https://custom-api.example.com";
        ZApiUrlFactory customFactory = new ZApiUrlFactory(customBaseUrl);
        String expectedUrl = "https://custom-api.example.com/instances/test-instance-123/token/test-token-456/send-text";

        // Act
        String actualUrl = customFactory.buildUrl(mockInstance, "send-text");

        // Assert
        assertEquals(expectedUrl, actualUrl);
        assertEquals(customBaseUrl, customFactory.getBaseUrl());
    }

    @Test
    void buildUrl_WithSpecialCharactersInEndpoint_ShouldEncodeCorrectly() {
        // Arrange
        String endpoint = "send text"; // Space should be encoded
        
        // Act & Assert - Should not throw exception, UriComponentsBuilder handles encoding
        assertDoesNotThrow(() -> urlFactory.buildUrl(mockInstance, endpoint));
    }

    @Test
    void buildUrlWithQueryParams_WithSpecialCharacters_ShouldBuildCorrectUrl() {
        // Arrange
        String endpoint = "search";
        String query = "hello world";
        
        // Act
        String actualUrl = urlFactory.buildUrlWithQueryParams(mockInstance, endpoint, "q", query);
        
        // Assert - Should create a valid URL with query parameters (Spring's UriComponentsBuilder handles encoding)
        assertTrue(actualUrl.contains("search?q=hello"), 
                  "URL should contain query parameter, but was: " + actualUrl);
        assertTrue(actualUrl.contains("instances/test-instance-123"), 
                  "URL should contain instance ID");
    }
}