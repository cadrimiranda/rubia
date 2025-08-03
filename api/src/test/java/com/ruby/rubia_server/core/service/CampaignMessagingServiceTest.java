package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignMessagingServiceTest {

    @Mock
    private MessagingService messagingService;

    @Mock
    private CampaignDelaySchedulingService delaySchedulingService;

    @Mock
    private CampaignMessagingProperties properties;

    @InjectMocks
    private CampaignMessagingService campaignMessagingService;

    private CampaignContact campaignContact;
    private Campaign campaign;
    private Customer customer;
    private MessageTemplate messageTemplate;
    private Company company;

    @BeforeEach
    void setUp() {
        // Mock properties with default values
        when(properties.getMinDelayMs()).thenReturn(30000);
        when(properties.getMaxDelayMs()).thenReturn(60000);
        when(properties.getMessageTimeout()).thenReturn(Duration.ofSeconds(30));
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
    void sendSingleMessageAsync_WithValidCampaignContact_ShouldReturnCompletedFuture() {
        // Arrange
        CompletableFuture<Boolean> expectedFuture = CompletableFuture.completedFuture(true);
        when(delaySchedulingService.scheduleMessageSend(eq(campaignContact), anyInt(), any()))
                .thenReturn(expectedFuture);

        // Act
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertTrue(result.join());
        verify(delaySchedulingService).scheduleMessageSend(eq(campaignContact), anyInt(), any());
    }

    @Test
    void sendSingleMessageAsync_WithNullCampaignContact_ShouldReturnFailedFuture() {
        // Act
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.join());
        verify(delaySchedulingService, never()).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    void sendSingleMessageAsync_WithNullCustomer_ShouldReturnFailedFuture() {
        // Arrange
        campaignContact.setCustomer(null);

        // Act
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.join());
        verify(delaySchedulingService, never()).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    void sendSingleMessageAsync_WithNullCampaign_ShouldReturnFailedFuture() {
        // Arrange
        campaignContact.setCampaign(null);

        // Act
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.join());
        verify(delaySchedulingService, never()).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    void sendSingleMessageAsync_WithEmptyCustomerPhone_ShouldReturnFailedFuture() {
        // Arrange
        customer.setPhone("");

        // Act
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.join());
        verify(delaySchedulingService, never()).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    void sendSingleMessageAsync_WithNullMessageTemplate_ShouldReturnFailedFuture() {
        // Arrange
        campaign.setInitialMessageTemplate(null);

        // Act
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);

        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.join());
        verify(delaySchedulingService, never()).scheduleMessageSend(any(), anyInt(), any());
    }
}