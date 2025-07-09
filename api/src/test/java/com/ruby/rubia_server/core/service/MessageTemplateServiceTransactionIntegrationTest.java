package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.MessageTemplateRevision;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.CompanyPlanType;
import com.ruby.rubia_server.core.exception.MessageTemplateTransactionException;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRevisionRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests focusing on transaction behavior and revision failure scenarios
 */
@TestPropertySource(properties = {
    "app.message-template.revision.fail-on-error=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class MessageTemplateServiceTransactionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MessageTemplateService messageTemplateService;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    @Autowired
    private MessageTemplateRevisionRepository messageTemplateRevisionRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private CompanyContextUtil companyContextUtil;

    @SpyBean
    private MessageTemplateRevisionService messageTemplateRevisionService;

    private CompanyGroup companyGroup;
    private Company company;
    private User user;
    private CreateMessageTemplateDTO createDTO;

    @BeforeEach
    void setUp() {
        // Create company group
        companyGroup = new CompanyGroup();
        companyGroup.setName("Test Group");
        companyGroup.setDescription("Test Description");
        companyGroup.setIsActive(true);
        companyGroup.setCreatedAt(LocalDateTime.now());
        companyGroup.setUpdatedAt(LocalDateTime.now());
        companyGroup = companyGroupRepository.save(companyGroup);

        // Create company
        company = new Company();
        company.setName("Test Company");
        company.setSlug("test-company-" + UUID.randomUUID().toString().substring(0, 8));
        company.setDescription("Test Description");
        company.setContactEmail("test@example.com");
        company.setContactPhone("(11) 99999-9999");
        company.setIsActive(true);
        company.setPlanType(CompanyPlanType.BASIC);
        company.setMaxUsers(10);
        company.setMaxWhatsappNumbers(1);
        company.setCompanyGroup(companyGroup);
        company.setCreatedAt(LocalDateTime.now());
        company.setUpdatedAt(LocalDateTime.now());
        company = companyRepository.save(company);

        // Create user
        user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setCompany(company);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        // Mock authentication
        when(companyContextUtil.getAuthenticatedUser()).thenReturn(user);
        when(companyContextUtil.getAuthenticatedUserId()).thenReturn(user.getId());

        // Create DTO
        createDTO = CreateMessageTemplateDTO.builder()
                .companyId(company.getId())
                .name("Test Template")
                .content("Hello {customerName}")
                .isAiGenerated(false)
                .tone("FORMAL")
                .build();
    }

    @Test
    void createTemplate_ShouldRollbackTransaction_WhenRevisionFailsAndFailOnErrorIsTrue() {
        // Given - Configure revision service to fail
        doThrow(new RuntimeException("Revision creation failed"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When & Then
        assertThrows(MessageTemplateTransactionException.class, () -> {
            messageTemplateService.create(createDTO);
        });

        // Verify transaction was rolled back - no template should exist
        List<MessageTemplate> templates = messageTemplateRepository.findAll();
        assertEquals(0, templates.size(), "Template should be rolled back when revision fails");

        // Verify no revision was created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findAll();
        assertEquals(0, revisions.size(), "No revision should exist after rollback");
    }

    // Note: For fail-on-error=false behavior, we would need a separate test class
    // with @TestPropertySource(properties = {"app.message-template.revision.fail-on-error=false"})

    @Test
    void updateTemplate_ShouldRollbackTransaction_WhenRevisionFailsAndFailOnErrorIsTrue() {
        // Given - Create template first
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // Reset the spy to avoid interference from creation
        reset(messageTemplateRevisionService);
        
        // Configure revision service to fail on update
        doThrow(new RuntimeException("Revision update failed"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        UpdateMessageTemplateDTO updateDTO = UpdateMessageTemplateDTO.builder()
                .content("Updated content")
                .build();

        // When & Then
        assertThrows(MessageTemplateTransactionException.class, () -> {
            messageTemplateService.update(template.getId(), updateDTO);
        });

        // Verify template was not updated
        MessageTemplate unchangedTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(unchangedTemplate);
        assertEquals(createDTO.getContent(), unchangedTemplate.getContent(), "Template content should remain unchanged after rollback");
        assertEquals(0, unchangedTemplate.getEditCount(), "Edit count should remain unchanged after rollback");
    }


    @Test
    void softDeleteTemplate_ShouldRollbackTransaction_WhenRevisionFailsAndFailOnErrorIsTrue() {
        // Given - Create template first
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // Reset the spy to avoid interference from creation
        reset(messageTemplateRevisionService);
        
        // Configure revision service to fail on delete
        doThrow(new RuntimeException("Revision delete failed"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When & Then
        assertThrows(MessageTemplateTransactionException.class, () -> {
            messageTemplateService.softDeleteById(template.getId());
        });

        // Verify template was not deleted
        MessageTemplate notDeletedTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(notDeletedTemplate);
        assertNull(notDeletedTemplate.getDeletedAt(), "Template should not be marked as deleted after rollback");
    }


    @Test
    void createTemplate_ShouldHandleNoAuthenticatedUser_GracefullyWithoutRevision() {
        // Given - Mock authentication to fail
        when(companyContextUtil.getAuthenticatedUser()).thenThrow(new IllegalStateException("No authenticated user"));

        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then - Template should be created without revision
        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());
        assertNull(result.getCreatedBy(), "CreatedBy should be null when no authenticated user");

        // Verify no revision was created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findAll();
        assertEquals(0, revisions.size(), "No revision should be created when no authenticated user");
    }

    @Test
    void updateTemplate_ShouldHandleNoAuthenticatedUser_GracefullyWithoutRevision() {
        // Given - Create template first with auth
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // Then mock authentication to fail for update
        when(companyContextUtil.getAuthenticatedUser()).thenThrow(new IllegalStateException("No authenticated user"));

        UpdateMessageTemplateDTO updateDTO = UpdateMessageTemplateDTO.builder()
                .content("Updated content")
                .build();

        // When
        messageTemplateService.update(template.getId(), updateDTO);

        // Then - Template should be updated without revision
        MessageTemplate updatedTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(updatedTemplate);
        assertEquals("Updated content", updatedTemplate.getContent());
        assertNull(updatedTemplate.getLastEditedBy(), "LastEditedBy should be null when no authenticated user");
        
        // Edit count should not be incremented when no authenticated user
        assertEquals(0, updatedTemplate.getEditCount(), "Edit count should not be incremented when no authenticated user");
    }

    @Test
    void multipleOperationsInSequence_ShouldMaintainDataIntegrity() {
        // Given - Create template
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // When - Perform multiple operations
        UpdateMessageTemplateDTO updateDTO1 = UpdateMessageTemplateDTO.builder()
                .content("First update")
                .build();
        messageTemplateService.update(template.getId(), updateDTO1);

        UpdateMessageTemplateDTO updateDTO2 = UpdateMessageTemplateDTO.builder()
                .content("Second update")
                .build();
        messageTemplateService.update(template.getId(), updateDTO2);

        messageTemplateService.softDeleteById(template.getId());
        messageTemplateService.restoreById(template.getId());

        // Then - Verify final state
        MessageTemplate finalTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(finalTemplate);
        assertEquals("Second update", finalTemplate.getContent());
        assertEquals(2, finalTemplate.getEditCount());
        assertNull(finalTemplate.getDeletedAt(), "Template should be restored");

        // Verify all revisions were created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(template.getId());
        assertEquals(5, revisions.size(), "Should have 5 revisions: initial + 2 updates + delete + restore");
        
        // Verify all revisions have company field set
        for (MessageTemplateRevision revision : revisions) {
            assertNotNull(revision.getCompany(), "Company should be set for revision " + revision.getRevisionNumber());
            assertEquals(company.getId(), revision.getCompany().getId());
        }
    }

    @Test
    void templateWithInvalidCompanyId_ShouldFailValidation() {
        // Given - DTO with invalid company ID
        CreateMessageTemplateDTO invalidDTO = CreateMessageTemplateDTO.builder()
                .companyId(UUID.randomUUID()) // Non-existent company
                .name("Test Template")
                .content("Hello {customerName}")
                .isAiGenerated(false)
                .build();

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            messageTemplateService.create(invalidDTO);
        });

        // Verify no template was created
        List<MessageTemplate> templates = messageTemplateRepository.findAll();
        assertEquals(0, templates.size(), "No template should be created with invalid company ID");
    }
}