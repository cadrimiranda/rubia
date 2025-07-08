package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateMessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateRevisionDTO;
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
class MessageTemplateRevisionServiceMockTest {

    @Mock
    private MessageTemplateRevisionRepository messageTemplateRevisionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @InjectMocks
    private MessageTemplateRevisionService messageTemplateRevisionService;

    private Company company;
    private MessageTemplate messageTemplate;
    private User user;
    private MessageTemplateRevision messageTemplateRevision;
    private CreateMessageTemplateRevisionDTO createDTO;
    private UpdateMessageTemplateRevisionDTO updateDTO;
    private UUID companyId;
    private UUID messageTemplateId;
    private UUID userId;
    private UUID messageTemplateRevisionId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        messageTemplateId = UUID.randomUUID();
        userId = UUID.randomUUID();
        messageTemplateRevisionId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        messageTemplate = MessageTemplate.builder()
                .id(messageTemplateId)
                .name("Test Template")
                .content("Original content")
                .build();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .build();

        createDTO = CreateMessageTemplateRevisionDTO.builder()
                .companyId(companyId)
                .templateId(messageTemplateId)
                .revisionNumber(2)
                .content("Revised content")
                .editedByUserId(userId)
                .build();

        updateDTO = UpdateMessageTemplateRevisionDTO.builder()
                .content("Updated content")
                .editedByUserId(userId)
                .build();

