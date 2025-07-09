package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.MessageTemplateRevision;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.CompanyPlanType;
import com.ruby.rubia_server.core.enums.RevisionType;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRevisionRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test to verify that MessageTemplate soft delete does not cascade to MessageTemplateRevisions
 */
@ExtendWith(MockitoExtension.class)
class MessageTemplateServiceRevisionPreservationTest {

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @Mock
    private MessageTemplateRevisionRepository messageTemplateRevisionRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyContextUtil companyContextUtil;

    @Mock
    private MessageTemplateRevisionService messageTemplateRevisionService;

    @InjectMocks
    private MessageTemplateService messageTemplateService;

    private Company company;
    private User user;
    private MessageTemplate messageTemplate;
    private UUID templateId;
    private UUID userId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        userId = UUID.randomUUID();
        companyId = UUID.randomUUID();

        // Create company
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        // Create user
        user = User.builder()
                .id(userId)
                .name("Test User")
                .company(company)
                .build();

        // Create message template
        messageTemplate = MessageTemplate.builder()
                .id(templateId)
                .name("Test Template")
                .content("Hello {customerName}")
                .company(company)
                .createdBy(user)
                .isAiGenerated(false)
                .editCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        // Mock authentication - using lenient to avoid unnecessary stubbing warnings
        lenient().when(companyContextUtil.getAuthenticatedUser()).thenReturn(user);
        lenient().when(companyContextUtil.getAuthenticatedUserId()).thenReturn(userId);
    }

    @Test
    void softDeleteById_ShouldNotDeleteRevisions_OnlyMarkTemplateAsDeleted() {
        // Given
        when(messageTemplateRepository.findById(templateId)).thenReturn(Optional.of(messageTemplate));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(messageTemplate);
        
        // When
        boolean result = messageTemplateService.softDeleteById(templateId);

        // Then
        assertTrue(result);
        
        // Verify that only the template was marked as deleted (soft delete)
        verify(messageTemplateRepository).findById(templateId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
        
        // Verify that the revision service was called to create a deletion revision
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(templateId),
            eq("Hello {customerName}"),
            eq(userId),
            eq(RevisionType.DELETE)
        );
        
        // Most importantly: verify that NO repository delete operations were called
        // This ensures that revisions are NOT cascaded deleted
        verify(messageTemplateRepository, never()).delete(any(MessageTemplate.class));
        verify(messageTemplateRepository, never()).deleteById(any(UUID.class));
        verify(messageTemplateRevisionRepository, never()).delete(any(MessageTemplateRevision.class));
        verify(messageTemplateRevisionRepository, never()).deleteById(any(UUID.class));
        verify(messageTemplateRevisionRepository, never()).deleteAll();
    }

    @Test
    void hardDeleteById_ShouldWarnAndCallSuperDelete_NotRecommended() {
        // Given
        lenient().when(messageTemplateRepository.findById(templateId)).thenReturn(Optional.of(messageTemplate));
        when(messageTemplateRepository.existsById(templateId)).thenReturn(true);
        
        // When
        boolean result = messageTemplateService.deleteById(templateId);

        // Then
        assertTrue(result);
        
        // Verify that the warning is logged and super.deleteById is called
        // This should trigger the cascade behavior from the entity relationship
        // NOTE: This test documents the current behavior but soft delete is recommended
        verify(messageTemplateRepository).existsById(templateId);
        verify(messageTemplateRepository).deleteById(templateId);
    }

    @Test
    void restoreById_ShouldRestoreTemplateWithoutAffectingExistingRevisions() {
        // Given - Template is already soft deleted
        MessageTemplate deletedTemplate = MessageTemplate.builder()
                .id(templateId)
                .name("Test Template")
                .content("Hello {customerName}")
                .company(company)
                .createdBy(user)
                .isAiGenerated(false)
                .editCount(0)
                .deletedAt(LocalDateTime.now()) // Already deleted
                .createdAt(LocalDateTime.now())
                .build();
        
        when(messageTemplateRepository.findById(templateId)).thenReturn(Optional.of(deletedTemplate));
        when(messageTemplateRepository.save(any(MessageTemplate.class))).thenReturn(deletedTemplate);
        
        // When
        boolean result = messageTemplateService.restoreById(templateId);

        // Then
        assertTrue(result);
        
        // Verify that only the template was restored
        verify(messageTemplateRepository).findById(templateId);
        verify(messageTemplateRepository).save(any(MessageTemplate.class));
        
        // Verify that the revision service was called to create a restoration revision
        verify(messageTemplateRevisionService).createRevisionFromTemplate(
            eq(templateId),
            eq("Hello {customerName}"),
            eq(userId),
            eq(RevisionType.RESTORE)
        );
        
        // Verify that NO repository delete operations were called
        verify(messageTemplateRevisionRepository, never()).delete(any(MessageTemplateRevision.class));
        verify(messageTemplateRevisionRepository, never()).deleteById(any(UUID.class));
    }
}