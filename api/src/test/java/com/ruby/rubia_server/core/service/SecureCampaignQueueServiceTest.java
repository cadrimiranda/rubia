package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecureCampaignQueueServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CampaignService campaignService;

    @Mock
    private CampaignContactService campaignContactService;

    @Mock
    private CampaignMessagingService campaignMessagingService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    @InjectMocks
    private SecureCampaignQueueService secureCampaignQueueService;

    private UUID campaignId;
    private UUID companyId;
    private String userId;
    private Campaign campaign;
    private Company company;
    private List<CampaignContact> pendingContacts;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        userId = "test-user-123";

        company = new Company();
        company.setId(companyId);
        company.setName("Test Company");

        campaign = new Campaign();
        campaign.setId(campaignId);
        campaign.setName("Test Campaign");
        campaign.setStatus(CampaignStatus.ACTIVE);
        campaign.setCompany(company);

        CampaignContact contact1 = new CampaignContact();
        contact1.setId(UUID.randomUUID());
        contact1.setStatus(CampaignContactStatus.PENDING);

        CampaignContact contact2 = new CampaignContact();
        contact2.setId(UUID.randomUUID());
        contact2.setStatus(CampaignContactStatus.PENDING);

        pendingContacts = List.of(contact1, contact2);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void enqueueCampaign_WithValidCampaign_ShouldAddToRedisQueue() throws Exception {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignContactService.findByCampaignIdAndStatus(campaignId, CampaignContactStatus.PENDING))
                .thenReturn(pendingContacts);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Act
        secureCampaignQueueService.enqueueCampaign(campaignId, companyId.toString(), userId);

        // Assert
        verify(campaignService).findById(campaignId);
        verify(campaignContactService).findByCampaignIdAndStatus(campaignId, CampaignContactStatus.PENDING);
        verify(valueOperations).set(anyString(), any(), eq(7L), eq(TimeUnit.DAYS));
        verify(zSetOperations, times(pendingContacts.size())).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void enqueueCampaign_WithInvalidCompany_ShouldThrowSecurityException() {
        // Arrange
        UUID differentCompanyId = UUID.randomUUID();
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () ->
                secureCampaignQueueService.enqueueCampaign(campaignId, differentCompanyId.toString(), userId)
        );
        
        assertTrue(exception.getMessage().contains("Acesso negado à campanha"));
    }

    @Test
    void enqueueCampaign_WithCampaignNotFound_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                secureCampaignQueueService.enqueueCampaign(campaignId, companyId.toString(), userId)
        );
        
        assertTrue(exception.getMessage().contains("Campanha não encontrada"));
    }

    @Test
    void enqueueCampaign_WithCampaignAlreadyInQueue_ShouldNotAddAgain() {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        // Act
        secureCampaignQueueService.enqueueCampaign(campaignId, companyId.toString(), userId);

        // Assert
        verify(campaignContactService, never()).findByCampaignIdAndStatus(any(), any());
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void enqueueCampaign_WithNoPendingContacts_ShouldNotAddToQueue() {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(campaignContactService.findByCampaignIdAndStatus(campaignId, CampaignContactStatus.PENDING))
                .thenReturn(List.of());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // Act
        secureCampaignQueueService.enqueueCampaign(campaignId, companyId.toString(), userId);

        // Assert
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void enqueueCampaign_WithInactiveCampaign_ShouldNotProcess() {
        // Arrange
        campaign.setStatus(CampaignStatus.PAUSED);
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));

        // Act
        secureCampaignQueueService.enqueueCampaign(campaignId, companyId.toString(), userId);

        // Assert
        verify(campaignContactService, never()).findByCampaignIdAndStatus(any(), any());
    }

    @Test
    void processMessageQueue_WithLockAcquired_ShouldProcessItems() throws Exception {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn("lock-value");
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenReturn(Set.of("{}"));
        when(objectMapper.readValue(anyString(), eq(SecureCampaignQueueService.SecureCampaignQueueItem.class)))
                .thenReturn(createTestQueueItem());

        // Mock campaign validation
        when(campaignService.findById(any())).thenReturn(Optional.of(campaign));
        when(valueOperations.get(contains("state"))).thenReturn(createTestCampaignState());
        when(campaignContactService.findById(any())).thenReturn(Optional.of(pendingContacts.get(0)));
        when(campaignMessagingService.sendSingleMessageAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(campaignContactService.update(any(), any())).thenReturn(Optional.of(pendingContacts.get(0)));

        // Act
        secureCampaignQueueService.processMessageQueue();

        // Assert
        verify(zSetOperations).rangeByScore(anyString(), anyDouble(), anyDouble());
        verify(campaignMessagingService).sendSingleMessageAsync(any());
        verify(zSetOperations).remove(anyString(), any());
    }

    @Test
    void processMessageQueue_WithLockNotAcquired_ShouldNotProcess() {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(false);

        // Act
        secureCampaignQueueService.processMessageQueue();

        // Assert
        verify(zSetOperations, never()).rangeByScore(anyString(), anyDouble(), anyDouble());
    }

    @Test
    void processMessageQueue_WithEmptyQueue_ShouldNotProcess() {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(valueOperations.get(anyString())).thenReturn("lock-value");
        when(zSetOperations.rangeByScore(anyString(), anyDouble(), anyDouble()))
                .thenReturn(Set.of());

        // Act
        secureCampaignQueueService.processMessageQueue();

        // Assert
        verify(campaignMessagingService, never()).sendSingleMessageAsync(any());
    }

    @Test
    void processSecureQueueItem_WithValidItem_ShouldProcessSuccessfully() throws Exception {
        // Arrange
        SecureCampaignQueueService.SecureCampaignQueueItem item = createTestQueueItem();
        
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(valueOperations.get(contains("state"))).thenReturn(createTestCampaignState());
        when(campaignContactService.findById(any())).thenReturn(Optional.of(pendingContacts.get(0)));
        when(campaignMessagingService.sendSingleMessageAsync(any()))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(campaignContactService.update(any(), any())).thenReturn(Optional.of(pendingContacts.get(0)));

        // Use reflection to call private method for testing
        var method = SecureCampaignQueueService.class.getDeclaredMethod("processSecureQueueItem", 
                SecureCampaignQueueService.SecureCampaignQueueItem.class);
        method.setAccessible(true);

        // Act
        boolean result = (boolean) method.invoke(secureCampaignQueueService, item);

        // Assert
        assertTrue(result);
        verify(campaignMessagingService).sendSingleMessageAsync(any());
        verify(campaignContactService).update(any(), any());
    }

    @Test
    void validateCampaignAccess_WithValidCompany_ShouldReturnTrue() throws Exception {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));

        // Use reflection to call private method
        var method = SecureCampaignQueueService.class.getDeclaredMethod("validateCampaignAccess", 
                UUID.class, String.class);
        method.setAccessible(true);

        // Act
        boolean result = (boolean) method.invoke(secureCampaignQueueService, campaignId, companyId.toString());

        // Assert
        assertTrue(result);
    }

    @Test
    void validateCampaignAccess_WithInvalidCompany_ShouldReturnFalse() throws Exception {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));

        // Use reflection to call private method
        var method = SecureCampaignQueueService.class.getDeclaredMethod("validateCampaignAccess", 
                UUID.class, String.class);
        method.setAccessible(true);

        // Act
        boolean result = (boolean) method.invoke(secureCampaignQueueService, campaignId, UUID.randomUUID().toString());

        // Assert
        assertFalse(result);
    }

    @Test
    void validateCampaignAccess_WithCampaignNotFound_ShouldReturnFalse() throws Exception {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.empty());

        // Use reflection to call private method
        var method = SecureCampaignQueueService.class.getDeclaredMethod("validateCampaignAccess", 
                UUID.class, String.class);
        method.setAccessible(true);

        // Act
        boolean result = (boolean) method.invoke(secureCampaignQueueService, campaignId, companyId.toString());

        // Assert
        assertFalse(result);
    }

    private SecureCampaignQueueService.SecureCampaignQueueItem createTestQueueItem() {
        return new SecureCampaignQueueService.SecureCampaignQueueItem(
                campaignId,
                pendingContacts.get(0).getId(),
                LocalDateTime.now(),
                1,
                companyId.toString(),
                userId
        );
    }

    private SecureCampaignQueueService.CampaignState createTestCampaignState() {
        SecureCampaignQueueService.CampaignState state = new SecureCampaignQueueService.CampaignState();
        state.setCampaignId(campaignId);
        state.setCompanyId(companyId.toString());
        state.setCreatedBy(userId);
        state.setStatus(CampaignStatus.ACTIVE);
        state.setTotalContacts(2);
        state.setProcessedContacts(0);
        state.setCreatedAt(LocalDateTime.now());
        return state;
    }
}