        messageTemplateRevision = MessageTemplateRevision.builder()
                .id(messageTemplateRevisionId)
                .company(company)
                .template(messageTemplate)
                .revisionNumber(createDTO.getRevisionNumber())
                .content(createDTO.getContent())
                .editedBy(user)
                .revisionTimestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createMessageTemplateRevision_ShouldCreateAndReturnMessageTemplateRevision_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRevisionRepository.save(any(MessageTemplateRevision.class))).thenReturn(messageTemplateRevision);

        // When
        MessageTemplateRevision result = messageTemplateRevisionService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(messageTemplateRevision.getId(), result.getId());
        assertEquals(createDTO.getRevisionNumber(), result.getRevisionNumber());
        assertEquals(createDTO.getContent(), result.getContent());
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(messageTemplateId, result.getTemplate().getId());
        assertEquals(userId, result.getEditedBy().getId());

        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRevisionRepository).save(any(MessageTemplateRevision.class));
    }

    @Test
    void createMessageTemplateRevision_ShouldCreateWithoutEditedBy_WhenNoUserProvided() {
        // Given
        CreateMessageTemplateRevisionDTO minimalDTO = CreateMessageTemplateRevisionDTO.builder()
                .companyId(companyId)
                .templateId(messageTemplateId)
                .revisionNumber(2)
                .content("Revised content")
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(messageTemplateRevisionRepository.save(any(MessageTemplateRevision.class))).thenReturn(messageTemplateRevision);

        // When
        MessageTemplateRevision result = messageTemplateRevisionService.create(minimalDTO);

        // Then
        assertNotNull(result);
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(messageTemplateId, result.getTemplate().getId());
        assertEquals(minimalDTO.getRevisionNumber(), result.getRevisionNumber());

        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository, never()).findById(any());
        verify(messageTemplateRevisionRepository).save(any(MessageTemplateRevision.class));
    }

    @Test
    void createMessageTemplateRevision_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateRevisionService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository, never()).findById(messageTemplateId);
        verify(userRepository, never()).findById(userId);
        verify(messageTemplateRevisionRepository, never()).save(any(MessageTemplateRevision.class));
    }

    @Test
    void createMessageTemplateRevision_ShouldThrowException_WhenTemplateNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateRevisionService.create(createDTO));
        
        assertEquals("MessageTemplate not found with ID: " + messageTemplateId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository, never()).findById(userId);
        verify(messageTemplateRevisionRepository, never()).save(any(MessageTemplateRevision.class));
    }

    @Test
    void createMessageTemplateRevision_ShouldThrowException_WhenEditedByUserNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> messageTemplateRevisionService.create(createDTO));
        
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRevisionRepository, never()).save(any(MessageTemplateRevision.class));
    }

    @Test
    void getMessageTemplateRevisionById_ShouldReturnMessageTemplateRevision_WhenExists() {
        // Given
        when(messageTemplateRevisionRepository.findById(messageTemplateRevisionId)).thenReturn(Optional.of(messageTemplateRevision));

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.findById(messageTemplateRevisionId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(messageTemplateRevision.getId(), result.get().getId());
        assertEquals(messageTemplateRevision.getRevisionNumber(), result.get().getRevisionNumber());
        
        verify(messageTemplateRevisionRepository).findById(messageTemplateRevisionId);
    }

    @Test
    void getMessageTemplateRevisionById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(messageTemplateRevisionRepository.findById(messageTemplateRevisionId)).thenReturn(Optional.empty());

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.findById(messageTemplateRevisionId);

        // Then
        assertTrue(result.isEmpty());
        verify(messageTemplateRevisionRepository).findById(messageTemplateRevisionId);
    }

    @Test
    void getAllMessageTemplateRevisions_ShouldReturnPagedResults() {
        // Given
        List<MessageTemplateRevision> messageTemplateRevisions = List.of(messageTemplateRevision);
        Page<MessageTemplateRevision> page = new PageImpl<>(messageTemplateRevisions);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(messageTemplateRevisionRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<MessageTemplateRevision> result = messageTemplateRevisionService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(messageTemplateRevision.getId(), result.getContent().get(0).getId());
        
        verify(messageTemplateRevisionRepository).findAll(pageable);
    }

    @Test
    void getMessageTemplateRevisionsByTemplateId_ShouldReturnRevisionsForTemplate() {
        // Given
        List<MessageTemplateRevision> messageTemplateRevisions = List.of(messageTemplateRevision);
        when(messageTemplateRevisionRepository.findByTemplateId(messageTemplateId)).thenReturn(messageTemplateRevisions);

        // When
        List<MessageTemplateRevision> result = messageTemplateRevisionService.findByTemplateId(messageTemplateId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplateRevision.getId(), result.get(0).getId());
        assertEquals(messageTemplateId, result.get(0).getTemplate().getId());
        
        verify(messageTemplateRevisionRepository).findByTemplateId(messageTemplateId);
    }

    @Test
    void updateMessageTemplateRevision_ShouldUpdateAndReturnMessageTemplateRevision_WhenValidData() {
        // Given
        when(messageTemplateRevisionRepository.findById(messageTemplateRevisionId)).thenReturn(Optional.of(messageTemplateRevision));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRevisionRepository.save(any(MessageTemplateRevision.class))).thenReturn(messageTemplateRevision);

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.update(messageTemplateRevisionId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        MessageTemplateRevision updated = result.get();
        assertEquals(messageTemplateRevision.getId(), updated.getId());
        
        verify(messageTemplateRevisionRepository).findById(messageTemplateRevisionId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRevisionRepository).save(any(MessageTemplateRevision.class));
    }

    @Test
    void updateMessageTemplateRevision_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(messageTemplateRevisionRepository.findById(messageTemplateRevisionId)).thenReturn(Optional.empty());

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.update(messageTemplateRevisionId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(messageTemplateRevisionRepository).findById(messageTemplateRevisionId);
        verify(messageTemplateRevisionRepository, never()).save(any(MessageTemplateRevision.class));
    }

    @Test
    void deleteMessageTemplateRevision_ShouldReturnTrue_WhenExists() {
        // Given
        when(messageTemplateRevisionRepository.existsById(messageTemplateRevisionId)).thenReturn(true);

        // When
        boolean result = messageTemplateRevisionService.deleteById(messageTemplateRevisionId);

        // Then
        assertTrue(result);
        
        verify(messageTemplateRevisionRepository).existsById(messageTemplateRevisionId);
        verify(messageTemplateRevisionRepository).deleteById(messageTemplateRevisionId);
    }

    @Test
    void deleteMessageTemplateRevision_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(messageTemplateRevisionRepository.existsById(messageTemplateRevisionId)).thenReturn(false);

        // When
        boolean result = messageTemplateRevisionService.deleteById(messageTemplateRevisionId);

        // Then
        assertFalse(result);
        
        verify(messageTemplateRevisionRepository).existsById(messageTemplateRevisionId);
        verify(messageTemplateRevisionRepository, never()).deleteById(messageTemplateRevisionId);
    }

    @Test
    void countByTemplateId_ShouldReturnCorrectCount() {
        // Given
        when(messageTemplateRevisionRepository.countByTemplateId(messageTemplateId)).thenReturn(5L);

        // When
        long count = messageTemplateRevisionService.countByTemplateId(messageTemplateId);

        // Then
        assertEquals(5L, count);
        verify(messageTemplateRevisionRepository).countByTemplateId(messageTemplateId);
    }

    @Test
    void findByEditedByUserId_ShouldReturnRevisionsForUser() {
        // Given
        List<MessageTemplateRevision> messageTemplateRevisions = List.of(messageTemplateRevision);
        when(messageTemplateRevisionRepository.findByEditedById(userId)).thenReturn(messageTemplateRevisions);

        // When
        List<MessageTemplateRevision> result = messageTemplateRevisionService.findByEditedByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplateRevision.getId(), result.get(0).getId());
        assertEquals(userId, result.get(0).getEditedBy().getId());
        verify(messageTemplateRevisionRepository).findByEditedById(userId);
    }

    @Test
    void findByTemplateIdAndRevisionNumber_ShouldReturnSpecificRevision() {
        // Given
        when(messageTemplateRevisionRepository.findByTemplateIdAndRevisionNumber(messageTemplateId, 2)).thenReturn(Optional.of(messageTemplateRevision));

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.findByTemplateIdAndRevisionNumber(messageTemplateId, 2);

        // Then
        assertTrue(result.isPresent());
        assertEquals(messageTemplateRevision.getId(), result.get().getId());
        assertEquals(2, result.get().getRevisionNumber());
        verify(messageTemplateRevisionRepository).findByTemplateIdAndRevisionNumber(messageTemplateId, 2);
    }

    @Test
    void findByTemplateIdOrderByRevisionNumberDesc_ShouldReturnRevisionsInDescendingOrder() {
        // Given
        List<MessageTemplateRevision> messageTemplateRevisions = List.of(messageTemplateRevision);
        when(messageTemplateRevisionRepository.findByTemplateIdOrderByRevisionNumberDesc(messageTemplateId)).thenReturn(messageTemplateRevisions);

        // When
        List<MessageTemplateRevision> result = messageTemplateRevisionService.findByTemplateIdOrderByRevisionNumberDesc(messageTemplateId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplateRevision.getId(), result.get(0).getId());
        verify(messageTemplateRevisionRepository).findByTemplateIdOrderByRevisionNumberDesc(messageTemplateId);
    }

    @Test
    void getLatestRevision_ShouldReturnMostRecentRevision() {
        // Given
        when(messageTemplateRevisionRepository.findFirstByTemplateIdOrderByRevisionNumberDesc(messageTemplateId)).thenReturn(Optional.of(messageTemplateRevision));

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.getLatestRevision(messageTemplateId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(messageTemplateRevision.getId(), result.get().getId());
        verify(messageTemplateRevisionRepository).findFirstByTemplateIdOrderByRevisionNumberDesc(messageTemplateId);
    }

    @Test
    void getOriginalRevision_ShouldReturnFirstRevision() {
        // Given
        when(messageTemplateRevisionRepository.findByTemplateIdAndRevisionNumber(messageTemplateId, 1)).thenReturn(Optional.of(messageTemplateRevision));

        // When
        Optional<MessageTemplateRevision> result = messageTemplateRevisionService.getOriginalRevision(messageTemplateId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(messageTemplateRevision.getId(), result.get().getId());
        verify(messageTemplateRevisionRepository).findByTemplateIdAndRevisionNumber(messageTemplateId, 1);
    }

    @Test
    void existsByTemplateIdAndRevisionNumber_ShouldReturnTrue_WhenExists() {
        // Given
        when(messageTemplateRevisionRepository.existsByTemplateIdAndRevisionNumber(messageTemplateId, 2)).thenReturn(true);

        // When
        boolean result = messageTemplateRevisionService.existsByTemplateIdAndRevisionNumber(messageTemplateId, 2);

        // Then
        assertTrue(result);
        verify(messageTemplateRevisionRepository).existsByTemplateIdAndRevisionNumber(messageTemplateId, 2);
    }

    @Test
    void findRevisionsBetweenNumbers_ShouldReturnRevisionsInRange() {
        // Given
        List<MessageTemplateRevision> messageTemplateRevisions = List.of(messageTemplateRevision);
        when(messageTemplateRevisionRepository.findByTemplateIdAndRevisionNumberBetweenOrderByRevisionNumber(messageTemplateId, 1, 3)).thenReturn(messageTemplateRevisions);

        // When
        List<MessageTemplateRevision> result = messageTemplateRevisionService.findRevisionsBetweenNumbers(messageTemplateId, 1, 3);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(messageTemplateRevision.getId(), result.get(0).getId());
        verify(messageTemplateRevisionRepository).findByTemplateIdAndRevisionNumberBetweenOrderByRevisionNumber(messageTemplateId, 1, 3);
    }

    @Test
    void getNextRevisionNumber_ShouldReturnCorrectNextNumber() {
        // Given
        when(messageTemplateRevisionRepository.findMaxRevisionNumberByTemplateId(messageTemplateId)).thenReturn(Optional.of(3));

        // When
        Integer result = messageTemplateRevisionService.getNextRevisionNumber(messageTemplateId);

        // Then
        assertEquals(4, result);
        verify(messageTemplateRevisionRepository).findMaxRevisionNumberByTemplateId(messageTemplateId);
    }

    @Test
    void getNextRevisionNumber_ShouldReturnOne_WhenNoRevisionsExist() {
        // Given
        when(messageTemplateRevisionRepository.findMaxRevisionNumberByTemplateId(messageTemplateId)).thenReturn(Optional.empty());

        // When
        Integer result = messageTemplateRevisionService.getNextRevisionNumber(messageTemplateId);

        // Then
        assertEquals(1, result);
        verify(messageTemplateRevisionRepository).findMaxRevisionNumberByTemplateId(messageTemplateId);
    }

    @Test
    void createRevisionFromTemplate_ShouldCreateRevisionWithIncrementedNumber() {
        // Given
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(messageTemplateRevisionRepository.findMaxRevisionNumberByTemplateId(messageTemplateId)).thenReturn(Optional.of(2));
        when(messageTemplateRevisionRepository.save(any(MessageTemplateRevision.class))).thenReturn(messageTemplateRevision);

        // When
        MessageTemplateRevision result = messageTemplateRevisionService.createRevisionFromTemplate(messageTemplateId, "New content", userId);

        // Then
        assertNotNull(result);
        verify(messageTemplateRepository).findById(messageTemplateId);
        verify(userRepository).findById(userId);
        verify(messageTemplateRevisionRepository).findMaxRevisionNumberByTemplateId(messageTemplateId);
        verify(messageTemplateRevisionRepository).save(any(MessageTemplateRevision.class));
    }
}