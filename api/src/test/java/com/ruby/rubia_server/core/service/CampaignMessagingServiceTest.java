package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignMessagingServiceTest {

    @Mock
    private MessagingService messagingService;

    @InjectMocks
    private CampaignMessagingService campaignMessagingService;

    private CampaignContact campaignContact;
    private Campaign campaign;
    private Customer customer;
    private MessageTemplate messageTemplate;
    private Company company;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Test Customer");
        customer.setPhone("5511999999999");
        customer.setCompany(company);

        messageTemplate = new MessageTemplate();
        messageTemplate.setId(UUID.randomUUID());
        messageTemplate.setName("Test Template");
        messageTemplate.setContent("Olá {{nome}}, bem-vindo à nossa campanha!");
        messageTemplate.setCompany(company);

        campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setName("Test Campaign");
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setCompany(company);
        campaign.setInitialMessageTemplate(messageTemplate);

        campaignContact = new CampaignContact();
        campaignContact.setId(UUID.randomUUID());
        campaignContact.setCampaign(campaign);
        campaignContact.setCustomer(customer);
        campaignContact.setStatus(CampaignContactStatus.PENDING);
        campaignContact.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void sendSingleMessage_WithValidCampaignContact_ShouldReturnTrue() {
        // Arrange
        MessageResult successResult = MessageResult.success("msg-123", "sent", "test-instance");
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(successResult);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertTrue(result);
        verify(messagingService).sendMessage(
                eq(customer.getPhone()),
                contains("Olá Test Customer")
        );
    }

    @Test
    void sendSingleMessage_WithMessagingServiceFailure_ShouldReturnFalse() {
        // Arrange
        MessageResult failureResult = MessageResult.error("Failed to send", "test-instance");
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(failureResult);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithMessagingServiceException_ShouldReturnFalse() {
        // Arrange
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenThrow(new RuntimeException("Erro no serviço de mensagens"));

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithNullCampaignContact_ShouldReturnFalse() {
        // Act
        boolean result = campaignMessagingService.sendSingleMessage(null);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithNullCustomer_ShouldReturnFalse() {
        // Arrange
        campaignContact.setCustomer(null);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithNullCampaign_ShouldReturnFalse() {
        // Arrange
        campaignContact.setCampaign(null);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithNullMessageTemplate_ShouldReturnFalse() {
        // Arrange
        campaign.setInitialMessageTemplate(null);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithEmptyCustomerPhone_ShouldReturnFalse() {
        // Arrange
        customer.setPhone("");

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithNullCustomerPhone_ShouldReturnFalse() {
        // Arrange
        customer.setPhone(null);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithEmptyTemplateContent_ShouldReturnFalse() {
        // Arrange
        messageTemplate.setContent("");

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithNullTemplateContent_ShouldReturnFalse() {
        // Arrange
        messageTemplate.setContent(null);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertFalse(result);
        verify(messagingService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithTemplateVariableReplacement_ShouldReplaceCorrectly() {
        // Arrange
        messageTemplate.setContent("Olá {{nome}}, sua idade é {{idade}} anos");
        customer.setName("João Silva");
        
        MessageResult successResult = MessageResult.success("msg-123", "sent", "test-instance");
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(successResult);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertTrue(result);
        verify(messagingService).sendMessage(
                eq(customer.getPhone()),
                eq("Olá João Silva, sua idade é {{idade}} anos") // {{nome}} replaced, {{idade}} kept
        );
    }


    @Test
    void sendSingleMessage_WithNullCustomerName_ShouldReplaceWithEmptyString() {
        // Arrange
        customer.setName(null);
        
        MessageResult successResult = MessageResult.success("msg-123", "sent", "test-instance");
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(successResult);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertTrue(result);
        verify(messagingService).sendMessage(
                eq(customer.getPhone()),
                eq("Olá , bem-vindo à nossa campanha!") // {{nome}} replaced with empty
        );
    }

    @Test
    void sendSingleMessage_WithLongDelay_ShouldComplete() {
        // Arrange
        MessageResult successResult = MessageResult.success("msg-123", "sent", "test-instance");
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(successResult);

        long startTime = System.currentTimeMillis();

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(result);
        assertTrue(duration >= 1000, "Should respect minimum 1 second delay"); // Check minimum delay
        assertTrue(duration <= 65000, "Should not exceed maximum delay significantly"); // Check reasonable maximum
        
        verify(messagingService).sendMessage(anyString(), anyString());
    }

    @Test
    void sendSingleMessage_WithValidData_ShouldLogCorrectly() {
        // Arrange
        MessageResult successResult = MessageResult.success("msg-123", "sent", "test-instance");
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(successResult);

        // Act
        boolean result = campaignMessagingService.sendSingleMessage(campaignContact);

        // Assert
        assertTrue(result);
        
        // Verify that the service was called with correct parameters
        verify(messagingService).sendMessage(
                eq("5511999999999"),
                eq("Olá Test Customer, bem-vindo à nossa campanha!")
        );
    }
}