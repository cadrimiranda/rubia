package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.ChatLidMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChatLidMappingRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
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
        entityManager.persistAndFlush(mapping);

        // When
        Optional<ChatLidMapping> result = repository.findByChatLid(chatLid1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChatLid()).isEqualTo(chatLid1);
        assertThat(result.get().getConversationId()).isEqualTo(conversationId1);
    }

    @Test
    void findByChatLid_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<ChatLidMapping> result = repository.findByChatLid("nonexistent@lid");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findByConversationId_ShouldReturnMapping_WhenExists() {
        // Given
        ChatLidMapping mapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        entityManager.persistAndFlush(mapping);

        // When
        Optional<ChatLidMapping> result = repository.findByConversationId(conversationId1);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConversationId()).isEqualTo(conversationId1);
        assertThat(result.get().getChatLid()).isEqualTo(chatLid1);
    }

    @Test
    void findByPhoneAndCompanyId_ShouldReturnMappings_WhenExists() {
        // Given
        ChatLidMapping mapping1 = createMapping(chatLid1, conversationId1, phone1, companyId);
        ChatLidMapping mapping2 = createMapping(chatLid2, conversationId2, phone1, companyId); // mesmo telefone
        entityManager.persistAndFlush(mapping1);
        entityManager.persistAndFlush(mapping2);

        // When
        List<ChatLidMapping> result = repository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(phone1, companyId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCreatedAt()).isAfterOrEqualTo(result.get(1).getCreatedAt());
    }

    @Test
    void findByPhoneAndCompanyId_ShouldReturnEmpty_WhenDifferentCompany() {
        // Given
        UUID otherCompanyId = UUID.randomUUID();
        ChatLidMapping mapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        entityManager.persistAndFlush(mapping);

        // When
        List<ChatLidMapping> result = repository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(phone1, otherCompanyId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void findMostRecentByPhoneAndCompanyId_ShouldReturnLatest_WhenMultipleExist() {
        // Given
        ChatLidMapping oldMapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        ChatLidMapping newMapping = createMapping(chatLid2, conversationId2, phone1, companyId);
        
        entityManager.persistAndFlush(oldMapping);
        entityManager.flush();
        
        // Pequeno delay para garantir timestamps diferentes
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        entityManager.persistAndFlush(newMapping);

        // When
        Optional<ChatLidMapping> result = repository.findMostRecentByPhoneAndCompanyId(phone1, companyId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getChatLid()).isEqualTo(chatLid2);
        assertThat(result.get().getConversationId()).isEqualTo(conversationId2);
    }

    @Test
    void existsByChatLid_ShouldReturnTrue_WhenExists() {
        // Given
        ChatLidMapping mapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        entityManager.persistAndFlush(mapping);

        // When
        boolean exists = repository.existsByChatLid(chatLid1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void existsByChatLid_ShouldReturnFalse_WhenNotExists() {
        // When
        boolean exists = repository.existsByChatLid("nonexistent@lid");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void deleteByCompanyIdAndCreatedAtBefore_ShouldRemoveOldMappings() {
        // Given
        ChatLidMapping oldMapping = createMapping(chatLid1, conversationId1, phone1, companyId);
        ChatLidMapping newMapping = createMapping(chatLid2, conversationId2, phone2, companyId);
        
        entityManager.persistAndFlush(oldMapping);
        entityManager.persistAndFlush(newMapping);
        
        // When
        int deleted = repository.deleteByCompanyIdAndCreatedAtBefore(
            companyId, 
            newMapping.getCreatedAt()
        );

        // Then
        assertThat(deleted).isEqualTo(1);
        assertThat(repository.existsByChatLid(chatLid1)).isFalse();
        assertThat(repository.existsByChatLid(chatLid2)).isTrue();
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