package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageTemplateServiceTest {

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @Mock
    private MessageTemplateRevisionService messageTemplateRevisionService;

    @Mock
    private CompanyContextUtil companyContextUtil;

    @InjectMocks
    private MessageTemplateService messageTemplateService;
    
    private Company company;
    private User user;
    private AIAgent aiAgent;
    private MessageTemplate messageTemplate;
    private CreateMessageTemplateDTO createDTO;
    private UUID companyId;
    private UUID userId;
    private UUID aiAgentId;
    private UUID messageTemplateId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        aiAgentId = UUID.randomUUID();
        messageTemplateId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .build();

        aiAgent = AIAgent.builder()
                .id(aiAgentId)
                .name("Test AI Agent")
                .build();

        createDTO = CreateMessageTemplateDTO.builder()
                .companyId(companyId)
                .name("Test Template")
                .content("Hello {customerName}, how are you?")
                .isAiGenerated(false)
                .tone("FORMAL")
                .build();

        messageTemplate = MessageTemplate.builder()
                .id(messageTemplateId)
                .company(company)
                .name(createDTO.getName())
                .content(createDTO.getContent())
                .isAiGenerated(createDTO.getIsAiGenerated())
                .createdBy(user)
                .tone(createDTO.getTone())
                .editCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Mock CompanyContextUtil to return our test user
        lenient().when(companyContextUtil.getAuthenticatedUser()).thenReturn(user);
        lenient().when(companyContextUtil.getAuthenticatedUserId()).thenReturn(userId);
    }

    @Test
    void createMessageTemplate_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository, never()).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldThrowException_WhenAIAgentNotFound() {
        // Given
        CreateMessageTemplateDTO dtoWithAiAgent = CreateMessageTemplateDTO.builder()
                .companyId(companyId)
                .name("Test Template")
                .content("Hello there!")
                .isAiGenerated(false)
                .aiAgentId(aiAgentId)
                .build();
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateService.create(dtoWithAiAgent));
        
        assertEquals("AIAgent not found with ID: " + aiAgentId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(messageTemplateRepository, never()).save(any(MessageTemplate.class));
    }

    @Test
    void getMessageTemplateById_ShouldReturnMessageTemplate_WhenExists() {
        // Given
        when(messageTemplateRepository.findByIdAndNotDeleted(messageTemplateId)).thenReturn(Optional.of(messageTemplate));

        // When
        Optional<MessageTemplate> result = messageTemplateService.findById(messageTemplateId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(messageTemplate.getId(), result.get().getId());
        assertEquals(messageTemplate.getName(), result.get().getName());
        
        verify(messageTemplateRepository).findByIdAndNotDeleted(messageTemplateId);
    }

    @Test
    void getMessageTemplateById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(messageTemplateRepository.findByIdAndNotDeleted(messageTemplateId)).thenReturn(Optional.empty());

        // When
        Optional<MessageTemplate> result = messageTemplateService.findById(messageTemplateId);

        // Then
        assertTrue(result.isEmpty());
        verify(messageTemplateRepository).findByIdAndNotDeleted(messageTemplateId);
    }

    @Test
    void deleteMessageTemplate_ShouldReturnTrue_WhenExists() {
        // Given
        when(messageTemplateRepository.existsById(messageTemplateId)).thenReturn(true);

        // When
        boolean result = messageTemplateService.deleteById(messageTemplateId);

        // Then
        assertTrue(result);
        
        verify(messageTemplateRepository).existsById(messageTemplateId);
        verify(messageTemplateRepository).deleteById(messageTemplateId);
    }

    @Test
    void deleteMessageTemplate_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(messageTemplateRepository.existsById(messageTemplateId)).thenReturn(false);

        // When
        boolean result = messageTemplateService.deleteById(messageTemplateId);

        // Then
        assertFalse(result);
        
        verify(messageTemplateRepository).existsById(messageTemplateId);
        verify(messageTemplateRepository, never()).deleteById(messageTemplateId);
    }
}