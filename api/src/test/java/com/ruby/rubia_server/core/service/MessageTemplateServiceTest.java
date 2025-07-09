package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.ruby.rubia_server.core.exception.MessageTemplateTransactionException;

@ExtendWith(MockitoExtension.class)
class MessageTemplateServiceTest {

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @Mock
    private MessageTemplateRevisionService messageTemplateRevisionService;

    @InjectMocks
    private MessageTemplateService messageTemplateService;
    
    private Company company;
    private User user;
    private AIAgent aiAgent;
    private MessageTemplate messageTemplate;
    private CreateMessageTemplateDTO createDTO;
    private UpdateMessageTemplateDTO updateDTO;
    private UUID companyId;
    private UUID userId;
    private UUID aiAgentId;
    private UUID messageTemplateId;

    @BeforeEach
    void setUp() {
        // Set the failOnRevisionError field to true by default for tests
        try {
            java.lang.reflect.Field field = MessageTemplateService.class.getDeclaredField("failOnRevisionError");
            field.setAccessible(true);
            field.set(messageTemplateService, true);
        } catch (Exception e) {
            fail("Failed to set failOnRevisionError field");
        }
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
                .createdByUserId(userId)
                .tone("FORMAL")
                .build();

        updateDTO = UpdateMessageTemplateDTO.builder()
                .name("Updated Template")
                .content("Updated content")
                .tone("INFORMAL")
                .lastEditedByUserId(userId)
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
    }

