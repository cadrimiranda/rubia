package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.PhoneCodeResult;
import com.ruby.rubia_server.core.entity.QrCodeResult;
import com.ruby.rubia_server.core.entity.ZApiStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZApiActivationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ZApiActivationService zapiActivationService;

    private static final String TEST_INSTANCE_URL = "https://api.z-api.io/instances/TEST123";
    private static final String TEST_TOKEN = "test-token-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(zapiActivationService, "instanceUrl", TEST_INSTANCE_URL);
        ReflectionTestUtils.setField(zapiActivationService, "token", TEST_TOKEN);
        ReflectionTestUtils.setField(zapiActivationService, "restTemplate", restTemplate);
    }

    @Test
    void getInstanceStatus_WithSuccessfulResponse_ShouldReturnConnectedStatus() {
        // Arrange
        Map<String, Object> mockResponse = Map.of(
            "connected", "true",
            "session", "test-session-123",
            "smartphoneConnected", "true"
        );
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        ZApiStatus result = zapiActivationService.getInstanceStatus();

        // Assert
        assertNotNull(result);
        assertTrue(result.isConnected());
        assertEquals("test-session-123", result.getSession());
        assertTrue(result.isSmartphoneConnected());
        assertFalse(result.isNeedsQrCode());
        assertNull(result.getError());
        assertEquals(mockResponse, result.getRawResponse());

        // Verify the correct URL was called
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        assertTrue(urlCaptor.getValue().contains("/status"));
    }

    @Test
    void getInstanceStatus_WithDisconnectedResponse_ShouldReturnDisconnectedStatus() {
        // Arrange
        Map<String, Object> mockResponse = Map.of(
            "connected", "false",
            "session", "",
            "smartphoneConnected", "false"
        );
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        ZApiStatus result = zapiActivationService.getInstanceStatus();

        // Assert
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertEquals("", result.getSession());
        assertFalse(result.isSmartphoneConnected());
        assertTrue(result.isNeedsQrCode());
        assertNull(result.getError());
    }

    @Test
    void getInstanceStatus_WithException_ShouldReturnErrorStatus() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RestClientException("Connection timeout"));

        // Act
        ZApiStatus result = zapiActivationService.getInstanceStatus();

        // Assert
        assertNotNull(result);
        assertFalse(result.isConnected());
        assertTrue(result.isNeedsQrCode());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Connection timeout"));
    }

    @Test
    void getQrCodeBytes_WithSuccessfulResponse_ShouldReturnBytes() {
        // Arrange
        byte[] mockBytes = "fake-qr-code-bytes".getBytes();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(mockBytes, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
            .thenReturn(responseEntity);

        // Act
        QrCodeResult result = zapiActivationService.getQrCodeBytes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("bytes", result.getType());
        assertEquals(mockBytes, result.getData());
        assertNull(result.getError());

        // Verify the correct URL was called
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class));
        assertTrue(urlCaptor.getValue().contains("/qr-code"));
    }

    @Test
    void getQrCodeBytes_WithException_ShouldReturnErrorResult() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
            .thenThrow(new RestClientException("Network error"));

        // Act
        QrCodeResult result = zapiActivationService.getQrCodeBytes();

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Network error"));
    }

    @Test
    void getQrCodeImage_WithSuccessfulResponse_ShouldReturnBase64Image() {
        // Arrange
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
        Map<String, Object> mockResponse = Map.of("image", base64Image);
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        QrCodeResult result = zapiActivationService.getQrCodeImage();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("base64", result.getType());
        assertEquals(base64Image, result.getData());
        assertNull(result.getError());

        // Verify the correct URL was called
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        assertTrue(urlCaptor.getValue().contains("/qr-code/image"));
    }

    @Test
    void getQrCodeImage_WithNullResponse_ShouldReturnErrorResult() {
        // Arrange
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        QrCodeResult result = zapiActivationService.getQrCodeImage();

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNotNull(result.getError());
        assertEquals("Failed to get QR code image", result.getError());
    }

    @Test
    void getPhoneCode_WithSuccessfulResponse_ShouldReturnPhoneCode() {
        // Arrange
        String phoneNumber = "5511999999999";
        String code = "123456";
        Map<String, Object> mockResponse = Map.of("code", code);
        
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        PhoneCodeResult result = zapiActivationService.getPhoneCode(phoneNumber);

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(code, result.getCode());
        assertEquals(phoneNumber, result.getPhoneNumber());
        assertNull(result.getError());

        // Verify the correct URL was called with phone number
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        assertTrue(urlCaptor.getValue().contains("/phone-code/" + phoneNumber));
    }

    @Test
    void getPhoneCode_WithException_ShouldReturnErrorResult() {
        // Arrange
        String phoneNumber = "5511999999999";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RestClientException("Invalid phone number"));

        // Act
        PhoneCodeResult result = zapiActivationService.getPhoneCode(phoneNumber);

        // Assert
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getCode());
        assertNotNull(result.getError());
        assertTrue(result.getError().contains("Invalid phone number"));
    }

    @Test
    void restartInstance_WithSuccessfulResponse_ShouldReturnTrue() {
        // Arrange
        Map<String, Object> mockResponse = Map.of("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        boolean result = zapiActivationService.restartInstance();

        // Assert
        assertTrue(result);

        // Verify the correct URL was called
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        assertTrue(urlCaptor.getValue().contains("/restart"));
    }

    @Test
    void restartInstance_WithErrorResponse_ShouldReturnFalse() {
        // Arrange
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        boolean result = zapiActivationService.restartInstance();

        // Assert
        assertFalse(result);
    }

    @Test
    void restartInstance_WithException_ShouldReturnFalse() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RestClientException("Service unavailable"));

        // Act
        boolean result = zapiActivationService.restartInstance();

        // Assert
        assertFalse(result);
    }

    @Test
    void disconnectInstance_WithSuccessfulResponse_ShouldReturnTrue() {
        // Arrange
        Map<String, Object> mockResponse = Map.of("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);

        // Act
        boolean result = zapiActivationService.disconnectInstance();

        // Assert
        assertTrue(result);

        // Verify the correct URL was called
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        assertTrue(urlCaptor.getValue().contains("/disconnect"));
    }

    @Test
    void disconnectInstance_WithException_ShouldReturnFalse() {
        // Arrange
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenThrow(new RestClientException("Connection failed"));

        // Act
        boolean result = zapiActivationService.disconnectInstance();

        // Assert
        assertFalse(result);
    }

    @Test
    void createHeaders_ShouldIncludeAuthorizationAndContentType() {
        // This is a private method, so we test it indirectly through other methods
        
        // Arrange
        Map<String, Object> mockResponse = Map.of("connected", "true");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        // Act
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenReturn(responseEntity);
        
        zapiActivationService.getInstanceStatus();
        
        // Assert - capture the HttpEntity to verify headers
        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.GET), entityCaptor.capture(), eq(Map.class));
        
        HttpEntity<?> capturedEntity = entityCaptor.getValue();
        assertNotNull(capturedEntity.getHeaders());
        assertTrue(capturedEntity.getHeaders().containsKey("Authorization"));
        assertEquals("Bearer " + TEST_TOKEN, capturedEntity.getHeaders().getFirst("Authorization"));
        assertEquals("application/json", capturedEntity.getHeaders().getFirst("Content-Type"));
    }
}