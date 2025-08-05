package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.MessagingProvider;
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
            .instanceId("test-instance-123")
            .accessToken("test-token-456")
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
    void hasConfiguredInstance_WithInactiveInstance_ShouldReturnFalse() {
        // Arrange
        WhatsAppInstance inactiveInstance = WhatsAppInstance.builder()
            .isActive(false)
            .build();
        
        List<WhatsAppInstance> instances = List.of(); // No active instances
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
        assertTrue(result.getIsActive());
        
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
        assertEquals(instanceIdValue, result.getInstanceId());
        verify(whatsappInstanceRepository).save(testInstance);
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
        assertFalse(savedInstance.getIsActive());
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
    void findNeedingConfiguration_ShouldReturnInstancesWithoutConfiguration() {
        // Arrange
        WhatsAppInstance notConfiguredInstance = WhatsAppInstance.builder()
            .instanceId(null) // Missing configuration
            .accessToken(null)
            .isActive(true)
            .build();
        
        WhatsAppInstance configuredInstance = WhatsAppInstance.builder()
            .instanceId("configured-id")
            .accessToken("configured-token")
            .isActive(true)
            .build();
        
        List<WhatsAppInstance> allInstances = List.of(notConfiguredInstance, configuredInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(allInstances);

        // Act
        List<WhatsAppInstance> result = whatsappInstanceService.findNeedingConfiguration(testCompany);

        // Assert
        assertEquals(1, result.size()); // Only not configured instance
        assertTrue(result.contains(notConfiguredInstance));
        assertFalse(result.contains(configuredInstance));
    }

    @Test
    void findActiveConnectedInstance_WithPrimaryInstance_ShouldReturnPrimary() {
        // Arrange
        WhatsAppInstance primaryInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(true)
            .isActive(true)
            .instanceId("primary-instance")
            .accessToken("primary-token")
            .build();

        WhatsAppInstance secondaryInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(false)
            .isActive(true)
            .instanceId("secondary-instance")
            .accessToken("secondary-token")
            .build();

        List<WhatsAppInstance> activeInstances = List.of(primaryInstance, secondaryInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(activeInstances);

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(primaryInstance, result.get());
        assertTrue(result.get().getIsPrimary());
        verify(whatsappInstanceRepository).findByCompanyAndIsActiveTrue(testCompany);
    }

    @Test
    void findActiveConnectedInstance_WithNoPrimaryButActiveInstances_ShouldReturnFirstActive() {
        // Arrange
        WhatsAppInstance firstInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(false)
            .isActive(true)
            .instanceId("first-instance")
            .accessToken("first-token")
            .build();

        WhatsAppInstance secondInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(false)
            .isActive(true)
            .instanceId("second-instance")
            .accessToken("second-token")
            .build();

        List<WhatsAppInstance> activeInstances = List.of(firstInstance, secondInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(activeInstances);

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(firstInstance, result.get());
        assertFalse(result.get().getIsPrimary());
    }

    @Test
    void findActiveConnectedInstance_WithNoActiveInstances_ShouldReturnEmpty() {
        // Arrange
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(List.of());

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isEmpty());
        verify(whatsappInstanceRepository).findByCompanyAndIsActiveTrue(testCompany);
    }

    @Test
    void findActiveConnectedInstance_WithMixedActiveStatus_ShouldOnlyConsiderActive() {
        // Arrange
        WhatsAppInstance activeInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(true)
            .isActive(true)
            .instanceId("active-instance")
            .accessToken("active-token")
            .build();

        WhatsAppInstance inactiveInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .isPrimary(false)
            .isActive(false)
            .instanceId("inactive-instance")
            .accessToken("inactive-token")
            .build();

        // Repository only returns active instances
        List<WhatsAppInstance> activeInstances = List.of(activeInstance);
        when(whatsappInstanceRepository.findByCompanyAndIsActiveTrue(testCompany))
            .thenReturn(activeInstances);

        // Act
        Optional<WhatsAppInstance> result = whatsappInstanceService.findActiveConnectedInstance(testCompany);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(activeInstance, result.get());
        assertTrue(result.get().getIsActive());
    }

    @Test
    void findInstanceForMessaging_WithValidPhoneNumber_ShouldReturnInstanceByPhone() {
        // Arrange
        String phoneNumber = "5511999888777";
        WhatsAppInstance instanceForPhone = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .phoneNumber(phoneNumber)
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
    void findInstanceForMessaging_WithInactivePhoneInstance_ShouldReturnEmpty() {
        // Arrange
        String phoneNumber = "5511999888777";
        WhatsAppInstance inactiveInstance = WhatsAppInstance.builder()
            .id(UUID.randomUUID())
            .company(testCompany)
            .phoneNumber(phoneNumber)
            .isActive(false)
            .instanceId("inactive-instance")
            .accessToken("inactive-token")
            .build();

        when(whatsappInstanceRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber))
            .thenReturn(Optional.empty()); // No active instance found

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