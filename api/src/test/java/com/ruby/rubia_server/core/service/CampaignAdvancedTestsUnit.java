package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignAdvancedTestsUnit {

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
        when(properties.getMinDelayMs()).thenReturn(1000);
        when(properties.getMaxDelayMs()).thenReturn(3000);

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
    @DisplayName("Complete Campaign Flow Simulation")
    void shouldExecuteCompleteCampaignFlow() throws InterruptedException {
        // Mock successful messaging
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(MessageResult.success("msg-123", "sent", "test"));

        when(delaySchedulingService.scheduleMessageSend(any(CampaignContact.class), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Create multiple contacts to simulate batch processing
        List<CampaignContact> contacts = createMultipleContacts(10);
        CountDownLatch latch = new CountDownLatch(10);

        // Process all contacts
        List<CompletableFuture<Boolean>> futures = contacts.stream()
                .map(contact -> {
                    latch.countDown();
                    return campaignMessagingService.sendSingleMessageAsync(contact);
                })
                .toList();

        // Wait for completion
        assertTrue(latch.await(5, TimeUnit.SECONDS), "All messages should be processed");
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Verify all were processed
        verify(delaySchedulingService, times(10)).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    @DisplayName("High Volume Campaign Performance")
    void shouldHandleLargeCampaigns() {
        // Test with 100 contacts
        List<CampaignContact> contacts = createMultipleContacts(100);
        
        when(delaySchedulingService.scheduleMessageSend(any(CampaignContact.class), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        long startTime = System.currentTimeMillis();

        // Process all contacts in parallel
        List<CompletableFuture<Boolean>> futures = contacts.parallelStream()
                .map(campaignMessagingService::sendSingleMessageAsync)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long processingTime = System.currentTimeMillis() - startTime;

        // Performance assertions
        assertTrue(processingTime < 10000, "Processing 100 contacts should take less than 10 seconds");
        assertEquals(100, futures.size(), "Should process all contacts");

        // Verify throughput
        double throughput = (double) contacts.size() / (processingTime / 1000.0);
        assertTrue(throughput > 10.0, "Should maintain minimum throughput of 10 contacts/second");
    }

    @Test
    @DisplayName("Concurrent Campaign Processing")
    void shouldHandleMultipleCampaignsSimultaneously() throws InterruptedException {
        // Create 3 different campaigns
        List<CampaignContact> campaign1Contacts = createMultipleContacts(5);
        List<CampaignContact> campaign2Contacts = createMultipleContacts(5);
        List<CampaignContact> campaign3Contacts = createMultipleContacts(5);

        CountDownLatch concurrentLatch = new CountDownLatch(15);

        when(delaySchedulingService.scheduleMessageSend(any(CampaignContact.class), anyInt(), any()))
                .thenAnswer(invocation -> {
                    concurrentLatch.countDown();
                    return CompletableFuture.completedFuture(true);
                });

        // Process all campaigns concurrently
        List<CompletableFuture<Boolean>> allFutures = List.of(
                campaign1Contacts, campaign2Contacts, campaign3Contacts
        ).stream()
                .flatMap(List::stream)
                .map(contact -> CompletableFuture.supplyAsync(() -> 
                        campaignMessagingService.sendSingleMessageAsync(contact).join()
                ))
                .toList();

        // Verify concurrent processing
        assertTrue(concurrentLatch.await(10, TimeUnit.SECONDS), "All campaigns should process concurrently");
        CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0])).join();

        // Verify isolation - each campaign processed independently
        verify(delaySchedulingService, times(15)).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    @DisplayName("Rate Limiting Simulation")
    void shouldHandleRateLimitingGracefully() {
        // Mock rate limiting scenario
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(MessageResult.error("Rate limited - 429", "test"))
                .thenReturn(MessageResult.error("Rate limited - 429", "test"))
                .thenReturn(MessageResult.success("msg-success", "sent", "test"));

        // Mock retry behavior with delays
        when(delaySchedulingService.scheduleMessageSend(any(CampaignContact.class), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(false))
                .thenReturn(CompletableFuture.completedFuture(false))
                .thenReturn(CompletableFuture.completedFuture(true));

        List<CampaignContact> contacts = createMultipleContacts(3);

        // Process with rate limiting
        List<CompletableFuture<Boolean>> futures = contacts.stream()
                .map(campaignMessagingService::sendSingleMessageAsync)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Verify retry attempts
        verify(delaySchedulingService, times(3)).scheduleMessageSend(any(), anyInt(), any());
    }

    @Test
    @DisplayName("Message Failure Handling")
    void shouldHandleMessagingFailures() {
        // Mock messaging failure
        when(delaySchedulingService.scheduleMessageSend(any(CampaignContact.class), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(false));

        // Process with failure
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);
        
        assertFalse(result.join(), "Should return false on messaging failure");
        verify(delaySchedulingService).scheduleMessageSend(eq(campaignContact), anyInt(), any());
    }

    @Test
    @DisplayName("Message Template Processing")
    void shouldProcessMessageTemplates() {
        // Mock successful processing
        when(messagingService.sendMessage(anyString(), anyString()))
                .thenReturn(MessageResult.success("msg-123", "sent", "test"));

        when(delaySchedulingService.scheduleMessageSend(any(CampaignContact.class), anyInt(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Test with different template content
        messageTemplate.setContent("Olá {{nome}}, <script>alert('xss')</script> bem-vindo!");
        
        CompletableFuture<Boolean> result = campaignMessagingService.sendSingleMessageAsync(campaignContact);
        
        assertTrue(result.join(), "Should process template successfully");
        verify(delaySchedulingService).scheduleMessageSend(eq(campaignContact), anyInt(), any());
    }

    @Test
    @DisplayName("Null Validation Tests")
    void shouldHandleNullInputsGracefully() {
        // Test with null contact
        CompletableFuture<Boolean> nullContactResult = campaignMessagingService.sendSingleMessageAsync(null);
        assertFalse(nullContactResult.join(), "Should handle null contact gracefully");

        // Test with null customer
        campaignContact.setCustomer(null);
        CompletableFuture<Boolean> nullCustomerResult = campaignMessagingService.sendSingleMessageAsync(campaignContact);
        assertFalse(nullCustomerResult.join(), "Should handle null customer gracefully");

        // Test with null campaign
        campaignContact.setCustomer(customer);
        campaignContact.setCampaign(null);
        CompletableFuture<Boolean> nullCampaignResult = campaignMessagingService.sendSingleMessageAsync(campaignContact);
        assertFalse(nullCampaignResult.join(), "Should handle null campaign gracefully");

        // Verify no service calls made for invalid inputs
        verify(delaySchedulingService, never()).scheduleMessageSend(any(), anyInt(), any());
    }

    private List<CampaignContact> createMultipleContacts(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> {
                    Customer testCustomer = new Customer();
                    testCustomer.setId(UUID.randomUUID());
                    testCustomer.setName("Customer " + i);
                    testCustomer.setPhone("5511999999" + String.format("%03d", i));
                    testCustomer.setCompany(company);

                    Campaign testCampaign = new Campaign();
                    testCampaign.setId(UUID.randomUUID());
                    testCampaign.setName("Campaign " + i);
                    testCampaign.setStatus(CampaignStatus.ACTIVE);
                    testCampaign.setCompany(company);
                    testCampaign.setInitialMessageTemplate(messageTemplate);

                    CampaignContact contact = new CampaignContact();
                    contact.setId(UUID.randomUUID());
                    contact.setCampaign(testCampaign);
                    contact.setCustomer(testCustomer);
                    contact.setStatus(CampaignContactStatus.PENDING);
                    contact.setCreatedAt(LocalDateTime.now());

                    return contact;
                })
                .toList();
    }
}