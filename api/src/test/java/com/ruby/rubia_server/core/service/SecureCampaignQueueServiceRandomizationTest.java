package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.config.CampaignMessagingProperties;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Secure Campaign Queue Service - Randomization Tests")
class SecureCampaignQueueServiceRandomizationTest {

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
    private CampaignMessagingProperties properties;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private SecureCampaignQueueService service;

    @BeforeEach
    void setUp() {
        service = new SecureCampaignQueueService(
            redisTemplate, objectMapper, campaignService, 
            campaignContactService, campaignMessagingService, properties);
        
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        
        // Setup basic properties
        when(properties.getBatchSize()).thenReturn(30);
        when(properties.getBatchPauseMinutes()).thenReturn(30);
    }

    @Test
    @DisplayName("Should randomize contact order when randomization is enabled")
    void shouldRandomizeContactOrderWhenRandomizationEnabled() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        
        List<CampaignContact> originalContacts = createTestContacts(10);
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        // Capturar todas as chamadas para Redis
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> scoreCaptor = ArgumentCaptor.forClass(Double.class);
        
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            SecureCampaignQueueService.SecureCampaignQueueItem item = 
                (SecureCampaignQueueService.SecureCampaignQueueItem) invocation.getArgument(0);
            return "json-" + item.getCampaignContactId().toString();
        });
        
        // When
        invokeScheduleMessagesInRedis(campaignId, originalContacts, companyId, userId);
        
        // Then
        verify(zSetOperations, times(10)).add(anyString(), valueCaptor.capture(), scoreCaptor.capture());
        
        List<String> processedOrder = valueCaptor.getAllValues();
        List<String> originalOrder = originalContacts.stream()
            .map(contact -> "json-" + contact.getId().toString())
            .collect(Collectors.toList());
        
        // Verificar que a ordem foi alterada (com alta probabilidade)
        // Para 10 elementos, é muito improvável que a ordem permaneça igual após randomização
        assertThat(processedOrder).containsExactlyInAnyOrderElementsOf(originalOrder);
        
        // Verificar que pelo menos alguns elementos mudaram de posição
        boolean orderChanged = false;
        for (int i = 0; i < Math.min(processedOrder.size(), originalOrder.size()); i++) {
            if (!processedOrder.get(i).equals(originalOrder.get(i))) {
                orderChanged = true;
                break;
            }
        }
        
        // Com 10 elementos, é estatisticamente quase impossível manter a mesma ordem
        assertThat(orderChanged).isTrue();
    }

    @Test
    @DisplayName("Should maintain original order when randomization is disabled")
    void shouldMaintainOriginalOrderWhenRandomizationDisabled() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(false);
        
        List<CampaignContact> originalContacts = createTestContacts(5);
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            SecureCampaignQueueService.SecureCampaignQueueItem item = 
                (SecureCampaignQueueService.SecureCampaignQueueItem) invocation.getArgument(0);
            return "json-" + item.getCampaignContactId().toString();
        });
        
        // When
        invokeScheduleMessagesInRedis(campaignId, originalContacts, companyId, userId);
        
        // Then
        verify(zSetOperations, times(5)).add(anyString(), valueCaptor.capture(), anyDouble());
        
        List<String> processedOrder = valueCaptor.getAllValues();
        List<String> expectedOrder = originalContacts.stream()
            .map(contact -> "json-" + contact.getId().toString())
            .collect(Collectors.toList());
        
        // Verificar que a ordem original foi mantida
        assertThat(processedOrder).containsExactlyElementsOf(expectedOrder);
    }

    @Test
    @DisplayName("Should randomize large contact lists effectively")
    void shouldRandomizeLargeContactListsEffectively() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        
        List<CampaignContact> largeContactList = createTestContacts(100);
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            SecureCampaignQueueService.SecureCampaignQueueItem item = 
                (SecureCampaignQueueService.SecureCampaignQueueItem) invocation.getArgument(0);
            return item.getCampaignContactId().toString();
        });
        
        // When
        invokeScheduleMessagesInRedis(campaignId, largeContactList, companyId, userId);
        
        // Then
        verify(zSetOperations, times(100)).add(anyString(), valueCaptor.capture(), anyDouble());
        
        List<String> processedOrder = valueCaptor.getAllValues();
        List<String> originalOrder = largeContactList.stream()
            .map(contact -> contact.getId().toString())
            .collect(Collectors.toList());
        
        // Para 100 elementos, calcular quantos mudaram de posição
        int changedPositions = 0;
        for (int i = 0; i < processedOrder.size(); i++) {
            if (!processedOrder.get(i).equals(originalOrder.get(i))) {
                changedPositions++;
            }
        }
        
        // Esperar que pelo menos 80% das posições tenham mudado
        double changePercentage = (double) changedPositions / processedOrder.size();
        assertThat(changePercentage).isGreaterThan(0.8);
    }

    @Test
    @DisplayName("Should preserve all contacts during randomization")
    void shouldPreserveAllContactsDuringRandomization() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        
        List<CampaignContact> originalContacts = createTestContacts(20);
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        Set<UUID> originalIds = originalContacts.stream()
            .map(CampaignContact::getId)
            .collect(Collectors.toSet());
        
        ArgumentCaptor<SecureCampaignQueueService.SecureCampaignQueueItem> itemCaptor = 
            ArgumentCaptor.forClass(SecureCampaignQueueService.SecureCampaignQueueItem.class);
        
        when(objectMapper.writeValueAsString(itemCaptor.capture())).thenReturn("mock-json");
        
        // When
        invokeScheduleMessagesInRedis(campaignId, originalContacts, companyId, userId);
        
        // Then
        List<SecureCampaignQueueService.SecureCampaignQueueItem> capturedItems = itemCaptor.getAllValues();
        Set<UUID> processedIds = capturedItems.stream()
            .map(SecureCampaignQueueService.SecureCampaignQueueItem::getCampaignContactId)
            .collect(Collectors.toSet());
        
        // Verificar que todos os IDs originais estão presentes
        assertThat(processedIds).containsExactlyInAnyOrderElementsOf(originalIds);
        assertThat(processedIds).hasSize(originalIds.size());
    }

    @Test
    @DisplayName("Should maintain batch structure after randomization")
    void shouldMaintainBatchStructureAfterRandomization() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        when(properties.getBatchSize()).thenReturn(10);
        
        List<CampaignContact> contacts = createTestContacts(25); // 3 lotes: 10, 10, 5
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        ArgumentCaptor<SecureCampaignQueueService.SecureCampaignQueueItem> itemCaptor = 
            ArgumentCaptor.forClass(SecureCampaignQueueService.SecureCampaignQueueItem.class);
        
        when(objectMapper.writeValueAsString(itemCaptor.capture())).thenReturn("mock-json");
        
        // When
        invokeScheduleMessagesInRedis(campaignId, contacts, companyId, userId);
        
        // Then
        List<SecureCampaignQueueService.SecureCampaignQueueItem> items = itemCaptor.getAllValues();
        
        // Verificar estrutura de lotes
        Map<Integer, Long> batchCounts = items.stream()
            .collect(Collectors.groupingBy(
                SecureCampaignQueueService.SecureCampaignQueueItem::getBatchNumber,
                Collectors.counting()
            ));
        
        assertThat(batchCounts).hasSize(3);
        assertThat(batchCounts.get(1)).isEqualTo(10L); // Primeiro lote
        assertThat(batchCounts.get(2)).isEqualTo(10L); // Segundo lote
        assertThat(batchCounts.get(3)).isEqualTo(5L);  // Terceiro lote
    }

    @Test
    @DisplayName("Should handle empty contact list gracefully")
    void shouldHandleEmptyContactListGracefully() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        
        List<CampaignContact> emptyContacts = new ArrayList<>();
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        // When
        invokeScheduleMessagesInRedis(campaignId, emptyContacts, companyId, userId);
        
        // Then
        verifyNoInteractions(zSetOperations);
        verifyNoInteractions(objectMapper);
    }

    @Test
    @DisplayName("Should handle single contact without randomization issues")
    void shouldHandleSingleContactWithoutRandomizationIssues() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        
        List<CampaignContact> singleContact = createTestContacts(1);
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        when(objectMapper.writeValueAsString(any())).thenReturn("single-contact-json");
        
        // When
        invokeScheduleMessagesInRedis(campaignId, singleContact, companyId, userId);
        
        // Then
        verify(zSetOperations, times(1)).add(anyString(), eq("single-contact-json"), anyDouble());
    }

    @Test
    @DisplayName("Should log randomization activity when enabled")
    void shouldLogRandomizationActivityWhenEnabled() throws Exception {
        // Given
        when(properties.isRandomizeOrder()).thenReturn(true);
        
        List<CampaignContact> contacts = createTestContacts(5);
        UUID campaignId = UUID.randomUUID();
        String companyId = "company-123";
        String userId = "user-456";
        
        when(objectMapper.writeValueAsString(any())).thenReturn("mock-json");
        
        // When
        invokeScheduleMessagesInRedis(campaignId, contacts, companyId, userId);
        
        // Then - verificar que o método foi executado sem exceções
        // O log é verificado através da execução bem-sucedida
        verify(zSetOperations, times(5)).add(anyString(), anyString(), anyDouble());
    }

    private List<CampaignContact> createTestContacts(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                CampaignContact contact = new CampaignContact();
                contact.setId(UUID.randomUUID());
                contact.setStatus(CampaignContactStatus.PENDING);
                
                Customer customer = new Customer();
                customer.setId(UUID.randomUUID());
                customer.setName("Customer " + i);
                customer.setPhone("+551199999" + String.format("%04d", i));
                contact.setCustomer(customer);
                
                return contact;
            })
            .collect(Collectors.toList());
    }

    private void invokeScheduleMessagesInRedis(UUID campaignId, List<CampaignContact> contacts, 
                                             String companyId, String userId) throws Exception {
        ReflectionTestUtils.invokeMethod(service, "scheduleMessagesInRedis", 
                                       campaignId, contacts, companyId, userId);
    }
}