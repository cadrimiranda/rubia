package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.MessagingProvider;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import com.ruby.rubia_server.core.repository.WhatsAppInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class WhatsAppInstanceServiceTest {

    @Mock
    private WhatsAppInstanceRepository whatsappInstanceRepository;

    @Mock
    private PhoneService phoneService;

    @InjectMocks
    private WhatsAppInstanceService whatsappInstanceService;

    private Company testCompany;
    private WhatsAppInstance testInstance;

    @BeforeEach
    void setUp() {
        CompanyGroup companyGroup = CompanyGroup.builder()
            .id(UUID.randomUUID())
            .name("Test Group")
            .build();

        testCompany = Company.builder()
            .id(UUID.randomUUID())
            .name("Test Company")
            .slug("test-company")
            .companyGroup(companyGroup)
            .maxWhatsappNumbers(3)
            .build();

        testInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .phoneNumber("5511999999999")
            .displayName("Test WhatsApp")
            .provider(MessagingProvider.Z_API)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isActive(true)
            .isPrimary(true)
            .createdAt(LocalDateTime.now())
            .build();

        // Configure PhoneService mock to return valid for all phone numbers (lenient to avoid unnecessary stubbing errors)
        lenient().when(phoneService.isValid(anyString())).thenReturn(true);
    }

    @Test
    void findByCompany_ShouldReturnActiveInstances() {
        // Arrange
        List<WhatsAppInstance> expectedInstances = List.of(testInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(expectedInstances);

        // Act
        List<WhatsAppInstance> result = whatsappInstanceService.findByCompany(testCompany);

        // Assert
        assertEquals(expectedInstances, result);
        verify(whatsappInstanceRepository).findByCompanyAndIsActiveTrue(testCompany);
    }

    @Test
    void findPrimaryByCompany_ShouldReturnPrimaryInstance() {
        // Arrange
        when(whatsappInstanceRepository.findByCompanyAndIsPrimaryTrueAndIsActiveTrue(testCompany))
            .thenReturn(Optional.of(testInstance));

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findPrimaryByCompany(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testInstance, result.get());
        verify(whatsappInstanceRepository).findByCompanyAndIsPrimaryTrueAndIsActiveTrue(testCompany);
    }

    @Test
    void hasConfiguredInstance_WithConfiguredInstance_ShouldReturnTrue() {
        // Arrange
        List<WhatsAppInstance> instances = List.of(testInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(instances);

        // Act
        boolean result = whatsappInstanceService.hasConfiguredInstance(testCompany);

        // Assert
        assertTrue(result);
    }

    @Test
    void hasConfiguredInstance_WithNotConfiguredInstance_ShouldReturnFalse() {
        // Arrange
        WhatsAppInstance notConfiguredInstance = WhatsAppInstance.builder()
            .status(WhatsAppInstanceStatus.NOT_CONFIGURED)
            .isActive(true)
            .build();
        
        List<WhatsAppInstance> instances = List.of(notConfiguredInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(instances);

        // Act
        boolean result = whatsappInstanceService.hasConfiguredInstance(testCompany);

        // Assert
        assertFalse(result);
    }

    @Test
    void hasConnectedInstance_WithConnectedInstance_ShouldReturnTrue() {
        // Arrange
        List<WhatsAppInstance> instances = List.of(testInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(instances);

        // Act
        boolean result = whatsappInstanceService.hasConnectedInstance(testCompany);

        // Assert
        assertTrue(result);
    }

    @Test
    void createInstance_FirstInstance_ShouldSetAsPrimary() {
        // Arrange
        String phoneNumber = "5511999888777";
        String displayName = "New WhatsApp";
        
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(List.of()); // No existing instances
        
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WhatsAppInstance result = whatsappInstanceService.createInstance(testCompany, phoneNumber, displayName);

        // Assert
        assertNotNull(result);
        assertEquals(testCompany, result.getCompany());
        assertEquals(phoneNumber, result.getPhoneNumber());
        assertEquals(displayName, result.getDisplayName());
        assertTrue(result.getIsPrimary()); // Should be primary as first instance
        assertTrue(result.getIsActive());
        assertEquals(WhatsAppInstanceStatus.NOT_CONFIGURED, result.getStatus());
        
        verify(whatsappInstanceRepository).save(any(WhatsAppInstance.class));
    }

    @Test
    void createInstance_SubsequentInstance_ShouldNotSetAsPrimary() {
        // Arrange
        String phoneNumber = "5511999888777";
        String displayName = "New WhatsApp";
        
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(List.of(testInstance)); // Existing instance
        
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WhatsAppInstance result = whatsappInstanceService.createInstance(testCompany, phoneNumber, displayName);

        // Assert
        assertNotNull(result);
        assertFalse(result.getIsPrimary()); // Should NOT be primary
        assertTrue(result.getIsActive());
    }

    @Test
    void updateInstanceStatus_ShouldUpdateStatusAndTimestamp() {
        // Arrange
        UUID instanceId = testInstance.getId();
        WhatsAppInstanceStatus newStatus = WhatsAppInstanceStatus.DISCONNECTED;
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WhatsAppInstance result = whatsappInstanceService.updateInstanceStatus(instanceId, newStatus);

        // Assert
        assertEquals(newStatus, result.getStatus());
        assertNotNull(result.getLastStatusCheck());
        verify(whatsappInstanceRepository).save(testInstance);
    }

    @Test
    void updateInstanceStatus_WithConnectedStatus_ShouldUpdateLastConnectedAt() {
        // Arrange
        UUID instanceId = testInstance.getId();
        WhatsAppInstanceStatus connectedStatus = WhatsAppInstanceStatus.CONNECTED;
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WhatsAppInstance result = whatsappInstanceService.updateInstanceStatus(instanceId, connectedStatus);

        // Assert
        assertEquals(connectedStatus, result.getStatus());
        assertNotNull(result.getLastConnectedAt());
        assertNull(result.getErrorMessage()); // Should clear error message
        verify(whatsappInstanceRepository).save(testInstance);
    }

    @Test
    void updateInstanceStatus_WithInvalidId_ShouldThrowException() {
        // Arrange
        UUID invalidId = UUID.randomUUID();
        when(whatsappInstanceRepository.findById(invalidId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            whatsappInstanceService.updateInstanceStatus(invalidId, WhatsAppInstanceStatus.CONNECTED)
        );
    }

    @Test
    void updateInstanceConfiguration_ShouldUpdateConfigurationFields() {
        // Arrange
        UUID instanceId = testInstance.getId();
        String instanceIdValue = "ZAPI123456";
        String accessToken = "token123";
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WhatsAppInstance result = whatsappInstanceService.updateInstanceConfiguration(
            instanceId, instanceIdValue, accessToken
        );

        // Assert
        assertEquals(instanceIdValue, result.getInstanceId());
        assertEquals(accessToken, result.getAccessToken());
        assertEquals(WhatsAppInstanceStatus.CONFIGURING, result.getStatus());
        verify(whatsappInstanceRepository).save(testInstance);
    }

    @Test
    void markInstanceAsError_ShouldSetErrorStatusAndMessage() {
        // Arrange
        UUID instanceId = testInstance.getId();
        String errorMessage = "Connection failed";
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenReturn(testInstance);

        // Act
        whatsappInstanceService.markInstanceAsError(instanceId, errorMessage);

        // Assert
        ArgumentCaptor<WhatsAppInstance> captor = ArgumentCaptor.forClass(WhatsAppInstance.class);
        verify(whatsappInstanceRepository).save(captor.capture());
        
        WhatsAppInstance savedInstance = captor.getValue();
        assertEquals(WhatsAppInstanceStatus.ERROR, savedInstance.getStatus());
        assertEquals(errorMessage, savedInstance.getErrorMessage());
        assertNotNull(savedInstance.getLastStatusCheck());
    }

    @Test
    void setPrimaryInstance_ShouldUpdatePrimaryFlags() {
        // Arrange
        UUID instanceId = testInstance.getId();
        
        WhatsAppInstance anotherInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(true) // Currently primary
            .isActive(true)
            .build();
        
        List<WhatsAppInstance> companyInstances = List.of(testInstance, anotherInstance);
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(companyInstances);
        when(whatsappInstanceRepository.saveAll(anyList()))
            .thenReturn(companyInstances);
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenReturn(testInstance);

        // Act
        whatsappInstanceService.setPrimaryInstance(instanceId);

        // Assert
        verify(whatsappInstanceRepository).saveAll(anyList()); // Save all other instances
        verify(whatsappInstanceRepository).save(testInstance); // Save the new primary
        
        // The other instance should have isPrimary set to false
        assertFalse(anotherInstance.getIsPrimary());
        assertTrue(testInstance.getIsPrimary());
    }

    @Test
    void deactivateInstance_ShouldSetInactiveAndSuspended() {
        // Arrange
        UUID instanceId = testInstance.getId();
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenReturn(testInstance);

        // Act
        whatsappInstanceService.deactivateInstance(instanceId);

        // Assert
        ArgumentCaptor<WhatsAppInstance> captor = ArgumentCaptor.forClass(WhatsAppInstance.class);
        verify(whatsappInstanceRepository).save(captor.capture());
        
        WhatsAppInstance savedInstance = captor.getValue();
        assertFalse(savedInstance.getIsActive());
        assertEquals(WhatsAppInstanceStatus.SUSPENDED, savedInstance.getStatus());
    }

    @Test
    void deactivateInstance_WhenPrimary_ShouldPromoteAnotherInstance() {
        // Arrange
        UUID instanceId = testInstance.getId();
        testInstance.setIsPrimary(true);
        
        WhatsAppInstance anotherInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(false)
            .isActive(true)
            .build();
        
        List<WhatsAppInstance> activeInstances = List.of(anotherInstance);
        
        when(whatsappInstanceRepository.findById(instanceId))
            .thenReturn(Optional.of(testInstance));
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(activeInstances);
        when(whatsappInstanceRepository.save(any(WhatsAppInstance.class)))
            .thenReturn(testInstance);

        // Act
        whatsappInstanceService.deactivateInstance(instanceId);

        // Assert
        verify(whatsappInstanceRepository, times(2)).save(any(WhatsAppInstance.class));
        assertTrue(anotherInstance.getIsPrimary()); // Should be promoted to primary
    }

    @Test
    void findNeedingConfiguration_ShouldReturnInstancesNeedingSetup() {
        // Arrange
        WhatsAppInstance notConfiguredInstance = WhatsAppInstance.builder()
            .status(WhatsAppInstanceStatus.NOT_CONFIGURED)
            .isActive(true)
            .build();
        
        WhatsAppInstance errorInstance = WhatsAppInstance.builder()
            .status(WhatsAppInstanceStatus.ERROR)
            .isActive(true)
            .build();
        
        WhatsAppInstance connectedInstance = WhatsAppInstance.builder()
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isActive(true)
            .build();
        
        List<WhatsAppInstance> allInstances = List.of(notConfiguredInstance, errorInstance, connectedInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(allInstances);

        // Act
        List<WhatsAppInstance> result = whatsappInstanceService.findNeedingConfiguration(testCompany);

        // Assert
        assertEquals(2, result.size()); // Only not configured and error instances
        assertTrue(result.contains(notConfiguredInstance));
        assertTrue(result.contains(errorInstance));
        assertFalse(result.contains(connectedInstance));
    }

    @Test
    void findActiveConnectedInstance_WithPrimaryConnectedInstance_ShouldReturnPrimary() {
        // Arrange
        WhatsAppInstance primaryInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isPrimary(true)
            .isActive(true)
            .instanceId("primary-instance")
            .accessToken("primary-token")
            .build();

        WhatsAppInstance secondaryInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isPrimary(false)
            .isActive(true)
            .instanceId("secondary-instance")
            .accessToken("secondary-token")
            .build();

        List<WhatsAppInstance> connectedInstances = List.of(primaryInstance, secondaryInstance);
        when(whatsappInstanceRepository.findByCompanyAndStatusAndIsActiveTrue(
            testCompany, WhatsAppInstanceStatus.CONNECTED))
            .thenReturn(connectedInstances);

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(primaryInstance, result.get());
        assertTrue(result.get().getIsPrimary());
        verify(whatsappInstanceRepository).findByCompanyAndStatusAndIsActiveTrue(
            testCompany, WhatsAppInstanceStatus.CONNECTED);
    }

    @Test
    void findActiveConnectedInstance_WithNoPrimaryButConnectedInstances_ShouldReturnFirstConnected() {
        // Arrange
        WhatsAppInstance firstInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isPrimary(false)
            .isActive(true)
            .instanceId("first-instance")
            .accessToken("first-token")
            .build();

        WhatsAppInstance secondInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isPrimary(false)
            .isActive(true)
            .instanceId("second-instance")
            .accessToken("second-token")
            .build();

        List<WhatsAppInstance> connectedInstances = List.of(firstInstance, secondInstance);
        when(whatsappInstanceRepository.findByCompanyAndStatusAndIsActiveTrue(
            testCompany, WhatsAppInstanceStatus.CONNECTED))
            .thenReturn(connectedInstances);

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(firstInstance, result.get());
        assertFalse(result.get().getIsPrimary());
    }

    @Test
    void findActiveConnectedInstance_WithNoConnectedInstances_ShouldReturnEmpty() {
        // Arrange
        when(whatsappInstanceRepository.findByCompanyAndStatusAndIsActiveTrue(
            testCompany, WhatsAppInstanceStatus.CONNECTED))
            .thenReturn(List.of());

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isEmpty());
        verify(whatsappInstanceRepository).findByCompanyAndStatusAndIsActiveTrue(
            testCompany, WhatsAppInstanceStatus.CONNECTED);
    }

    @Test
    void findActiveConnectedInstance_WithMixedStatusInstances_ShouldOnlyConsiderConnected() {
        // Arrange
        WhatsAppInstance connectedInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isPrimary(true)
            .isActive(true)
            .instanceId("connected-instance")
            .accessToken("connected-token")
            .build();

        WhatsAppInstance disconnectedInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.DISCONNECTED)
            .isPrimary(false)
            .isActive(true)
            .instanceId("disconnected-instance")
            .accessToken("disconnected-token")
            .build();

        WhatsAppInstance errorInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .status(WhatsAppInstanceStatus.ERROR)
            .isPrimary(false)
            .isActive(true)
            .instanceId("error-instance")
            .accessToken("error-token")
            .build();

        // Repository only returns connected instances
        List<WhatsAppInstance> connectedInstances = List.of(connectedInstance);
        when(whatsappInstanceRepository.findByCompanyAndStatusAndIsActiveTrue(
            testCompany, WhatsAppInstanceStatus.CONNECTED))
            .thenReturn(connectedInstances);

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(connectedInstance, result.get());
        assertEquals(WhatsAppInstanceStatus.CONNECTED, result.get().getStatus());
    }

    @Test
    void findInstanceForMessaging_WithValidPhoneNumber_ShouldReturnInstanceByPhone() {
        // Arrange
        String phoneNumber = "5511999888777";
        WhatsAppInstance instanceForPhone = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .phoneNumber(phoneNumber)
            .status(WhatsAppInstanceStatus.CONNECTED)
            .isActive(true)
            .instanceId("phone-instance")
            .accessToken("phone-token")
            .build();

        when(whatsappInstanceRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber))
            .thenReturn(Optional.of(instanceForPhone));

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findInstanceForMessaging(phoneNumber);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(instanceForPhone, result.get());
        assertEquals(phoneNumber, result.get().getPhoneNumber());
        verify(whatsappInstanceRepository).findByPhoneNumberAndIsActiveTrue(phoneNumber);
    }

    @Test
    void findInstanceForMessaging_WithDisconnectedPhoneInstance_ShouldReturnEmpty() {
        // Arrange
        String phoneNumber = "5511999888777";
        WhatsAppInstance disconnectedInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .phoneNumber(phoneNumber)
            .status(WhatsAppInstanceStatus.DISCONNECTED)
            .isActive(true)
            .instanceId("disconnected-instance")
            .accessToken("disconnected-token")
            .build();

        when(whatsappInstanceRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber))
            .thenReturn(Optional.of(disconnectedInstance));

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findInstanceForMessaging(phoneNumber);

        // Assert
        assertTrue(result.isEmpty());
        verify(whatsappInstanceRepository).findByPhoneNumberAndIsActiveTrue(phoneNumber);
    }

    @Test
    void findInstanceForMessaging_WithInvalidPhoneNumber_ShouldReturnEmpty() {
        // Arrange
        String invalidPhoneNumber = "invalid-phone";
        when(whatsappInstanceRepository.findByPhoneNumberAndIsActiveTrue(invalidPhoneNumber))
            .thenReturn(Optional.empty());

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findInstanceForMessaging(invalidPhoneNumber);

        // Assert
        assertTrue(result.isEmpty());
        verify(whatsappInstanceRepository).findByPhoneNumberAndIsActiveTrue(invalidPhoneNumber);
    }
}