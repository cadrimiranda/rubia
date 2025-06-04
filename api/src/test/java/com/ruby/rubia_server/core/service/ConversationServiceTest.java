package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.ConversationChannel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @InjectMocks
    private ConversationService conversationService;
    
    private Conversation conversation;
    private Customer customer;
    private User user;
    private Department department;
    private CreateConversationDTO createDTO;
    private UpdateConversationDTO updateDTO;
    private UUID conversationId;
    private UUID customerId;
    private UUID userId;
    private UUID departmentId;
    
    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        
        customer = Customer.builder()
                .id(customerId)
                .phone("+5511999999001")
                .name("João Silva")
                .build();
        
        department = Department.builder()
                .id(departmentId)
                .name("Comercial")
                .build();
        
        user = User.builder()
                .id(userId)
                .name("Agent Silva")
                .email("agent@test.com")
                .role(UserRole.AGENT)
                .department(department)
                .build();
        
        conversation = Conversation.builder()
                .id(conversationId)
                .customer(customer)
                .assignedUser(user)
                .department(department)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(0)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateConversationDTO.builder()
                .customerId(customerId)
                .assignedUserId(userId)
                .departmentId(departmentId)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(0)
                .isPinned(false)
                .build();
        
        updateDTO = UpdateConversationDTO.builder()
                .status(ConversationStatus.ESPERANDO)
                .priority(1)
                .isPinned(true)
                .build();
    }
    
    @Test
    void create_ShouldCreateConversation_WhenValidData() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        
        ConversationDTO result = conversationService.create(createDTO);
        
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        assertThat(result.getAssignedUserId()).isEqualTo(userId);
        assertThat(result.getDepartmentId()).isEqualTo(departmentId);
        assertThat(result.getStatus()).isEqualTo(ConversationStatus.ENTRADA);
        
        verify(customerRepository).findById(customerId);
        verify(userRepository).findById(userId);
        verify(departmentRepository).findById(departmentId);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenCustomerNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> conversationService.create(createDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cliente não encontrado");
        
        verify(customerRepository).findById(customerId);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenUserNotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> conversationService.create(createDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usuário não encontrado");
        
        verify(customerRepository).findById(customerId);
        verify(userRepository).findById(userId);
        verify(conversationRepository, never()).save(any(Conversation.class));
    }
    
    @Test
    void findById_ShouldReturnConversation_WhenExists() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        
        ConversationDTO result = conversationService.findById(conversationId);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(conversationId);
        assertThat(result.getCustomerName()).isEqualTo("João Silva");
        
        verify(conversationRepository).findById(conversationId);
    }
    
    @Test
    void findById_ShouldThrowException_WhenNotExists() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> conversationService.findById(conversationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
        
        verify(conversationRepository).findById(conversationId);
    }
    
    @Test
    void findByStatus_ShouldReturnConversations() {
        when(conversationRepository.findByStatusOrderedByPriorityAndUpdatedAt(ConversationStatus.ENTRADA))
                .thenReturn(List.of(conversation));
        
        List<ConversationDTO> result = conversationService.findByStatus(ConversationStatus.ENTRADA);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ConversationStatus.ENTRADA);
        
        verify(conversationRepository).findByStatusOrderedByPriorityAndUpdatedAt(ConversationStatus.ENTRADA);
    }
    
    @Test
    void assignToUser_ShouldAssignUserAndChangeStatus() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        
        ConversationDTO result = conversationService.assignToUser(conversationId, userId);
        
        assertThat(result).isNotNull();
        
        verify(conversationRepository).findById(conversationId);
        verify(userRepository).findById(userId);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void changeStatus_ShouldUpdateStatus() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        
        ConversationDTO result = conversationService.changeStatus(conversationId, ConversationStatus.FINALIZADOS);
        
        assertThat(result).isNotNull();
        
        verify(conversationRepository).findById(conversationId);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void pinConversation_ShouldTogglePinStatus() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        
        ConversationDTO result = conversationService.pinConversation(conversationId);
        
        assertThat(result).isNotNull();
        
        verify(conversationRepository).findById(conversationId);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void update_ShouldUpdateConversation_WhenValidData() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        
        ConversationDTO result = conversationService.update(conversationId, updateDTO);
        
        assertThat(result).isNotNull();
        
        verify(conversationRepository).findById(conversationId);
        verify(conversationRepository).save(any(Conversation.class));
    }
    
    @Test
    void delete_ShouldDeleteConversation_WhenExists() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        
        conversationService.delete(conversationId);
        
        verify(conversationRepository).existsById(conversationId);
        verify(conversationRepository).deleteById(conversationId);
    }
    
    @Test
    void delete_ShouldThrowException_WhenNotExists() {
        when(conversationRepository.existsById(conversationId)).thenReturn(false);
        
        assertThatThrownBy(() -> conversationService.delete(conversationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrada");
        
        verify(conversationRepository).existsById(conversationId);
        verify(conversationRepository, never()).deleteById(any());
    }
    
    @Test
    void countByStatus_ShouldReturnCount() {
        when(conversationRepository.countByStatus(ConversationStatus.ENTRADA)).thenReturn(5L);
        
        long result = conversationService.countByStatus(ConversationStatus.ENTRADA);
        
        assertThat(result).isEqualTo(5L);
        
        verify(conversationRepository).countByStatus(ConversationStatus.ENTRADA);
    }
    
    @Test
    void countActiveByUser_ShouldReturnCount() {
        when(conversationRepository.countActiveConversationsByUser(userId)).thenReturn(3L);
        
        long result = conversationService.countActiveByUser(userId);
        
        assertThat(result).isEqualTo(3L);
        
        verify(conversationRepository).countActiveConversationsByUser(userId);
    }
}