package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.ChatLidMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatLidMappingRepositoryTest {

    @Mock
    private ChatLidMappingRepository repository;

    private UUID companyId;
    private UUID conversationId1;
    private UUID conversationId2;
    private String phone1;
    private String phone2;
    private String chatLid1;
    private String chatLid2;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        conversationId1 = UUID.randomUUID();
        conversationId2 = UUID.randomUUID();
        phone1 = "5511999999999";
        phone2 = "5511888888888";
        chatLid1 = "269161355821173@lid";
        chatLid2 = "269161355821174@lid";
    }

    @Test
    void findByChatLid_ShouldReturnMapping_WhenExists() {
        // Given
        ChatLidMapping mapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        when(repository.findByChatLid(chatLid1)).thenReturn(Optional.of(mapping));

        // When
        Optional<ChatLidMapping> result = repository.findByChatLid(chatLid1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChatLid()).isEqualTo(chatLid1);
        assertThat(result.get().getConversationId()).isEqualTo(conversationId1);
        verify(repository).findByChatLid(chatLid1);
    }

    @Test
    void findByChatLid_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(repository.findByChatLid("nonexistent@lid")).thenReturn(Optional.empty());

        // When
        Optional<ChatLidMapping> result = repository.findByChatLid("nonexistent@lid");

        // Then
        assertThat(result).isEmpty();
        verify(repository).findByChatLid("nonexistent@lid");
    }

    @Test
    void findByConversationId_ShouldReturnMapping_WhenExists() {
        // Given
        ChatLidMapping mapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        when(repository.findByConversationId(conversationId1)).thenReturn(Optional.of(mapping));

        // When
        Optional<ChatLidMapping> result = repository.findByConversationId(conversationId1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConversationId()).isEqualTo(conversationId1);
        assertThat(result.get().getChatLid()).isEqualTo(chatLid1);
        verify(repository).findByConversationId(conversationId1);
    }

    @Test
    void existsByChatLid_ShouldReturnTrue_WhenExists() {
        // Given
        when(repository.existsByChatLid(chatLid1)).thenReturn(true);

        // When
        boolean exists = repository.existsByChatLid(chatLid1);

        // Then
        assertThat(exists).isTrue();
        verify(repository).existsByChatLid(chatLid1);
    }

    private ChatLidMapping createMapping(String chatLid, UUID conversationId, String phone, UUID companyId) {
        return ChatLidMapping.builder()
            .chatLid(chatLid)
            .conversationId(conversationId)
            .phone(phone)
            .companyId(companyId)
            .whatsappInstanceId(UUID.randomUUID())
            .fromCampaign(false)
            .build();
    }
}