    @Test
    void createMessageTemplate_ShouldCreateAndReturnMessageTemplate_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(messageTemplate.getId(), result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getContent(), result.getContent());
        assertEquals(createDTO.getIsAiGenerated(), result.getIsAiGenerated());
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(userId, result.getCreatedBy().getId());

        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldCreateWithoutOptionalEntities_WhenOnlyRequiredDataProvided() {
        // Given
        CreateMessageTemplateDTO minimalDTO = CreateMessageTemplateDTO.builder()
                .companyId(companyId)
                .name("Test Template")
                .content("Hello there!")
                .isAiGenerated(false)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        MessageTemplate result = messageTemplateService.create(minimalDTO);

        // Then
        assertNotNull(result);
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(minimalDTO.getName(), result.getName());
        // Note: result returns the mocked messageTemplate, not the actual created one

        verify(companyRepository).findById(companyId);
        verify(userRepository, never()).findById(any());
        verify(aiAgentRepository, never()).findById(any());
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
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
        verify(userRepository, never()).findById(userId);
        verify(messageTemplateRepository, never()).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldThrowException_WhenCreatedByUserNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateService.create(createDTO));
        
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository, never()).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldThrowException_WhenAIAgentNotFound() {
        // Given
        createDTO.setCreatedByUserId(null);
        createDTO.setAiAgentId(aiAgentId);
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateService.create(createDTO));
        
        assertEquals("AIAgent not found with ID: " + aiAgentId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(messageTemplateRepository, never()).save(any(MessageTemplate.class));
    }

    @Test
    void getMessageTemplateById_ShouldReturnMessageTemplate_WhenExists() {
        // Given
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));

        // When
        Optional<MessageTemplate> result = messageTemplateService.findById(messageTemplateId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(messageTemplate.getId(), result.get().getId());
        assertEquals(messageTemplate.getName(), result.get().getName());
        
        verify(messageTemplateRepository).findById(messageTemplateId);
    }

    @Test
    void getMessageTemplateById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.empty());

        // When
        Optional<MessageTemplate> result = messageTemplateService.findById(messageTemplateId);

        // Then
        assertTrue(result.isEmpty());
        verify(messageTemplateRepository).findById(messageTemplateId);
    }

    @Test
    void getAllMessageTemplates_ShouldReturnPagedResults() {
        // Given
        List<MessageTemplate> messageTemplates = List.of(messageTemplate);
        Page<MessageTemplate> page = new PageImpl<>(messageTemplates);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(messageTemplateRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<MessageTemplate> result = messageTemplateService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(messageTemplate.getId(), result.getContent().get(0).getId());
        
        verify(messageTemplateRepository).findAll(pageable);
    }

    @Test
    void getMessageTemplatesByCompanyId_ShouldReturnMessageTemplatesForCompany() {
        // Given
        List<MessageTemplate> messageTemplates = List.of(messageTemplate);
        when(messageTemplateRepository.findByCompanyId(companyId)).thenReturn(messageTemplates);

        // When
        List<MessageTemplate> result = messageTemplateService.findByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplate.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(messageTemplateRepository).findByCompanyId(companyId);
    }

    @Test
    void updateMessageTemplate_ShouldUpdateAndReturnMessageTemplate_WhenValidData() {
        // Given
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        Optional<MessageTemplate> result = messageTemplateService.update(messageTemplateId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        MessageTemplate updated = result.get();
        assertEquals(messageTemplate.getId(), updated.getId());
        
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void updateMessageTemplate_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.empty());

        // When
        Optional<MessageTemplate> result = messageTemplateService.update(messageTemplateId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(messageTemplateRepository, never()).save(any(MessageTemplate.class));
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

    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(messageTemplateRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When
        long count = messageTemplateService.countByCompanyId(companyId);

        // Then
        assertEquals(5L, count);
        verify(messageTemplateRepository).countByCompanyId(companyId);
    }

    @Test
    void findByIsAiGenerated_ShouldReturnMessageTemplatesWithSpecificFlag() {
        // Given
        List<MessageTemplate> messageTemplates = List.of(messageTemplate);
        when(messageTemplateRepository.findByIsAiGenerated(true)).thenReturn(messageTemplates);

        // When
        List<MessageTemplate> result = messageTemplateService.findByIsAiGenerated(true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplate.getId(), result.get(0).getId());
        verify(messageTemplateRepository).findByIsAiGenerated(true);
    }

    @Test
    void findByCreatedByUserId_ShouldReturnMessageTemplatesForUser() {
        // Given
        List<MessageTemplate> messageTemplates = List.of(messageTemplate);
        when(messageTemplateRepository.findByCreatedById(userId)).thenReturn(messageTemplates);

        // When
        List<MessageTemplate> result = messageTemplateService.findByCreatedByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplate.getId(), result.get(0).getId());
        assertEquals(userId, result.get(0).getCreatedBy().getId());
        verify(messageTemplateRepository).findByCreatedById(userId);
    }

    @Test
    void findByCompanyIdAndIsAiGenerated_ShouldReturnFilteredMessageTemplates() {
        // Given
        List<MessageTemplate> messageTemplates = List.of(messageTemplate);
        when(messageTemplateRepository.findByCompanyIdAndIsAiGenerated(companyId, false)).thenReturn(messageTemplates);

        // When
        List<MessageTemplate> result = messageTemplateService.findByCompanyIdAndIsAiGenerated(companyId, false);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplate.getId(), result.get(0).getId());
        verify(messageTemplateRepository).findByCompanyIdAndIsAiGenerated(companyId, false);
    }

    @Test
    void findByNameContaining_ShouldReturnMatchingMessageTemplates() {
        // Given
        String searchTerm = "Test";
        List<MessageTemplate> messageTemplates = List.of(messageTemplate);
        when(messageTemplateRepository.findByNameContainingIgnoreCase(searchTerm)).thenReturn(messageTemplates);

        // When
        List<MessageTemplate> result = messageTemplateService.findByNameContaining(searchTerm);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplate.getId(), result.get(0).getId());
        verify(messageTemplateRepository).findByNameContainingIgnoreCase(searchTerm);
    }

    @Test
    void existsByNameAndCompanyId_ShouldReturnTrue_WhenExists() {
        // Given
        when(messageTemplateRepository.existsByNameAndCompanyId("Test Template", companyId)).thenReturn(true);

        // When
        boolean result = messageTemplateService.existsByNameAndCompanyId("Test Template", companyId);

        // Then
        assertTrue(result);
        verify(messageTemplateRepository).existsByNameAndCompanyId("Test Template", companyId);
    }

    @Test
    void incrementEditCount_ShouldUpdateEditCountAndLastEditor() {
        // Given
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        Optional<MessageTemplate> result = messageTemplateService.incrementEditCount(messageTemplateId, userId);

        // Then
        assertTrue(result.isPresent());
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void cloneTemplate_ShouldCreateCopyWithNewName() {
        // Given
        String newName = "Cloned Template";
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        Optional<MessageTemplate> result = messageTemplateService.cloneTemplate(messageTemplateId, newName);

        // Then
        assertTrue(result.isPresent());
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldCreateInitialRevision_WhenCreatedByUserIdProvided() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(messageTemplate.getId(), result.getId());
        
        // Verify that initial revision is created
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(messageTemplate.getId()),
            eq(createDTO.getContent()),
            eq(userId)
        );
        
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldNotCreateInitialRevision_WhenCreatedByUserIdNotProvided() {
        // Given
        CreateMessageTemplateDTO dtoWithoutUser = CreateMessageTemplateDTO.builder()
                .companyId(companyId)
                .name("Test Template")
                .content("Hello there!")
                .isAiGenerated(false)
                .build();
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        MessageTemplate result = messageTemplateService.create(dtoWithoutUser);

        // Then
        assertNotNull(result);
        
        // Verify that no revision is created when user ID is not provided
        verify(messageTemplateRevisionService, never()).createRevisionFromTemplate(any(), any(), any());
        
        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void updateMessageTemplate_ShouldCreateRevision_WhenContentChangesAndUserProvided() {
        // Given
        String originalContent = "Original content";
        String newContent = "Updated content";
        
        messageTemplate.setContent(originalContent);
        
        UpdateMessageTemplateDTO updateWithContentChange = UpdateMessageTemplateDTO.builder()
                .name("Updated Template")
                .content(newContent)
                .lastEditedByUserId(userId)
                .build();
        
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        Optional<MessageTemplate> result = messageTemplateService.update(messageTemplateId, updateWithContentChange);

        // Then
        assertTrue(result.isPresent());
        
        // Verify that revision is created when content changes
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(messageTemplateId),
            eq(newContent),
            eq(userId)
        );
        
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void updateMessageTemplate_ShouldNotCreateRevision_WhenContentDoesNotChange() {
        // Given
        String originalContent = "Original content";
        
        messageTemplate.setContent(originalContent);
        
        UpdateMessageTemplateDTO updateWithoutContentChange = UpdateMessageTemplateDTO.builder()
                .name("Updated Template Name Only")
                .content(originalContent) // Same content
                .lastEditedByUserId(userId)
                .build();
        
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        Optional<MessageTemplate> result = messageTemplateService.update(messageTemplateId, updateWithoutContentChange);

        // Then
        assertTrue(result.isPresent());
        
        // Verify that no revision is created when content doesn't change
        verify(messageTemplateRevisionService, never()).createRevisionFromTemplate(any(), any(), any());
        
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void updateMessageTemplate_ShouldNotCreateRevision_WhenUserNotProvided() {
        // Given
        String originalContent = "Original content";
        String newContent = "Updated content";
        
        messageTemplate.setContent(originalContent);
        
        UpdateMessageTemplateDTO updateWithoutUser = UpdateMessageTemplateDTO.builder()
                .name("Updated Template")
                .content(newContent)
                .build(); // No lastEditedByUserId
        
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);

        // When
        Optional<MessageTemplate> result = messageTemplateService.update(messageTemplateId, updateWithoutUser);

        // Then
        assertTrue(result.isPresent());
        
        // Verify that no revision is created when user is not provided
        verify(messageTemplateRevisionService, never()).createRevisionFromTemplate(any(), any(), any());
        
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void createMessageTemplate_ShouldRollbackTransaction_WhenRevisionCreationFails() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);
        
        // Mock revision service to throw exception
        doThrow(new RuntimeException("Revision creation failed")).when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When & Then
        MessageTemplateTransactionException exception = assertThrows(MessageTemplateTransactionException.class, 
            () -> messageTemplateService.create(createDTO));
        
        assertEquals("Failed to create initial revision for template", exception.getMessage());
        
        // Verify that revision creation was attempted
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(messageTemplate.getId()),
            eq(createDTO.getContent()),
            eq(userId)
        );
        
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }

    @Test
    void updateMessageTemplate_ShouldRollbackTransaction_WhenRevisionCreationFails() {
        // Given
        String originalContent = "Original content";
        String newContent = "Updated content";
        
        messageTemplate.setContent(originalContent);
        
        UpdateMessageTemplateDTO updateWithContentChange = UpdateMessageTemplateDTO.builder()
                .content(newContent)
                .lastEditedByUserId(userId)
                .build();
        
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        // Mock revision service to throw exception
        doThrow(new RuntimeException("Revision creation failed")).when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When & Then
        MessageTemplateTransactionException exception = assertThrows(MessageTemplateTransactionException.class, 
            () -> messageTemplateService.update(messageTemplateId, updateWithContentChange));
        
        assertEquals("Failed to create revision for template update", exception.getMessage());
        
        // Verify that revision creation was attempted
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(messageTemplateId),
            eq(newContent),
            eq(userId)
        );
        
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        // Note: save should not be called due to rollback
    }
    
    @Test
    void createMessageTemplate_ShouldContinueCreation_WhenRevisionCreationFailsAndConfigDisabled() {
        // Given
        // Use reflection to set the failOnRevisionError field to false
        try {
            java.lang.reflect.Field field = MessageTemplateService.class.getDeclaredField("failOnRevisionError");
            field.setAccessible(true);
            field.set(messageTemplateService, false);
        } catch (Exception e) {
            fail("Failed to set failOnRevisionError field");
        }
        
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);
        
        // Mock revision service to throw exception
        doThrow(new RuntimeException("Revision creation failed")).when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(messageTemplate.getId(), result.getId());
        
        // Verify that revision creation was attempted but template was still created
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(messageTemplate.getId()),
            eq(createDTO.getContent()),
            eq(userId)
        );
        
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
    }
}