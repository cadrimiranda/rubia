package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.repository.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Campaign Queue Integration Tests")
class CampaignQueueIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CampaignQueueProcessor campaignQueueProcessor;

    @Autowired
    private CampaignProcessingService campaignProcessingService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CampaignContactRepository campaignContactRepository;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockBean
    private MessagingService messagingService;

    private Company company;
    private CompanyGroup companyGroup;
    private MessageTemplate messageTemplate;

    private static final String QUEUE_KEY = "rubia:campaign:queue";
    private static final String PROCESSING_KEY = "rubia:campaign:processing";
    private static final String ERROR_KEY = "rubia:campaign:error";

    @BeforeEach
    void setUp() {
        // Clear Redis queues
        redisTemplate.delete(QUEUE_KEY);
        redisTemplate.delete(PROCESSING_KEY);
        redisTemplate.delete(ERROR_KEY);

        // Create test data
        companyGroup = new CompanyGroup();
        companyGroup.setName("Test Group " + System.nanoTime());
        companyGroup = companyGroupRepository.save(companyGroup);

        company = new Company();
        company.setName("Test Company");
        company.setSlug("test-company-" + System.nanoTime());
        company.setContactPhone("5511999999999");
        company.setCompanyGroup(companyGroup);
        company = companyRepository.save(company);

        messageTemplate = new MessageTemplate();
        messageTemplate.setName("Test Template");
        messageTemplate.setContent("Olá {{nome}}, bem-vindo à nossa campanha!");
        messageTemplate.setCompany(company);
        messageTemplate = messageTemplateRepository.save(messageTemplate);

        // Mock messaging service to always succeed
        when(messagingService.sendMessage(anyString(), anyString(), any(Company.class)))
                .thenReturn(MessageResult.builder()
                        .success(true)
                        .messageId("msg-" + UUID.randomUUID())
                        .status("sent")
                        .provider("test")
                        .build());
    }

    @Test
    @DisplayName("Should process complete queue workflow end-to-end")
    @Transactional
    void shouldProcessCompleteQueueWorkflowEndToEnd() throws Exception {
        // Given - Create campaign with contacts
        Campaign campaign = createTestCampaign();
        List<Customer> customers = createTestCustomers(5);
        List<CampaignContact> contacts = createCampaignContacts(campaign, customers);

        // Add contacts to Redis queue manually
        enqueueCampaignContacts(campaign, contacts);

        // Verify queue has items
        Long queueSize = redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE);
        assertEquals(5L, queueSize);

        CountDownLatch processedLatch = new CountDownLatch(5);
        when(messagingService.sendMessage(anyString(), anyString(), any(Company.class)))
                .thenAnswer(invocation -> {
                    processedLatch.countDown();
                    return MessageResult.builder()
                            .success(true)
                            .messageId("msg-" + UUID.randomUUID())
                            .status("sent")
                            .provider("test")
                            .build();
                });

        // When - Process queue
        campaignQueueProcessor.processCampaignQueue();

        // Wait for processing
        assertTrue(processedLatch.await(30, TimeUnit.SECONDS), 
                  "All messages should be processed within 30 seconds");

        // Then - Verify results
        verify(messagingService, times(5)).sendMessage(anyString(), anyString(), any(Company.class));
        
        // Verify metrics were updated
        assertTrue(meterRegistry.get("campaign.messages.total").counter().count() > 0);
    }

    @Test
    @DisplayName("Should handle Redis connection failures gracefully")
    void shouldHandleRedisConnectionFailuresGracefully() {
        // Given - Create campaign but simulate Redis failure
        Campaign campaign = createTestCampaign();
        
        // Mock Redis operations to throw exceptions
        when(redisTemplate.opsForZSet()).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - Should not crash the application
        assertDoesNotThrow(() -> {
            campaignQueueProcessor.processCampaignQueue();
        });

        // Verify metrics still work
        Map<String, Object> status = campaignQueueProcessor.getProcessorStatus();
        assertNotNull(status.get("error"));
    }

    @Test
    @DisplayName("Should recover stuck messages after timeout")
    void shouldRecoverStuckMessagesAfterTimeout() throws Exception {
        // Given - Create stuck message in processing queue
        Campaign campaign = createTestCampaign();
        List<Customer> customers = createTestCustomers(1);
        CampaignContact contact = createCampaignContacts(campaign, customers).get(0);

        // Create stuck queue item with old timestamp
        CampaignQueueProcessor.CampaignQueueItem stuckItem = 
            new CampaignQueueProcessor.CampaignQueueItem(
                campaign.getId(), contact.getId(), company.getId().toString());
        stuckItem.setProcessingStartedAt(System.currentTimeMillis() - 400000L); // 6+ minutes ago

        String stuckItemJson = new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(stuckItem);
        
        // Add to processing queue
        redisTemplate.opsForList().rightPush(PROCESSING_KEY, stuckItemJson);

        // Verify it's in processing queue
        assertEquals(1L, redisTemplate.opsForList().size(PROCESSING_KEY));
        assertEquals(0L, redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE));

        // When - Run recovery
        campaignQueueProcessor.recoverStuckMessages();

        // Then - Should move back to main queue
        assertEquals(0L, redisTemplate.opsForList().size(PROCESSING_KEY));
        assertEquals(1L, redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE));
    }

    @Test
    @DisplayName("Should maintain concurrency limits under load")
    void shouldMaintainConcurrencyLimitsUnderLoad() throws Exception {
        // Given - Create many contacts to test concurrency
        Campaign campaign = createTestCampaign();
        List<Customer> customers = createTestCustomers(20);
        List<CampaignContact> contacts = createCampaignContacts(campaign, customers);
        
        enqueueCampaignContacts(campaign, contacts);

        CountDownLatch concurrencyLatch = new CountDownLatch(20);
        Set<Thread> processingThreads = Collections.synchronizedSet(new HashSet<>());

        // Mock to track concurrent executions
        when(messagingService.sendMessage(anyString(), anyString(), any(Company.class)))
                .thenAnswer(invocation -> {
                    processingThreads.add(Thread.currentThread());
                    
                    // Simulate processing time
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    
                    concurrencyLatch.countDown();
                    return MessageResult.builder()
                            .success(true)
                            .messageId("msg-" + UUID.randomUUID())
                            .status("sent")
                            .provider("test")
                            .build();
                });

        // When - Process with multiple iterations to simulate load
        for (int i = 0; i < 5; i++) {
            campaignQueueProcessor.processCampaignQueue();
            Thread.sleep(100); // Small delay between batches
        }

        // Wait for processing
        assertTrue(concurrencyLatch.await(60, TimeUnit.SECONDS), 
                  "All messages should be processed within 60 seconds");

        // Then - Verify concurrency was respected
        verify(messagingService, times(20)).sendMessage(anyString(), anyString(), any(Company.class));
        assertTrue(processingThreads.size() <= 10, "Should not exceed concurrency limit of 10 threads");
    }

    @Test
    @DisplayName("Should handle mixed success and failure scenarios")
    void shouldHandleMixedSuccessAndFailureScenarios() throws Exception {
        // Given
        Campaign campaign = createTestCampaign();
        List<Customer> customers = createTestCustomers(6);
        List<CampaignContact> contacts = createCampaignContacts(campaign, customers);
        
        enqueueCampaignContacts(campaign, contacts);

        CountDownLatch mixedResultsLatch = new CountDownLatch(6);

        // Mock alternating success/failure
        when(messagingService.sendMessage(anyString(), anyString(), any(Company.class)))
                .thenReturn(MessageResult.success("msg-1", "sent", "test"))
                .thenReturn(MessageResult.error("Temporary failure", "test"))
                .thenReturn(MessageResult.success("msg-2", "sent", "test"))
                .thenReturn(MessageResult.error("Rate limited", "test"))
                .thenReturn(MessageResult.success("msg-3", "sent", "test"))
                .thenReturn(MessageResult.error("Service unavailable", "test"));

        // When
        campaignQueueProcessor.processCampaignQueue();

        // Then - Should handle all scenarios gracefully
        verify(messagingService, times(6)).sendMessage(anyString(), anyString(), any(Company.class));

        // Verify error items were moved to error queue
        Long errorQueueSize = redisTemplate.opsForList().size(ERROR_KEY);
        assertEquals(3L, errorQueueSize, "Failed messages should be in error queue");
    }

    @Test
    @DisplayName("Should provide accurate status and metrics")
    void shouldProvideAccurateStatusAndMetrics() throws Exception {
        // Given
        Campaign campaign = createTestCampaign();
        List<Customer> customers = createTestCustomers(3);
        List<CampaignContact> contacts = createCampaignContacts(campaign, customers);
        
        enqueueCampaignContacts(campaign, contacts);

        // Add some items to processing and error queues for testing
        redisTemplate.opsForList().rightPush(PROCESSING_KEY, "processing-item-1");
        redisTemplate.opsForList().rightPush(ERROR_KEY, "error-item-1");

        // When
        Map<String, Object> status = campaignQueueProcessor.getProcessorStatus();

        // Then
        assertEquals(3L, status.get("queueSize"));
        assertEquals(1L, status.get("processingSize"));
        assertEquals(1L, status.get("errorSize"));
        assertNotNull(status.get("availableConcurrency"));
        assertNotNull(status.get("maxConcurrency"));
        assertNotNull(status.get("canProcessQueue"));
    }

    @Test
    @DisplayName("Should handle shutdown gracefully")
    void shouldHandleShutdownGracefully() throws Exception {
        // Given - Items in processing
        Campaign campaign = createTestCampaign();
        List<Customer> customers = createTestCustomers(2);
        List<CampaignContact> contacts = createCampaignContacts(campaign, customers);
        
        CampaignQueueProcessor.CampaignQueueItem item1 = 
            new CampaignQueueProcessor.CampaignQueueItem(
                campaign.getId(), contacts.get(0).getId(), company.getId().toString());
        CampaignQueueProcessor.CampaignQueueItem item2 = 
            new CampaignQueueProcessor.CampaignQueueItem(
                campaign.getId(), contacts.get(1).getId(), company.getId().toString());

        String item1Json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(item1);
        String item2Json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(item2);

        redisTemplate.opsForList().rightPush(PROCESSING_KEY, item1Json);
        redisTemplate.opsForList().rightPush(PROCESSING_KEY, item2Json);

        assertEquals(2L, redisTemplate.opsForList().size(PROCESSING_KEY));
        assertEquals(0L, redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE));

        // When - Shutdown
        campaignQueueProcessor.shutdown();

        // Then - Items should be moved back to main queue
        assertEquals(0L, redisTemplate.opsForList().size(PROCESSING_KEY));
        assertEquals(2L, redisTemplate.opsForZSet().count(QUEUE_KEY, 0, Double.MAX_VALUE));
    }

    private Campaign createTestCampaign() {
        Campaign campaign = new Campaign();
        campaign.setName("Integration Test Campaign " + System.nanoTime());
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setCompany(company);
        campaign.setInitialMessageTemplate(messageTemplate);
        return campaignRepository.save(campaign);
    }

    private List<Customer> createTestCustomers(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Customer customer = new Customer();
            customer.setName("Customer " + i);
            customer.setPhone("551199999" + String.format("%04d", i));
            customer.setCompany(company);
            customers.add(customerRepository.save(customer));
        }
        return customers;
    }

    private List<CampaignContact> createCampaignContacts(Campaign campaign, List<Customer> customers) {
        List<CampaignContact> contacts = new ArrayList<>();
        for (Customer customer : customers) {
            CampaignContact contact = new CampaignContact();
            contact.setCampaign(campaign);
            contact.setCustomer(customer);
            contact.setStatus(CampaignContactStatus.PENDING);
            contacts.add(campaignContactRepository.save(contact));
        }
        return contacts;
    }

    private void enqueueCampaignContacts(Campaign campaign, List<CampaignContact> contacts) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        
        long baseTimestamp = System.currentTimeMillis();
        
        for (int i = 0; i < contacts.size(); i++) {
            CampaignContact contact = contacts.get(i);
            CampaignQueueProcessor.CampaignQueueItem queueItem = 
                new CampaignQueueProcessor.CampaignQueueItem(
                    campaign.getId(),
                    contact.getId(),
                    company.getId().toString()
                );
            
            String itemJson = objectMapper.writeValueAsString(queueItem);
            long timestamp = baseTimestamp + (i * 1000);
            redisTemplate.opsForZSet().add(QUEUE_KEY, itemJson, timestamp);
        }
    }
}