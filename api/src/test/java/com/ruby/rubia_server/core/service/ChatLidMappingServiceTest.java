package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.ChatLidMapping;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.repository.ChatLidMappingRepository;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatLidMappingServiceTest {

    @Mock
    private ChatLidMappingRepository repository;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ChatLidMappingService service;

    private String chatLid;
    private String phone;
    private UUID companyId;
    private UUID conversationId;
    private UUID instanceId;
    private ChatLidMapping mapping;

    @BeforeEach
    void setUp() {
        chatLid = "269161355821173@lid";
        phone = "5511999999999";
        companyId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        instanceId = UUID.randomUUID();

        mapping = ChatLidMapping.builder()
            .chatLid(chatLid)
            .conversationId(conversationId)
            .phone(phone)
            .companyId(companyId)
            .whatsappInstanceId(instanceId)
            .fromCampaign(false)
            .build();
    }

    @Test
    void findOrCreateMapping_ShouldReturnExisting_WhenChatLidExists() {
        // Given
        when(repository.findByChatLid(chatLid)).thenReturn(Optional.of(mapping));

        // When
        ChatLidMapping result = service.findOrCreateMapping(chatLid, phone, companyId, instanceId);

        // Then
        assertThat(result).isEqualTo(mapping);
        verify(repository, never()).save(any());
        verifyNoInteractions(conversationService);
    }

    @Test
    void findOrCreateMapping_ShouldCreateTemporary_WhenChatLidNotExists() {
        // Given
        when(repository.findByChatLid(chatLid)).thenReturn(Optional.empty());

        // When
        ChatLidMapping result = service.findOrCreateMapping(chatLid, phone, companyId, instanceId);

        // Then
        assertThat(result.getChatLid()).isEqualTo(chatLid);
        assertThat(result.getPhone()).isEqualTo(phone);
        assertThat(result.getCompanyId()).isEqualTo(companyId);
        assertThat(result.getWhatsappInstanceId()).isEqualTo(instanceId);
        assertThat(result.getFromCampaign()).isFalse();
    }

    @Test
    void findConversationByChatLid_ShouldReturnConversation_WhenMappingExists() {
        // Given
        ConversationDTO conversationDTO = ConversationDTO.builder()
            .id(conversationId)
            .chatLid(chatLid)
            .build();
            
        when(repository.findByChatLid(chatLid)).thenReturn(Optional.of(mapping));
        when(conversationService.findById(conversationId, companyId)).thenReturn(conversationDTO);

        // When
        Optional<Conversation> result = service.findConversationByChatLid(chatLid);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(conversationId);
        assertThat(result.get().getChatLid()).isEqualTo(chatLid);
    }

    @Test
    void findConversationByChatLid_ShouldReturnEmpty_WhenMappingNotExists() {
        // Given
        when(repository.findByChatLid(chatLid)).thenReturn(Optional.empty());

        // When
        Optional<Conversation> result = service.findConversationByChatLid(chatLid);

        // Then
        assertThat(result).isEmpty();
        verifyNoInteractions(conversationService);
    }

    @Test
    void findConversationByChatLid_ShouldReturnEmpty_WhenConversationNotFound() {
        // Given
        when(repository.findByChatLid(chatLid)).thenReturn(Optional.of(mapping));
        when(conversationService.findById(conversationId, companyId)).thenReturn(null);

        // When
        Optional<Conversation> result = service.findConversationByChatLid(chatLid);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void createMappingForCampaign_ShouldCreateMapping_WithFromCampaignTrue() {
        // Given
        when(repository.save(any(ChatLidMapping.class))).thenReturn(mapping);

        // When
        ChatLidMapping result = service.createMappingForCampaign(conversationId, phone, companyId, instanceId);

        // Then
        verify(repository).save(argThat(m -> 
            m.getConversationId().equals(conversationId) &&
            m.getPhone().equals(phone) &&
            m.getCompanyId().equals(companyId) &&
            m.getFromCampaign().equals(true) &&
            m.getChatLid() == null // Campaign mappings start without chatLid
        ));
    }

    @Test
    void updateMappingWithChatLid_ShouldUpdateExisting_WhenMappingFound() {
        // Given
        UUID mappingId = UUID.randomUUID();
        ChatLidMapping existingMapping = ChatLidMapping.builder()
            .id(mappingId)
            .conversationId(conversationId)
            .phone(phone)
            .companyId(companyId)
            .fromCampaign(true)
            .build();

        when(repository.findByConversationId(conversationId)).thenReturn(Optional.of(existingMapping));
        when(repository.save(any(ChatLidMapping.class))).thenReturn(existingMapping);

        // When
        Optional<ChatLidMapping> result = service.updateMappingWithChatLid(conversationId, chatLid);

        // Then
        verify(repository).save(argThat(m -> 
            m.getId().equals(mappingId) &&
            m.getChatLid().equals(chatLid)
        ));
        assertThat(result).isPresent();
    }

    @Test
    void updateMappingWithChatLid_ShouldReturnEmpty_WhenMappingNotFound() {
        // Given
        when(repository.findByConversationId(conversationId)).thenReturn(Optional.empty());

        // When
        Optional<ChatLidMapping> result = service.updateMappingWithChatLid(conversationId, chatLid);

        // Then
        assertThat(result).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void findMappingsByPhone_ShouldReturnMappings_WhenFound() {
        // Given
        List<ChatLidMapping> mappings = List.of(mapping);
        when(repository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(phone, companyId))
            .thenReturn(mappings);

        // When
        List<ChatLidMapping> result = service.findMappingsByPhone(phone, companyId);

        // Then
        assertThat(result).isEqualTo(mappings);
    }

    @Test
    void cleanupOldMappings_ShouldDeleteOldMappings_WhenCalled() {
        // Given
        int daysToKeep = 30;
        int deletedCount = 5;
        when(repository.deleteByCompanyIdAndCreatedAtBefore(eq(companyId), any()))
            .thenReturn(deletedCount);

        // When
        int result = service.cleanupOldMappings(companyId, daysToKeep);

        // Then
        assertThat(result).isEqualTo(deletedCount);
        verify(repository).deleteByCompanyIdAndCreatedAtBefore(eq(companyId), any());
    }

    @Test
    void findOrCreateMapping_ShouldThrowException_WhenInvalidInput() {
        // When/Then
        assertThatThrownBy(() -> service.findOrCreateMapping(null, phone, companyId, instanceId))
            .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> service.findOrCreateMapping(chatLid, null, companyId, instanceId))
            .isInstanceOf(IllegalArgumentException.class);
        
        assertThatThrownBy(() -> service.findOrCreateMapping(chatLid, phone, null, instanceId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void existsByChatLid_ShouldReturnCorrectValue() {
        // Given
        when(repository.existsByChatLid(chatLid)).thenReturn(true);

        // When
        boolean exists = service.existsByChatLid(chatLid);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void findMappingByConversationId_ShouldReturnMapping_WhenFound() {
        // Given
        when(repository.findByConversationId(conversationId)).thenReturn(Optional.of(mapping));

        // When
        Optional<ChatLidMapping> result = service.findMappingByConversationId(conversationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(mapping);
    }

    @Test
    void countMappingsByCompany_ShouldReturnCount() {
        // Given
        long expectedCount = 10L;
        when(repository.countByCompanyId(companyId)).thenReturn(expectedCount);

        // When
        long count = service.countMappingsByCompany(companyId);

        // Then
        assertThat(count).isEqualTo(expectedCount);
    }

    @Test
    void findCampaignMappings_ShouldReturnCampaignMappings() {
        // Given
        List<ChatLidMapping> campaignMappings = List.of(mapping);
        when(repository.findByFromCampaignTrueAndCompanyId(companyId)).thenReturn(campaignMappings);

        // When
        List<ChatLidMapping> result = service.findCampaignMappings(companyId);

        // Then
        assertThat(result).isEqualTo(campaignMappings);
    }
}