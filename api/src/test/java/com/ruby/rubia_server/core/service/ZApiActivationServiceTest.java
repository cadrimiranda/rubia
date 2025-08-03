package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.PhoneCodeResult;
import com.ruby.rubia_server.core.entity.QrCodeResult;
import com.ruby.rubia_server.core.entity.ZApiStatus;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.service.WhatsAppInstanceService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import com.ruby.rubia_server.core.validation.WhatsAppInstanceValidator;
import com.ruby.rubia_server.core.factory.ZApiUrlFactory;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ZApiActivationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private WhatsAppInstanceService whatsAppInstanceService;

    @Mock
    private CompanyContextUtil companyContextUtil;

    @Mock
    private WhatsAppInstanceValidator instanceValidator;

    @Mock
    private ZApiUrlFactory urlFactory;

    private ZApiActivationService zapiActivationService;

    private static final String TEST_INSTANCE_ID = "TEST123";
    private static final String TEST_TOKEN = "test-token-123";
    private static final String CLIENT_TOKEN = "client-token-123";

    @BeforeEach
    void setUp() {
        zapiActivationService = new ZApiActivationService(whatsAppInstanceService, companyContextUtil, instanceValidator, urlFactory);
        
        // Mock company and instance setup
        Company mockCompany = new Company();
        WhatsAppInstance mockInstance = WhatsAppInstance.builder()
            .instanceId(TEST_INSTANCE_ID)
            .accessToken(TEST_TOKEN)
            .build();
            
        lenient().when(companyContextUtil.getCurrentCompany()).thenReturn(mockCompany);
        lenient().when(whatsAppInstanceService.findActiveConnectedInstance(mockCompany))
            .thenReturn(java.util.Optional.of(mockInstance));
        
        // Setup default URL factory behavior
        lenient().when(urlFactory.buildUrl(any(WhatsAppInstance.class), anyString()))
            .thenAnswer(invocation -> {
                WhatsAppInstance instance = invocation.getArgument(0);
                String endpoint = invocation.getArgument(1);
                return String.format("https://api.z-api.io/instances/%s/token/%s/%s", 
                                   instance.getInstanceId(), instance.getAccessToken(), endpoint);
            });
        
        // Setup default validator behavior
        lenient().doNothing().when(instanceValidator).validateInstanceConfiguration(any());
        
        ReflectionTestUtils.setField(zapiActivationService, "clientToken", CLIENT_TOKEN);
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
        String dataUrl = "data:image/png;base64," + base64Image;
        Map<String, Object> mockResponse = Map.of("value", dataUrl);
        
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
    void createHeaders_ShouldIncludeClientTokenAndContentType() {
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
        assertTrue(capturedEntity.getHeaders().containsKey("client-token"));
        assertEquals(CLIENT_TOKEN, capturedEntity.getHeaders().getFirst("client-token"));
        assertEquals("application/json", capturedEntity.getHeaders().getFirst("Content-Type"));
    }

    @Test
    void shouldUseDynamicInstanceDataFromService() {
        // Arrange - Different instance data than mock setup
        Company testCompany = new Company();
        WhatsAppInstance dynamicInstance = WhatsAppInstance.builder()
            .instanceId("dynamic-test-instance")
            .accessToken("dynamic-test-token")
            .build();
            
        when(companyContextUtil.getCurrentCompany()).thenReturn(testCompany);
        when(whatsAppInstanceService.findActiveConnectedInstance(testCompany))
            .thenReturn(Optional.of(dynamicInstance));

        Map<String, Object> mockResponse = Map.of("connected", "true");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                
                // Verify URL uses dynamic instance data
                assertTrue(url.contains("dynamic-test-instance"), "URL should contain dynamic instance ID");
                assertTrue(url.contains("dynamic-test-token"), "URL should contain dynamic token");
                
                return responseEntity;
            });

        // Act
        ZApiStatus result = zapiActivationService.getInstanceStatus();

        // Assert
        assertNotNull(result);
        assertTrue(result.isConnected());
        
        // Verify service methods were called
        verify(companyContextUtil).getCurrentCompany();
        verify(whatsAppInstanceService).findActiveConnectedInstance(testCompany);
    }

    @Test
    void shouldFallbackToConfiguringInstanceWhenNoConnectedInstance() {
        // Arrange - No connected instance but one in configuration
        Company testCompany = new Company();
        
        WhatsAppInstance configuringInstance = WhatsAppInstance.builder()
            .instanceId("configuring-instance")
            .accessToken("configuring-token")
            .status(WhatsAppInstanceStatus.CONFIGURING)
            .isActive(true)
            .build();

        List<WhatsAppInstance> allInstances = List.of(configuringInstance);
        
        when(companyContextUtil.getCurrentCompany()).thenReturn(testCompany);
        when(whatsAppInstanceService.findActiveConnectedInstance(testCompany))
            .thenReturn(Optional.empty()); // No connected instance
        when(whatsAppInstanceService.findByCompany(testCompany))
            .thenReturn(allInstances);

        Map<String, Object> mockResponse = Map.of("connected", "false", "session", "");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                
                // Verify URL uses configuring instance data
                assertTrue(url.contains("configuring-instance"), "URL should contain configuring instance ID");
                assertTrue(url.contains("configuring-token"), "URL should contain configuring token");
                
                return responseEntity;
            });

        // Act
        ZApiStatus result = zapiActivationService.getInstanceStatus();

        // Assert
        assertNotNull(result);
        assertFalse(result.isConnected());
        
        // Verify fallback logic was used
        verify(whatsAppInstanceService).findActiveConnectedInstance(testCompany);
        verify(whatsAppInstanceService).findByCompany(testCompany);
    }

    @Test
    void shouldHandleInstanceInAwaitingQrScanState() {
        // Arrange - Instance in QR scan state
        Company testCompany = new Company();
        
        WhatsAppInstance qrScanInstance = WhatsAppInstance.builder()
            .instanceId("qr-scan-instance")
            .accessToken("qr-scan-token")
            .status(WhatsAppInstanceStatus.AWAITING_QR_SCAN)
            .isActive(true)
            .build();

        List<WhatsAppInstance> allInstances = List.of(qrScanInstance);
        
        when(companyContextUtil.getCurrentCompany()).thenReturn(testCompany);
        when(whatsAppInstanceService.findActiveConnectedInstance(testCompany))
            .thenReturn(Optional.empty());
        when(whatsAppInstanceService.findByCompany(testCompany))
            .thenReturn(allInstances);

        byte[] mockQrBytes = "mock-qr-code".getBytes();
        ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(mockQrBytes, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
            .thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                
                // Verify URL uses QR scan instance data
                assertTrue(url.contains("qr-scan-instance"), "URL should contain QR scan instance ID");
                assertTrue(url.contains("qr-scan-token"), "URL should contain QR scan token");
                
                return responseEntity;
            });

        // Act
        QrCodeResult result = zapiActivationService.getQrCodeBytes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("bytes", result.getType());
        assertArrayEquals(mockQrBytes, (byte[]) result.getData());
    }

    @Test
    void shouldThrowExceptionWhenNoConfiguredInstanceFound() {
        // Arrange - Reset mocks and configure no instances with proper configuration
        reset(companyContextUtil, whatsAppInstanceService, instanceValidator);
        Company testCompany = new Company();
        
        WhatsAppInstance notConfiguredInstance = WhatsAppInstance.builder()
            .instanceId(null) // Missing configuration
            .accessToken(null)
            .status(WhatsAppInstanceStatus.NOT_CONFIGURED)
            .isActive(true)
            .build();

        List<WhatsAppInstance> allInstances = List.of(notConfiguredInstance);
        
        when(companyContextUtil.getCurrentCompany()).thenReturn(testCompany);
        when(whatsAppInstanceService.findActiveConnectedInstance(testCompany))
            .thenReturn(Optional.empty());
        when(whatsAppInstanceService.findByCompany(testCompany))
            .thenReturn(allInstances);

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> zapiActivationService.getInstanceStatus()
        );

        assertEquals("No configured WhatsApp instance found for activation", exception.getMessage());
        
        // Verify no REST call was made
        verify(restTemplate, never()).exchange(
            anyString(), 
            eq(HttpMethod.GET), 
            any(HttpEntity.class), 
            eq(Map.class)
        );
    }

    @Test
    void shouldHandleCompanyContextError() {
        // Arrange - Reset mocks and configure company context error
        reset(companyContextUtil, whatsAppInstanceService, instanceValidator);
        when(companyContextUtil.getCurrentCompany())
            .thenThrow(new IllegalStateException("No company context"));

        // Act & Assert
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> zapiActivationService.getInstanceStatus()
        );

        assertEquals("Cannot determine active WhatsApp instance", exception.getMessage());
        
        // Verify no service calls were made
        verify(whatsAppInstanceService, never()).findActiveConnectedInstance(any());
        verify(whatsAppInstanceService, never()).findByCompany(any());
    }

    @Test
    void shouldUseConnectedInstanceWhenAvailableDuringActivation() {
        // Arrange - Both connected and configuring instances available
        Company testCompany = new Company();
        
        WhatsAppInstance connectedInstance = WhatsAppInstance.builder()
            .instanceId("connected-instance")
            .accessToken("connected-token")
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isPrimary(true)
            .isActive(true)
            .build();

        when(companyContextUtil.getCurrentCompany()).thenReturn(testCompany);
        when(whatsAppInstanceService.findActiveConnectedInstance(testCompany))
            .thenReturn(Optional.of(connectedInstance));

        Map<String, Object> mockResponse = Map.of("connected", "true", "session", "active");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(mockResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
            .thenAnswer(invocation -> {
                String url = invocation.getArgument(0);
                
                // Should prefer connected instance over configuring ones
                assertTrue(url.contains("connected-instance"), "URL should use connected instance");
                assertTrue(url.contains("connected-token"), "URL should use connected token");
                
                return responseEntity;
            });

        // Act
        ZApiStatus result = zapiActivationService.getInstanceStatus();

        // Assert
        assertNotNull(result);
        assertTrue(result.isConnected());
        
        // Should not need to check for other instances
        verify(whatsAppInstanceService).findActiveConnectedInstance(testCompany);
        verify(whatsAppInstanceService, never()).findByCompany(testCompany);
    }
}