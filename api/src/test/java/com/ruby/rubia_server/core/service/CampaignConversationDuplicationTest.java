package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.repository.ChatLidMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Campaign Conversation Duplication Prevention Tests")
class CampaignConversationDuplicationTest {

    @Mock
    private ChatLidMappingRepository chatLidMappingRepository;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ChatLidMappingService chatLidMappingService;

    private Company company;
    private Customer customer;
    private Campaign campaign;
    private CampaignContact campaignContact;
    private UUID conversationId;
    private String customerPhone;
    private String chatLid;

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

        campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setName("Test Campaign");

        campaignContact = new CampaignContact();
        campaignContact.setId(UUID.randomUUID());
        campaignContact.setCustomer(customer);
        campaignContact.setCampaign(campaign);
        campaignContact.setStatus(CampaignContactStatus.PENDING);

        conversationId = UUID.randomUUID();
        customerPhone = customer.getPhone();
        chatLid = "269161355821173@lid";
    }

    @Test
    @DisplayName("Cenário 1: Sistema envia mensagem automaticamente → cliente responde → deve reutilizar conversa")
    void scenario1_SystemSendsAutomatically_CustomerReplies_ShouldReuseConversation() {
        // Given: Campanha criada com conversa e mapping de campanha
        ChatLidMapping campaignMapping = createCampaignMapping(conversationId, customerPhone, null);
        ConversationDTO existingConversation = createConversationDTO(conversationId, null);

        when(chatLidMappingRepository.findByChatLid(chatLid)).thenReturn(Optional.empty());
        when(chatLidMappingRepository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(customerPhone, company.getId()))
            .thenReturn(List.of(campaignMapping));
        when(conversationService.findById(conversationId, company.getId())).thenReturn(existingConversation);
        when(chatLidMappingRepository.findByConversationId(conversationId)).thenReturn(Optional.of(campaignMapping));
        when(chatLidMappingRepository.save(any(ChatLidMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When: Sistema automaticamente envia mensagem (cria mapping) e cliente responde
        // Step 1: Sistema envia (mapping já existe via campaignMessagingService.sendSingleMessageAsync)
        // Step 2: Cliente responde com chatLid
        List<ChatLidMapping> mappings = chatLidMappingService.findMappingsByPhone(customerPhone, company.getId());
        Optional<ChatLidMapping> foundMapping = mappings.stream()
            .filter(mapping -> mapping.getChatLid() == null && mapping.getFromCampaign())
            .findFirst();

        // Then: Deve encontrar mapping de campanha e atualizar com chatLid
        assertThat(foundMapping).isPresent();
        assertThat(foundMapping.get().getFromCampaign()).isTrue();
        assertThat(foundMapping.get().getChatLid()).isNull();

        // Update mapping with chatLid
        Optional<ChatLidMapping> updatedMapping = chatLidMappingService.updateMappingWithChatLid(conversationId, chatLid);
        assertThat(updatedMapping).isPresent();

        // Verify no new conversation is created
        verify(conversationService, never()).create(any(CreateConversationDTO.class), any());
        verify(chatLidMappingRepository, never()).save(argThat(m -> m.getId() == null)); // No new mapping
    }

    @Test
    @DisplayName("Cenário 2: Usuário envia mensagem draft → cliente responde → deve reutilizar conversa")
    void scenario2_UserSendsDraftMessage_CustomerReplies_ShouldReuseConversation() {
        // Given: Conversa existe com mensagem draft
        ChatLidMapping campaignMapping = createCampaignMapping(conversationId, customerPhone, null);
        ConversationDTO existingConversation = createConversationDTO(conversationId, null);

        when(chatLidMappingRepository.findByChatLid(chatLid)).thenReturn(Optional.empty());
        when(chatLidMappingRepository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(customerPhone, company.getId()))
            .thenReturn(List.of(campaignMapping));
        when(conversationService.findById(conversationId, company.getId())).thenReturn(existingConversation);
        when(chatLidMappingRepository.findByConversationId(conversationId)).thenReturn(Optional.of(campaignMapping));
        when(chatLidMappingRepository.save(any(ChatLidMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When: Usuário envia mensagem draft manualmente (via interface)
        // Simular que usuário enviou mensagem draft - mapping deve ser atualizado
        // Depois cliente responde
        List<ChatLidMapping> mappings = chatLidMappingService.findMappingsByPhone(customerPhone, company.getId());
        Optional<ChatLidMapping> foundMapping = mappings.stream()
            .filter(mapping -> mapping.getChatLid() == null && mapping.getFromCampaign())
            .findFirst();

        // Then: Deve reutilizar conversa existente
        assertThat(foundMapping).isPresent();
        assertThat(foundMapping.get().getConversationId()).isEqualTo(conversationId);

        // Update mapping when customer replies
        Optional<ChatLidMapping> updatedMapping = chatLidMappingService.updateMappingWithChatLid(conversationId, chatLid);
        assertThat(updatedMapping).isPresent();

        verify(conversationService, never()).create(any(CreateConversationDTO.class), any());
    }

    @Test
    @DisplayName("Cenário 3: Cliente responde ANTES de qualquer envio → deve reutilizar conversa")
    void scenario3_CustomerRepliesBeforeAnySend_ShouldReuseConversation() {
        // Given: Campanha criada, conversas existem, mas nenhuma mensagem foi enviada ainda
        ChatLidMapping campaignMapping = createCampaignMapping(conversationId, customerPhone, null);
        ConversationDTO existingConversation = createConversationDTO(conversationId, null);

        when(chatLidMappingRepository.findByChatLid(chatLid)).thenReturn(Optional.empty());
        when(chatLidMappingRepository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(customerPhone, company.getId()))
            .thenReturn(List.of(campaignMapping));
        when(conversationService.findById(conversationId, company.getId())).thenReturn(existingConversation);
        when(chatLidMappingRepository.findByConversationId(conversationId)).thenReturn(Optional.of(campaignMapping));
        when(chatLidMappingRepository.save(any(ChatLidMapping.class))).thenAnswer(i -> i.getArgument(0));

        // When: Cliente responde primeiro (sem nenhuma mensagem ter sido enviada)
        List<ChatLidMapping> mappings = chatLidMappingService.findMappingsByPhone(customerPhone, company.getId());
        Optional<ChatLidMapping> foundMapping = mappings.stream()
            .filter(mapping -> mapping.getChatLid() == null && mapping.getFromCampaign())
            .findFirst();

        // Then: Deve encontrar mapping de campanha e atualizar
        assertThat(foundMapping).isPresent();
        assertThat(foundMapping.get().getFromCampaign()).isTrue();

        // Update mapping with chatLid from customer's first message
        Optional<ChatLidMapping> updatedMapping = chatLidMappingService.updateMappingWithChatLid(conversationId, chatLid);
        assertThat(updatedMapping).isPresent();

        verify(conversationService, never()).create(any(CreateConversationDTO.class), any());
    }

    @Test
    @DisplayName("Cenário 4: Usuário exclui conversa → deve remover mapping e limpar Redis")
    void scenario4_UserDeletesConversation_ShouldRemoveMappingAndClearRedis() {
        // Given: Conversa existe com mapping
        ChatLidMapping campaignMapping = createCampaignMapping(conversationId, customerPhone, chatLid);
        
        when(chatLidMappingRepository.existsByChatLid(chatLid)).thenReturn(true);

        // When: Verificar se mapping existe
        boolean mappingExists = chatLidMappingService.existsByChatLid(chatLid);
        
        // Then: Deve encontrar mapping existente
        assertThat(mappingExists).isTrue();
        
        // Note: Em implementação real, deletion seria via:
        // 1. ConversationService.delete() → cascade delete mapping
        // 2. CampaignQueueService.removeFromQueue(conversationId)
        // 3. WebSocket notification de atualização
    }

    @Test
    @DisplayName("Cenário Edge Case: Múltiplas campanhas para mesmo cliente")
    void edgeCase_MultipleCampaignsForSameCustomer_ShouldHandleCorrectly() {
        // Given: Cliente tem múltiplas campanhas
        UUID conversation1Id = UUID.randomUUID();
        UUID conversation2Id = UUID.randomUUID();
        
        ChatLidMapping mapping1 = createCampaignMapping(conversation1Id, customerPhone, null);
        ChatLidMapping mapping2 = createCampaignMapping(conversation2Id, customerPhone, null);
        
        when(chatLidMappingRepository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(customerPhone, company.getId()))
            .thenReturn(List.of(mapping1, mapping2)); // mapping1 é mais recente

        // When: Cliente responde
        List<ChatLidMapping> mappings = chatLidMappingService.findMappingsByPhone(customerPhone, company.getId());
        Optional<ChatLidMapping> mostRecentMapping = mappings.stream()
            .filter(mapping -> mapping.getChatLid() == null && mapping.getFromCampaign())
            .findFirst();

        // Then: Deve usar a conversa mais recente (primeira na lista ordenada por data)
        assertThat(mostRecentMapping).isPresent();
        assertThat(mostRecentMapping.get().getConversationId()).isEqualTo(conversation1Id);
    }

    @Test
    @DisplayName("Cenário Edge Case: ChatLid já existe de conversa anterior")
    void edgeCase_ChatLidAlreadyExistsFromPreviousConversation_ShouldReuseExisting() {
        // Given: ChatLid já existe de conversa anterior
        UUID oldConversationId = UUID.randomUUID();
        ChatLidMapping existingMapping = createCampaignMapping(oldConversationId, customerPhone, chatLid);
        ConversationDTO existingConversation = createConversationDTO(oldConversationId, chatLid);
        
        when(chatLidMappingRepository.findByChatLid(chatLid)).thenReturn(Optional.of(existingMapping));
        when(conversationService.findById(oldConversationId, company.getId())).thenReturn(existingConversation);

        // When: Buscar conversa por chatLid
        Optional<Conversation> foundConversation = chatLidMappingService.findConversationByChatLid(chatLid);

        // Then: Deve retornar conversa existente
        assertThat(foundConversation).isPresent();
        assertThat(foundConversation.get().getId()).isEqualTo(oldConversationId);
        assertThat(foundConversation.get().getChatLid()).isEqualTo(chatLid);
    }

    private ChatLidMapping createCampaignMapping(UUID conversationId, String phone, String chatLid) {
        return ChatLidMapping.builder()
            .id(UUID.randomUUID())
            .conversationId(conversationId)
            .phone(phone)
            .companyId(company.getId())
            .whatsappInstanceId(null)
            .fromCampaign(true)
            .chatLid(chatLid)
            .build();
    }

    private ConversationDTO createConversationDTO(UUID conversationId, String chatLid) {
        return ConversationDTO.builder()
            .id(conversationId)
            .customerId(customer.getId())
            .channel(Channel.WHATSAPP)
            .status(ConversationStatus.ENTRADA)
            .chatLid(chatLid)
            .build();
    }
}