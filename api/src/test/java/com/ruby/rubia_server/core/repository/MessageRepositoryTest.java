package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.ConversationChannel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MessageRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private MessageRepository messageRepository;
    
    private Message message1;
    private Message message2;
    private Message message3;
    private Message message4;
    private Conversation conversation1;
    private Conversation conversation2;
    private Customer customer1;
    private User agent1;
    private Company company1;
    private Department department1;
    private UUID conversation1Id;
    private UUID conversation2Id;
    private UUID customer1Id;
    private UUID agent1Id;
    
    @BeforeEach
    void setUp() {
        conversation1Id = UUID.randomUUID();
        conversation2Id = UUID.randomUUID();
        customer1Id = UUID.randomUUID();
        agent1Id = UUID.randomUUID();
        
        company1 = Company.builder()
                .id(UUID.randomUUID())
                .name("Company 1")
                .slug("company1")
                .isActive(true)
                .build();
        
        entityManager.persistAndFlush(company1);
        
        department1 = Department.builder()
                .name("Suporte")
                .description("Departamento de suporte")
                .company(company1)
                .autoAssign(true)
                .build();
        
        entityManager.persistAndFlush(department1);
        
        customer1 = Customer.builder()
                .id(customer1Id)
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .company(company1)
                .isBlocked(false)
                .build();
        
        entityManager.persistAndFlush(customer1);
        
        agent1 = User.builder()
                .id(agent1Id)
                .name("Agent Silva")
                .email("agent@company1.com")
                .passwordHash("hash123")
                .role(UserRole.AGENT)
                .company(company1)
                .department(department1)
                .isOnline(true)
                .build();
        
        entityManager.persistAndFlush(agent1);
        
        conversation1 = Conversation.builder()
                .id(conversation1Id)
                .customer(customer1)
                .assignedUser(agent1)
                .department(department1)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .isPinned(false)
                .company(company1)
                .build();
        
        conversation2 = Conversation.builder()
                .id(conversation2Id)
                .customer(customer1)
                .assignedUser(null)
                .department(department1)
                .status(ConversationStatus.ESPERANDO)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .isPinned(false)
                .company(company1)
                .build();
        
        entityManager.persistAndFlush(conversation1);
        entityManager.persistAndFlush(conversation2);
        
        message1 = Message.builder()
                .conversation(conversation1)
                .company(company1)
                .senderId(customer1Id)
                .senderType(SenderType.CUSTOMER)
                .content("Olá, preciso de ajuda")
                .messageType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .externalMessageId("ext_001")
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        
        message2 = Message.builder()
                .conversation(conversation1)
                .company(company1)
                .senderId(agent1Id)
                .senderType(SenderType.AGENT)
                .content("Claro! Como posso ajudar?")
                .messageType(MessageType.TEXT)
                .status(MessageStatus.READ)
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        
        message3 = Message.builder()
                .conversation(conversation1)
                .company(company1)
                .senderId(customer1Id)
                .senderType(SenderType.CUSTOMER)
                .content("Tenho um problema com meu pedido")
                .messageType(MessageType.TEXT)
                .status(MessageStatus.DELIVERED)
                .externalMessageId("ext_002")
                .isAiGenerated(false)
                .createdAt(LocalDateTime.now().minusMinutes(30))
                .build();
        
        message4 = Message.builder()
                .conversation(conversation2)
                .company(company1)
                .senderId(customer1Id)
                .senderType(SenderType.CUSTOMER)
                .content("Esta é outra conversa")
                .messageType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .externalMessageId("ext_003")
                .isAiGenerated(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        entityManager.persistAndFlush(message1);
        entityManager.persistAndFlush(message2);
        entityManager.persistAndFlush(message3);
        entityManager.persistAndFlush(message4);
    }
    
    @Test
    void findByConversationId_ShouldReturnMessagesFromConversation() {
        List<Message> result = messageRepository.findByConversationId(conversation1Id);
        
        assertThat(result).hasSize(3);
        assertThat(result).extracting(msg -> msg.getConversation().getId())
                .containsOnly(conversation1Id);
    }
    
    @Test
    void findByConversationId_WithPageable_ShouldReturnPagedMessages() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Message> result = messageRepository.findByConversationId(conversation1Id, pageable);
        
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }
    
    @Test
    void findByConversationIdOrderedByCreatedAt_ShouldReturnOrderedMessages() {
        List<Message> result = messageRepository.findByConversationIdOrderedByCreatedAt(conversation1Id);
        
        assertThat(result).hasSize(3);
        // Should be ordered by creation time (oldest first)
        assertThat(result.get(0).getContent()).isEqualTo("Olá, preciso de ajuda");
        assertThat(result.get(1).getContent()).isEqualTo("Claro! Como posso ajudar?");
        assertThat(result.get(2).getContent()).isEqualTo("Tenho um problema com meu pedido");
    }
    
    @Test
    void findByConversationIdOrderedByCreatedAtDesc_ShouldReturnOrderedMessagesDesc() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Message> result = messageRepository.findByConversationIdOrderedByCreatedAtDesc(conversation1Id, pageable);
        
        assertThat(result.getContent()).hasSize(3);
        // Should be ordered by creation time (newest first)
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Tenho um problema com meu pedido");
        assertThat(result.getContent().get(1).getContent()).isEqualTo("Claro! Como posso ajudar?");
        assertThat(result.getContent().get(2).getContent()).isEqualTo("Olá, preciso de ajuda");
    }
    
    @Test
    void findByExternalMessageId_ShouldReturnMessage_WhenExists() {
        Optional<Message> result = messageRepository.findByExternalMessageId("ext_001");
        
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Olá, preciso de ajuda");
    }
    
    @Test
    void findByExternalMessageId_ShouldReturnEmpty_WhenNotExists() {
        Optional<Message> result = messageRepository.findByExternalMessageId("ext_999");
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findBySenderType_ShouldReturnMessagesFromSenderType() {
        List<Message> result = messageRepository.findBySenderType(SenderType.CUSTOMER);
        
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(msg -> msg.getSenderType() == SenderType.CUSTOMER);
    }
    
    @Test
    void findBySenderId_ShouldReturnMessagesFromSender() {
        List<Message> result = messageRepository.findBySenderId(customer1Id);
        
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(msg -> msg.getSenderId().equals(customer1Id));
    }
    
    @Test
    void findByStatus_ShouldReturnMessagesWithStatus() {
        List<Message> result = messageRepository.findByStatus(MessageStatus.SENT);
        
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(msg -> msg.getStatus() == MessageStatus.SENT);
    }
    
    @Test
    void findUnreadCustomerMessages_ShouldReturnUnreadCustomerMessages() {
        List<Message> result = messageRepository.findUnreadCustomerMessages(conversation1Id);
        
        assertThat(result).hasSize(2); // message1 (SENT) and message3 (DELIVERED)
        assertThat(result).allMatch(msg -> msg.getSenderType() == SenderType.CUSTOMER);
        assertThat(result).allMatch(msg -> 
                msg.getStatus() == MessageStatus.SENT || msg.getStatus() == MessageStatus.DELIVERED);
    }
    
    @Test
    void countUnreadCustomerMessages_ShouldReturnCorrectCount() {
        long result = messageRepository.countUnreadCustomerMessages(conversation1Id);
        
        assertThat(result).isEqualTo(2);
    }
    
    @Test
    void findLastMessageByConversation_ShouldReturnLastMessage() {
        Optional<Message> result = messageRepository.findLastMessageByConversation(conversation1Id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("Tenho um problema com meu pedido");
    }
    
    @Test
    void findAiGeneratedMessagesByConversation_ShouldReturnAiMessages() {
        List<Message> result = messageRepository.findAiGeneratedMessagesByConversation(conversation2Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsAiGenerated()).isTrue();
    }
    
    @Test
    void findAiGeneratedMessagesByConversation_ShouldReturnEmpty_WhenNoAiMessages() {
        List<Message> result = messageRepository.findAiGeneratedMessagesByConversation(conversation1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void searchByContent_ShouldReturnMatchingMessages() {
        List<Message> result = messageRepository.searchByContent("ajuda");
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Message::getContent)
                .allMatch(content -> content.toLowerCase().contains("ajuda"));
    }
    
    @Test
    void searchByContentInConversation_ShouldReturnMatchingMessagesInConversation() {
        List<Message> result = messageRepository.searchByContentInConversation(conversation1Id, "problema");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).contains("problema");
        assertThat(result.get(0).getConversation().getId()).isEqualTo(conversation1Id);
    }
    
    @Test
    void findByDateRange_ShouldReturnMessagesInRange() {
        LocalDateTime start = LocalDateTime.now().minusHours(3);
        LocalDateTime end = LocalDateTime.now().plusHours(1);
        
        List<Message> result = messageRepository.findByDateRange(start, end);
        
        assertThat(result).hasSize(4); // All messages are within this range
    }
    
    @Test
    void countByConversationId_ShouldReturnCorrectCount() {
        long result = messageRepository.countByConversationId(conversation1Id);
        
        assertThat(result).isEqualTo(3);
    }
    
    @Test
    void countByConversationIdAndSenderType_ShouldReturnCorrectCount() {
        long result = messageRepository.countByConversationIdAndSenderType(conversation1Id, SenderType.CUSTOMER);
        
        assertThat(result).isEqualTo(2);
    }
    
    @Test
    void findRecentMessagesByAgent_ShouldReturnAgentMessages() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> result = messageRepository.findRecentMessagesByAgent(agent1Id, pageable);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSenderType()).isEqualTo(SenderType.AGENT);
        assertThat(result.get(0).getSenderId()).isEqualTo(agent1Id);
    }
    
    @Test
    void findConversationIdsByAgent_ShouldReturnConversationIds() {
        List<UUID> result = messageRepository.findConversationIdsByAgent(agent1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result).contains(conversation1Id);
    }
    
    @Test
    void existsByExternalMessageId_ShouldReturnTrue_WhenExists() {
        boolean result = messageRepository.existsByExternalMessageId("ext_001");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByExternalMessageId_ShouldReturnFalse_WhenNotExists() {
        boolean result = messageRepository.existsByExternalMessageId("ext_999");
        
        assertThat(result).isFalse();
    }
}