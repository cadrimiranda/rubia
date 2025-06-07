package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.*;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {
    
    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private CompanyRepository companyRepository;
    
    @InjectMocks
    private MessageService messageService;
    
    private Message message;
    private Conversation conversation;
    private Customer customer;
    private User user;
    private Company company;
    private CreateMessageDTO createDTO;
    private UpdateMessageDTO updateDTO;
    private UUID messageId;
    private UUID conversationId;
    private UUID userId;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .slug("test-company")
                .isActive(true)
                .build();
        
        customer = Customer.builder()
                .id(UUID.randomUUID())
                .phone("+5511999999001")
                .name("João Silva")
                .company(company)
                .build();
        
        user = User.builder()
                .id(userId)
                .name("Agent Silva")
                .email("agent@test.com")
                .role(UserRole.AGENT)
                .company(company)
                .build();
        
        conversation = Conversation.builder()
                .id(conversationId)
                .customer(customer)
                .assignedUser(user)
                .company(company)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .build();
        
        message = Message.builder()
                .id(messageId)
                .conversation(conversation)
                .content("Hello world")
                .senderType(SenderType.CUSTOMER)
                .senderId(null)
                .messageType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .isAiGenerated(false)
                .company(company)
                .createdAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .companyId(companyId)
                .content("Hello world")
                .senderType(SenderType.CUSTOMER)
                .senderId(null)
                .messageType(MessageType.TEXT)
                .isAiGenerated(false)
                .build();
        
        updateDTO = UpdateMessageDTO.builder()
                .content("Updated content")
                .status(MessageStatus.READ)
                .build();
    }
    
    @Test
    void create_ShouldCreateMessage_WhenValidData() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        
        MessageDTO result = messageService.create(createDTO);
        
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isEqualTo(conversationId);
        assertThat(result.getContent()).isEqualTo("Hello world");
        assertThat(result.getSenderType()).isEqualTo(SenderType.CUSTOMER);
        
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).save(any(Message.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenConversationNotFound() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> messageService.create(createDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conversa não encontrada");
        
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository, never()).save(any(Message.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenExternalMessageIdAlreadyExists() {
        CreateMessageDTO dtoWithExternalId = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .companyId(companyId)
                .content("Hello world")
                .senderType(SenderType.CUSTOMER)
                .externalMessageId("external_123")
                .build();
        
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.existsByExternalMessageId("external_123")).thenReturn(true);
        
        assertThatThrownBy(() -> messageService.create(dtoWithExternalId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
        
        verify(conversationRepository).findById(conversationId);
        verify(messageRepository).existsByExternalMessageId("external_123");
        verify(messageRepository, never()).save(any(Message.class));
    }
    
    @Test
    void findById_ShouldReturnMessage_WhenExists() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        
        MessageDTO result = messageService.findById(messageId);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(messageId);
        assertThat(result.getContent()).isEqualTo("Hello world");
        
        verify(messageRepository).findById(messageId);
    }
    
    @Test
    void findById_ShouldThrowException_WhenNotExists() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> messageService.findById(messageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
        
        verify(messageRepository).findById(messageId);
    }
    
    @Test
    void findByConversation_ShouldReturnMessages() {
        when(messageRepository.findByConversationIdOrderedByCreatedAt(conversationId))
                .thenReturn(List.of(message));
        
        List<MessageDTO> result = messageService.findByConversation(conversationId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConversationId()).isEqualTo(conversationId);
        
        verify(messageRepository).findByConversationIdOrderedByCreatedAt(conversationId);
    }
    
    @Test
    void markAsRead_ShouldUpdateStatusAndTimestamp() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        
        MessageDTO result = messageService.markAsRead(messageId);
        
        assertThat(result).isNotNull();
        
        verify(messageRepository).findById(messageId);
        verify(messageRepository).save(any(Message.class));
    }
    
    @Test
    void markAsDelivered_ShouldUpdateStatusAndTimestamp() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        
        MessageDTO result = messageService.markAsDelivered(messageId);
        
        assertThat(result).isNotNull();
        
        verify(messageRepository).findById(messageId);
        verify(messageRepository).save(any(Message.class));
    }
    
    @Test
    void update_ShouldUpdateMessage_WhenValidData() {
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);
        
        MessageDTO result = messageService.update(messageId, updateDTO);
        
        assertThat(result).isNotNull();
        
        verify(messageRepository).findById(messageId);
        verify(messageRepository).save(any(Message.class));
    }
    
    @Test
    void delete_ShouldDeleteMessage_WhenExists() {
        when(messageRepository.existsById(messageId)).thenReturn(true);
        
        messageService.delete(messageId);
        
        verify(messageRepository).existsById(messageId);
        verify(messageRepository).deleteById(messageId);
    }
    
    @Test
    void delete_ShouldThrowException_WhenNotExists() {
        when(messageRepository.existsById(messageId)).thenReturn(false);
        
        assertThatThrownBy(() -> messageService.delete(messageId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
        
        verify(messageRepository).existsById(messageId);
        verify(messageRepository, never()).deleteById(any());
    }
    
    @Test
    void countUnreadByConversation_ShouldReturnCount() {
        when(messageRepository.countUnreadCustomerMessages(conversationId)).thenReturn(5L);
        
        long result = messageService.countUnreadByConversation(conversationId);
        
        assertThat(result).isEqualTo(5L);
        
        verify(messageRepository).countUnreadCustomerMessages(conversationId);
    }
}