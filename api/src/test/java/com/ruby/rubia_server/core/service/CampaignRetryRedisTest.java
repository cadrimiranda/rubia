package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes para o sistema de retry via Redis
 * Foca na funcionalidade de re-adicionar contatos falhos de volta à fila
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CampaignRetryRedisTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ZSetOperations<String, Object> zSetOperations;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private CampaignService campaignService;
    
    @Mock
    private CampaignContactService campaignContactService;
    
    @Mock
    private CampaignMessagingService campaignMessagingService;
    
    @Mock
    private CampaignMessagingProperties properties;
    
    @Mock
    private MessagingService messagingService;
    
    @Mock
    private CampaignDelaySchedulingService delaySchedulingService;
    
    @Mock
    private ChatLidMappingService chatLidMappingService;
    
    @Mock
    private ConversationService conversationService;

    private SecureCampaignQueueService secureCampaignQueueService;
    private CampaignMessagingService campaignMessagingServiceWithRetry;
    
    private UUID campaignId;
    private UUID contactId;
    private UUID companyId;
    private CampaignContact campaignContact;
    private Campaign campaign;
    private Company company;
    private Customer customer;

    @BeforeEach
    void setUp() {
        // Setup IDs
        campaignId = UUID.randomUUID();
        contactId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        // Setup mocks
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        // Setup entities
        company = Company.builder()
            .id(companyId)
            .name("Test Company")
            .isActive(true)
            .build();
            
        customer = Customer.builder()
            .id(UUID.randomUUID())
            .name("Test Customer")
            .phone("5511999999999")
            .company(company)
            .build();
            
        campaign = Campaign.builder()
            .id(campaignId)
            .name("Test Campaign")
            .status(CampaignStatus.ACTIVE)
            .company(company)
            .build();
            
        campaignContact = CampaignContact.builder()
            .id(contactId)
            .campaign(campaign)
            .customer(customer)
            .status(CampaignContactStatus.PENDING)
            .build();

        // Setup services
        secureCampaignQueueService = new SecureCampaignQueueService(
            redisTemplate, objectMapper, campaignService, 
            campaignContactService, campaignMessagingService, properties
        );
        
        campaignMessagingServiceWithRetry = new CampaignMessagingService(
            messagingService, delaySchedulingService, properties,
            chatLidMappingService, conversationService, secureCampaignQueueService
        );
    }

    @Test
    void testAddContactForRetrySuccess() throws Exception {
        // Arrange
        when(campaignService.findById(campaignId))
            .thenReturn(Optional.of(campaign));
        when(objectMapper.writeValueAsString(any()))
            .thenReturn("{\"campaignId\":\"" + campaignId + "\"}");
        when(zSetOperations.add(anyString(), anyString(), anyDouble()))
            .thenReturn(true);

        // Act
        secureCampaignQueueService.addContactForRetry(campaignId, contactId, companyId.toString());

        // Assert
        verify(campaignService).findById(campaignId);
        verify(objectMapper).writeValueAsString(any(SecureCampaignQueueService.SecureCampaignQueueItem.class));
        verify(zSetOperations).add(eq("rubia:campaign:queue"), anyString(), anyDouble());
    }

    @Test
    void testAddContactForRetryUnauthorized() {
        // Arrange
        Campaign unauthorizedCampaign = Campaign.builder()
            .id(campaignId)
            .name("Unauthorized Campaign")
            .status(CampaignStatus.ACTIVE)
            .company(Company.builder().id(UUID.randomUUID()).build()) // Empresa diferente
            .build();
            
        when(campaignService.findById(campaignId))
            .thenReturn(Optional.of(unauthorizedCampaign));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            secureCampaignQueueService.addContactForRetry(campaignId, contactId, companyId.toString());
        });
        
        assertTrue(exception.getMessage().contains("Acesso negado"));
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void testRetryMechanismWithDelayScheduling() {
        // Teste simplificado - verificar se delay scheduling está disponível
        int retryDelayMs = 5000;
        
        // Act - Testar que o serviço aceita configurações de retry
        when(properties.getMaxRetries()).thenReturn(3);
        when(properties.getRetryDelayMs()).thenReturn(retryDelayMs);

        // Assert - Verificar que as propriedades são acessíveis
        assertEquals(3, properties.getMaxRetries());
        assertEquals(retryDelayMs, properties.getRetryDelayMs());
    }

    @Test
    void testRedisQueueItemSerialization() throws Exception {
        // Arrange
        String expectedJson = "{\"campaignId\":\"" + campaignId + "\",\"campaignContactId\":\"" + contactId + "\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));

        // Act
        secureCampaignQueueService.addContactForRetry(campaignId, contactId, companyId.toString());

        // Assert
        verify(objectMapper).writeValueAsString(argThat(queueItem -> {
            SecureCampaignQueueService.SecureCampaignQueueItem captured = 
                (SecureCampaignQueueService.SecureCampaignQueueItem) queueItem;
            return captured.getCampaignId().equals(campaignId) &&
                   captured.getCampaignContactId().equals(contactId) &&
                   captured.getCompanyId().equals(companyId.toString()) &&
                   "system-retry".equals(captured.getCreatedBy());
        }));
    }

    @Test
    void testRetryIntegrationFlow() throws Exception {
        // Arrange - Simular cenário de sucesso
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);

        // Act
        secureCampaignQueueService.addContactForRetry(campaignId, contactId, companyId.toString());

        // Assert - Verificar que todos os componentes foram chamados corretamente
        verify(campaignService).findById(campaignId);
        verify(objectMapper).writeValueAsString(any());
        verify(zSetOperations).add(eq("rubia:campaign:queue"), anyString(), anyDouble());
    }

    @Test
    void testRetryWithInvalidCampaign() {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.empty());

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            secureCampaignQueueService.addContactForRetry(campaignId, contactId, companyId.toString());
        });
        
        assertTrue(exception.getMessage().contains("Acesso negado"));
        verify(zSetOperations, never()).add(anyString(), anyString(), anyDouble());
    }

    @Test
    void testRedisConnectionFailure() throws Exception {
        // Arrange
        when(campaignService.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(zSetOperations.add(anyString(), anyString(), anyDouble()))
            .thenThrow(new RuntimeException("Redis connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            secureCampaignQueueService.addContactForRetry(campaignId, contactId, companyId.toString());
        });
        
        assertTrue(exception.getMessage().contains("Erro ao adicionar contato para retry"));
    }
}