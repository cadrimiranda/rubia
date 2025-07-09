package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateConversationParticipantDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationParticipantDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationParticipantServiceTest {

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @InjectMocks
    private ConversationParticipantService conversationParticipantService;

    private Company company;
    private Conversation conversation;
    private Customer customer;
    private User user;
    private AIAgent aiAgent;
    private ConversationParticipant conversationParticipant;
    private CreateConversationParticipantDTO createDTO;
    private UpdateConversationParticipantDTO updateDTO;
    private UUID companyId;
    private UUID conversationId;
    private UUID customerId;
    private UUID userId;
    private UUID aiAgentId;
    private UUID conversationParticipantId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
        aiAgentId = UUID.randomUUID();
        conversationParticipantId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        conversation = Conversation.builder()
                .id(conversationId)
                .company(company)
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .build();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .build();

        aiAgent = AIAgent.builder()
                .id(aiAgentId)
                .name("Test AI Agent")
                .build();

        createDTO = CreateConversationParticipantDTO.builder()
                .companyId(companyId)
                .conversationId(conversationId)
                .customerId(customerId)
                .isActive(true)
                .build();

        updateDTO = UpdateConversationParticipantDTO.builder()
                .isActive(false)
                .leftAt(LocalDateTime.now())
                .build();

        conversationParticipant = ConversationParticipant.builder()
                .id(conversationParticipantId)
                .company(company)
                .conversation(conversation)
                .customer(customer)
                .isActive(createDTO.getIsActive())
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createConversationParticipant_ShouldCreateAndReturnConversationParticipant_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(conversationParticipant);

        // When
        ConversationParticipant result = conversationParticipantService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(conversationParticipant.getId(), result.getId());
        assertEquals(createDTO.getIsActive(), result.getIsActive());
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(conversationId, result.getConversation().getId());
        assertEquals(customerId, result.getCustomer().getId());

        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(customerRepository).findById(customerId);
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldCreateWithUser_WhenUserProvided() {
        // Given
        createDTO.setCustomerId(null);
        createDTO.setUserId(userId);
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(conversationParticipant);

        // When
        ConversationParticipant result = conversationParticipantService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(conversationId, result.getConversation().getId());

        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(userRepository).findById(userId);
        verify(customerRepository, never()).findById(any());
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldCreateWithAIAgent_WhenAIAgentProvided() {
        // Given
        createDTO.setCustomerId(null);
        createDTO.setAiAgentId(aiAgentId);
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(conversationParticipant);

        // When
        ConversationParticipant result = conversationParticipantService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(conversationId, result.getConversation().getId());

        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(customerRepository, never()).findById(any());
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationParticipantService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository, never()).findById(conversationId);
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldThrowException_WhenConversationNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationParticipantService.create(createDTO));
        
        assertEquals("Conversation not found with ID: " + conversationId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldThrowException_WhenCustomerNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationParticipantService.create(createDTO));
        
        assertEquals("Customer not found with ID: " + customerId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(customerRepository).findById(customerId);
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldThrowException_WhenUserNotFound() {
        // Given
        createDTO.setCustomerId(null);
        createDTO.setUserId(userId);
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationParticipantService.create(createDTO));
        
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(userRepository).findById(userId);
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldThrowException_WhenAIAgentNotFound() {
        // Given
        createDTO.setCustomerId(null);
        createDTO.setAiAgentId(aiAgentId);
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationParticipantService.create(createDTO));
        
        assertEquals("AIAgent not found with ID: " + aiAgentId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(conversationRepository).findById(conversationId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void createConversationParticipant_ShouldThrowException_WhenNoParticipantProvided() {
        // Given
        createDTO.setCustomerId(null);
        createDTO.setUserId(null);
        createDTO.setAiAgentId(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> conversationParticipantService.create(createDTO));
        
        assertEquals("Exactly one participant (customer, user, or AI agent) must be provided", exception.getMessage());
        // Since validation fails early, repository methods should not be called
        verify(companyRepository, never()).findById(any());
        verify(conversationRepository, never()).findById(any());
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void getConversationParticipantById_ShouldReturnConversationParticipant_WhenExists() {
        // Given
        when(conversationParticipantRepository.findById(conversationParticipantId)).thenReturn(Optional.of(conversationParticipant));

        // When
        Optional<ConversationParticipant> result = conversationParticipantService.findById(conversationParticipantId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(conversationParticipant.getId(), result.get().getId());
        assertEquals(conversationParticipant.getIsActive(), result.get().getIsActive());
        
        verify(conversationParticipantRepository).findById(conversationParticipantId);
    }

    @Test
    void getConversationParticipantById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(conversationParticipantRepository.findById(conversationParticipantId)).thenReturn(Optional.empty());

        // When
        Optional<ConversationParticipant> result = conversationParticipantService.findById(conversationParticipantId);

        // Then
        assertTrue(result.isEmpty());
        verify(conversationParticipantRepository).findById(conversationParticipantId);
    }

    @Test
    void getAllConversationParticipants_ShouldReturnPagedResults() {
        // Given
        List<ConversationParticipant> conversationParticipants = List.of(conversationParticipant);
        Page<ConversationParticipant> page = new PageImpl<>(conversationParticipants);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(conversationParticipantRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<ConversationParticipant> result = conversationParticipantService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(conversationParticipant.getId(), result.getContent().get(0).getId());
        
        verify(conversationParticipantRepository).findAll(pageable);
    }

    @Test
    void getConversationParticipantsByCompanyId_ShouldReturnConversationParticipantsForCompany() {
        // Given
        List<ConversationParticipant> conversationParticipants = List.of(conversationParticipant);
        when(conversationParticipantRepository.findByCompanyId(companyId)).thenReturn(conversationParticipants);

        // When
        List<ConversationParticipant> result = conversationParticipantService.findByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationParticipant.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(conversationParticipantRepository).findByCompanyId(companyId);
    }

    @Test
    void updateConversationParticipant_ShouldUpdateAndReturnConversationParticipant_WhenValidData() {
        // Given
        when(conversationParticipantRepository.findById(conversationParticipantId)).thenReturn(Optional.of(conversationParticipant));
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(conversationParticipant);

        // When
        Optional<ConversationParticipant> result = conversationParticipantService.update(conversationParticipantId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        ConversationParticipant updated = result.get();
        assertEquals(conversationParticipant.getId(), updated.getId());
        
        verify(conversationParticipantRepository).findById(conversationParticipantId);
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    void updateConversationParticipant_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(conversationParticipantRepository.findById(conversationParticipantId)).thenReturn(Optional.empty());

        // When
        Optional<ConversationParticipant> result = conversationParticipantService.update(conversationParticipantId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(conversationParticipantRepository).findById(conversationParticipantId);
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    void deleteConversationParticipant_ShouldReturnTrue_WhenExists() {
        // Given
        when(conversationParticipantRepository.existsById(conversationParticipantId)).thenReturn(true);

        // When
        boolean result = conversationParticipantService.deleteById(conversationParticipantId);

        // Then
        assertTrue(result);
        
        verify(conversationParticipantRepository).existsById(conversationParticipantId);
        verify(conversationParticipantRepository).deleteById(conversationParticipantId);
    }

    @Test
    void deleteConversationParticipant_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(conversationParticipantRepository.existsById(conversationParticipantId)).thenReturn(false);

        // When
        boolean result = conversationParticipantService.deleteById(conversationParticipantId);

        // Then
        assertFalse(result);
        
        verify(conversationParticipantRepository).existsById(conversationParticipantId);
        verify(conversationParticipantRepository, never()).deleteById(conversationParticipantId);
    }

    @Test
    void findByConversationId_ShouldReturnParticipantsForConversation() {
        // Given
        List<ConversationParticipant> conversationParticipants = List.of(conversationParticipant);
        when(conversationParticipantRepository.findByConversationId(conversationId)).thenReturn(conversationParticipants);

        // When
        List<ConversationParticipant> result = conversationParticipantService.findByConversationId(conversationId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationParticipant.getId(), result.get(0).getId());
        assertEquals(conversationId, result.get(0).getConversation().getId());
        verify(conversationParticipantRepository).findByConversationId(conversationId);
    }

    @Test
    void findActiveByConversationId_ShouldReturnActiveParticipants() {
        // Given
        List<ConversationParticipant> conversationParticipants = List.of(conversationParticipant);
        when(conversationParticipantRepository.findByConversationIdAndIsActive(conversationId, true)).thenReturn(conversationParticipants);

        // When
        List<ConversationParticipant> result = conversationParticipantService.findActiveByConversationId(conversationId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationParticipant.getId(), result.get(0).getId());
        verify(conversationParticipantRepository).findByConversationIdAndIsActive(conversationId, true);
    }

    @Test
    void findByCustomerId_ShouldReturnParticipantsForCustomer() {
        // Given
        List<ConversationParticipant> conversationParticipants = List.of(conversationParticipant);
        when(conversationParticipantRepository.findByCustomerId(customerId)).thenReturn(conversationParticipants);

        // When
        List<ConversationParticipant> result = conversationParticipantService.findByCustomerId(customerId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationParticipant.getId(), result.get(0).getId());
        assertEquals(customerId, result.get(0).getCustomer().getId());
        verify(conversationParticipantRepository).findByCustomerId(customerId);
    }

    @Test
    void leaveConversation_ShouldMarkAsInactive() {
        // Given
        when(conversationParticipantRepository.findById(conversationParticipantId)).thenReturn(Optional.of(conversationParticipant));
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(conversationParticipant);

        // When
        Optional<ConversationParticipant> result = conversationParticipantService.leaveConversation(conversationParticipantId);

        // Then
        assertTrue(result.isPresent());
        verify(conversationParticipantRepository).findById(conversationParticipantId);
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }

    @Test
    void rejoinConversation_ShouldMarkAsActive() {
        // Given
        conversationParticipant.setIsActive(false);
        conversationParticipant.setLeftAt(LocalDateTime.now());
        
        when(conversationParticipantRepository.findById(conversationParticipantId)).thenReturn(Optional.of(conversationParticipant));
        when(conversationParticipantRepository.save(any(ConversationParticipant.class))).thenReturn(conversationParticipant);

        // When
        Optional<ConversationParticipant> result = conversationParticipantService.rejoinConversation(conversationParticipantId);

        // Then
        assertTrue(result.isPresent());
        verify(conversationParticipantRepository).findById(conversationParticipantId);
        verify(conversationParticipantRepository).save(any(ConversationParticipant.class));
    }
}