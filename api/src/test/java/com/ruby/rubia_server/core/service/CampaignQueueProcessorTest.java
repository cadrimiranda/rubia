package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ListOperations;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampaignQueueProcessor Unit Tests")
class CampaignQueueProcessorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private CampaignMessagingService messageService;
    
    @Mock
    private CampaignContactService campaignContactService;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;
    
    @Mock
    private ListOperations<String, Object> listOperations;

    private ObjectMapper objectMapper;
    private Semaphore concurrencyLimiter;
    private Semaphore queueProcessingLimiter;
    private MeterRegistry meterRegistry;
    private CampaignQueueProcessor processor;

    private static final String QUEUE_KEY = "rubia:campaign:queue";
    private static final String PROCESSING_KEY = "rubia:campaign:processing";
    private static final String ERROR_KEY = "rubia:campaign:error";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        concurrencyLimiter = new Semaphore(10);
        queueProcessingLimiter = new Semaphore(1);
        meterRegistry = new SimpleMeterRegistry();

        // Setup Redis template mocks
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);

        processor = new CampaignQueueProcessor(
                redisTemplate,
                messageService,
                campaignContactService,
                objectMapper,
                concurrencyLimiter,
                queueProcessingLimiter,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Should process queue items successfully")
    void shouldProcessQueueItemsSuccessfully() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignQueueProcessor.CampaignQueueItem queueItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        String itemJson = objectMapper.writeValueAsString(queueItem);

        CampaignContact mockContact = createMockCampaignContact(contactId, campaignId);

        // Setup mocks
        when(campaignContactService.findByIdWithRelations(contactId)).thenReturn(Optional.of(mockContact));
        when(messageService.sendSingleMessageAsync(mockContact))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(zSetOperations.count(QUEUE_KEY, 0, Double.MAX_VALUE)).thenReturn(1L);
        when(listOperations.size(PROCESSING_KEY)).thenReturn(0L);
        when(listOperations.size(ERROR_KEY)).thenReturn(0L);

        // Mock Redis pipeline operations
        List<Object> pipelineResults = Arrays.asList(itemJson);
        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(pipelineResults);

        // When
        processor.processCampaignQueue();

        // Then
        verify(campaignContactService).findByIdWithRelations(contactId);
        verify(messageService).sendSingleMessageAsync(mockContact);
        verify(listOperations).rightPush(PROCESSING_KEY, itemJson);
    }

    @Test
    @DisplayName("Should handle contact not found gracefully")
    void shouldHandleContactNotFoundGracefully() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignQueueProcessor.CampaignQueueItem queueItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        String itemJson = objectMapper.writeValueAsString(queueItem);

        // Setup mocks - contact not found
        when(campaignContactService.findByIdWithRelations(contactId)).thenReturn(Optional.empty());
        when(zSetOperations.count(QUEUE_KEY, 0, Double.MAX_VALUE)).thenReturn(1L);
        when(listOperations.size(PROCESSING_KEY)).thenReturn(0L);
        when(listOperations.size(ERROR_KEY)).thenReturn(0L);

        // Mock Redis pipeline operations
        List<Object> pipelineResults = Arrays.asList(itemJson);
        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(pipelineResults);

        // When
        processor.processCampaignQueue();

        // Then
        verify(campaignContactService).findByIdWithRelations(contactId);
        verify(messageService, never()).sendSingleMessageAsync(any());
        verify(listOperations).remove(PROCESSING_KEY, 1, itemJson);
    }

    @Test
    @DisplayName("Should skip non-pending contacts")
    void shouldSkipNonPendingContacts() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignQueueProcessor.CampaignQueueItem queueItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        String itemJson = objectMapper.writeValueAsString(queueItem);

        CampaignContact mockContact = createMockCampaignContact(contactId, campaignId);
        mockContact.setStatus(CampaignContactStatus.SENT); // Already sent

        // Setup mocks
        when(campaignContactService.findByIdWithRelations(contactId)).thenReturn(Optional.of(mockContact));
        when(zSetOperations.count(QUEUE_KEY, 0, Double.MAX_VALUE)).thenReturn(1L);
        when(listOperations.size(PROCESSING_KEY)).thenReturn(0L);
        when(listOperations.size(ERROR_KEY)).thenReturn(0L);

        // Mock Redis pipeline operations
        List<Object> pipelineResults = Arrays.asList(itemJson);
        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(pipelineResults);

        // When
        processor.processCampaignQueue();

        // Then
        verify(campaignContactService).findByIdWithRelations(contactId);
        verify(messageService, never()).sendSingleMessageAsync(any());
        verify(listOperations).remove(PROCESSING_KEY, 1, itemJson);
    }

    @Test
    @DisplayName("Should handle concurrency limits correctly")
    void shouldHandleConcurrencyLimitsCorrectly() {
        // Given - exhaust all permits
        for (int i = 0; i < 10; i++) {
            concurrencyLimiter.tryAcquire();
        }

        // When
        processor.processCampaignQueue();

        // Then - should not process any items due to concurrency limit
        // The processor should not call Redis operations when concurrency is exhausted
        assertEquals(0, concurrencyLimiter.availablePermits());
        
        // Verify that no Redis operations were called because of concurrency limit
        verify(redisTemplate, never()).executePipelined(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    @Test
    @DisplayName("Should recover stuck messages based on timeout")
    void shouldRecoverStuckMessagesBasedOnTimeout() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        // Create stuck item with old timestamp
        CampaignQueueProcessor.CampaignQueueItem stuckItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        stuckItem.setProcessingStartedAt(System.currentTimeMillis() - 400000L); // 6+ minutes ago
        String stuckItemJson = objectMapper.writeValueAsString(stuckItem);

        // Setup mocks
        when(listOperations.range(PROCESSING_KEY, 0, -1))
                .thenReturn(Arrays.asList(stuckItemJson));

        // When
        processor.recoverStuckMessages();

        // Then
        verify(listOperations).remove(PROCESSING_KEY, 1, stuckItemJson);
        verify(zSetOperations).add(eq(QUEUE_KEY), eq(stuckItemJson), anyDouble());
    }

    @Test
    @DisplayName("Should not recover fresh messages")
    void shouldNotRecoverFreshMessages() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        // Create fresh item with recent timestamp
        CampaignQueueProcessor.CampaignQueueItem freshItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        freshItem.setProcessingStartedAt(System.currentTimeMillis() - 60000L); // 1 minute ago
        String freshItemJson = objectMapper.writeValueAsString(freshItem);

        // Setup mocks
        when(listOperations.range(PROCESSING_KEY, 0, -1))
                .thenReturn(Arrays.asList(freshItemJson));

        // When
        processor.recoverStuckMessages();

        // Then
        verify(listOperations, never()).remove(eq(PROCESSING_KEY), eq(1), eq(freshItemJson));
        verify(zSetOperations, never()).add(eq(QUEUE_KEY), eq(freshItemJson), anyDouble());
    }

    @Test
    @DisplayName("Should update metrics correctly")
    void shouldUpdateMetricsCorrectly() throws Exception {
        // Given - Create a scenario where items are processed to trigger updateMetrics()
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignQueueProcessor.CampaignQueueItem queueItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        String itemJson = objectMapper.writeValueAsString(queueItem);

        CampaignContact mockContact = createMockCampaignContact(contactId, campaignId);

        // Setup mocks
        when(campaignContactService.findByIdWithRelations(contactId)).thenReturn(Optional.of(mockContact));
        when(messageService.sendSingleMessageAsync(mockContact))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(zSetOperations.count(QUEUE_KEY, 0, Double.MAX_VALUE)).thenReturn(5L);
        when(listOperations.size(PROCESSING_KEY)).thenReturn(2L);
        when(listOperations.size(ERROR_KEY)).thenReturn(1L);

        // Mock Redis pipeline operations with at least one item
        List<Object> pipelineResults = Arrays.asList(itemJson);
        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(pipelineResults);

        // When
        processor.processCampaignQueue();

        // Then - Verify that Redis operations were called for metrics during updateMetrics()
        verify(zSetOperations).count(QUEUE_KEY, 0, Double.MAX_VALUE);
        verify(listOperations).size(PROCESSING_KEY);
        verify(listOperations).size(ERROR_KEY);
        
        // Verify that the meterRegistry was used to record gauge metrics
        assertTrue(meterRegistry.getMeters().stream()
            .anyMatch(meter -> meter.getId().getName().equals("campaign.queue.size")));
        assertTrue(meterRegistry.getMeters().stream()
            .anyMatch(meter -> meter.getId().getName().equals("campaign.processing.size")));
        assertTrue(meterRegistry.getMeters().stream()
            .anyMatch(meter -> meter.getId().getName().equals("campaign.error.size")));
    }

    @Test
    @DisplayName("Should handle processing errors gracefully")
    void shouldHandleProcessingErrorsGracefully() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "company-123";

        CampaignQueueProcessor.CampaignQueueItem queueItem = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        String itemJson = objectMapper.writeValueAsString(queueItem);

        CampaignContact mockContact = createMockCampaignContact(contactId, campaignId);

        // Setup mocks - messaging service throws exception
        when(campaignContactService.findByIdWithRelations(contactId)).thenReturn(Optional.of(mockContact));
        when(messageService.sendSingleMessageAsync(mockContact))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Messaging failed")));

        when(zSetOperations.count(QUEUE_KEY, 0, Double.MAX_VALUE)).thenReturn(1L);
        when(listOperations.size(PROCESSING_KEY)).thenReturn(0L);
        when(listOperations.size(ERROR_KEY)).thenReturn(0L);

        // Mock Redis pipeline operations
        List<Object> pipelineResults = Arrays.asList(itemJson);
        when(redisTemplate.executePipelined(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(pipelineResults);

        // When
        processor.processCampaignQueue();

        // Then
        verify(campaignContactService).findByIdWithRelations(contactId);
        verify(messageService).sendSingleMessageAsync(mockContact);
        verify(listOperations).remove(PROCESSING_KEY, 1, itemJson);
        verify(listOperations).rightPush(ERROR_KEY, itemJson);
    }

    @Test
    @DisplayName("Should validate CampaignQueueItem serialization")
    void shouldValidateCampaignQueueItemSerialization() throws Exception {
        // Given
        UUID campaignId = UUID.randomUUID();
        UUID contactId = UUID.randomUUID();
        String companyId = "test-company";
        long startTime = System.currentTimeMillis();

        CampaignQueueProcessor.CampaignQueueItem original = 
            new CampaignQueueProcessor.CampaignQueueItem(campaignId, contactId, companyId);
        original.setProcessingStartedAt(startTime);

        // When
        String json = objectMapper.writeValueAsString(original);
        CampaignQueueProcessor.CampaignQueueItem deserialized = 
            objectMapper.readValue(json, CampaignQueueProcessor.CampaignQueueItem.class);

        // Then
        assertEquals(campaignId, deserialized.getCampaignId());
        assertEquals(contactId, deserialized.getCampaignContactId());
        assertEquals(companyId, deserialized.getCompanyId());
        assertEquals(startTime, deserialized.getProcessingStartedAt());
    }

    @Test
    @DisplayName("Should return processor status correctly")
    void shouldReturnProcessorStatusCorrectly() {
        // Given
        when(zSetOperations.count(QUEUE_KEY, 0, Double.MAX_VALUE)).thenReturn(3L);
        when(listOperations.size(PROCESSING_KEY)).thenReturn(1L);
        when(listOperations.size(ERROR_KEY)).thenReturn(0L);

        // When
        Map<String, Object> status = processor.getProcessorStatus();

        // Then
        assertEquals(false, status.get("isProcessing"));
        assertEquals(0, status.get("activeTasks"));
        assertEquals(3L, status.get("queueSize"));
        assertEquals(1L, status.get("processingSize"));
        assertEquals(0L, status.get("errorSize"));
        assertEquals(10, status.get("availableConcurrency"));
        assertEquals(50, status.get("maxConcurrency"));
        assertEquals(true, status.get("canProcessQueue"));
    }

    private CampaignContact createMockCampaignContact(UUID contactId, UUID campaignId) {
        CampaignContact contact = new CampaignContact();
        contact.setId(contactId);
        contact.setStatus(CampaignContactStatus.PENDING);

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setName("Test Customer");
        customer.setPhone("5511999999999");

        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setName("Test Company");
        customer.setCompany(company);

        Campaign campaign = new Campaign();
        campaign.setId(campaignId);
        campaign.setName("Test Campaign");

        contact.setCustomer(customer);
        contact.setCampaign(campaign);

        return contact;
    }
}