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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for fail-on-error=false behavior where operations continue despite revision failures
 */
@TestPropertySource(properties = {
    "app.message-template.revision.fail-on-error=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class MessageTemplateServiceContinueOnErrorIntegrationTest extends AbstractIntegrationTest {

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private CompanyGroup companyGroup;
    private Company company;
    private User user;
    private CreateMessageTemplateDTO createDTO;

    @BeforeEach
    void setUp() {
        // Clean existing data
        messageTemplateRevisionRepository.deleteAll();
        messageTemplateRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();
        companyGroupRepository.deleteAll();

        // Create company group
        companyGroup = new CompanyGroup();
        companyGroup.setName("Test Group");
        companyGroup.setDescription("Test Description");
        companyGroup.setIsActive(true);
        companyGroup.setCreatedAt(LocalDateTime.now());
        companyGroup.setUpdatedAt(LocalDateTime.now());
        companyGroup = companyGroupRepository.save(companyGroup);
        
        // Flush to ensure company group is persisted
        entityManager.flush();

        // Create company using SQL to avoid enum issues in TestContainers
        UUID companyId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO companies (id, name, slug, description, contact_email, contact_phone, is_active, plan_type, max_users, max_whatsapp_numbers, company_group_id, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?::companyplantype, ?, ?, ?, ?, ?)",
            companyId,
            "Test Company",
            "test-company-" + UUID.randomUUID().toString().substring(0, 8),
            "Test Description",
            "test@example.com",
            "(11) 99999-9999",
            true,
            "BASIC",
            10,
            1,
            companyGroup.getId(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        company = companyRepository.findById(companyId).orElseThrow();

        // Create user using SQL to avoid enum issues in TestContainers
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
            "INSERT INTO users (id, name, email, password_hash, company_id, role, is_online, is_whatsapp_active, created_at, updated_at) " +
            "VALUES (?, ?, ?, ?, ?, ?::userrole, ?, ?, ?, ?)",
            userId,
            "Test User",
            "test@example.com",
            "test-hash",
            company.getId(),
            "AGENT",
            false,
            false,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        user = userRepository.findById(userId).orElseThrow();

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
    void createTemplate_ShouldContinue_WhenRevisionFailsAndFailOnErrorIsFalse() {
        // Given - Configure revision service to fail
        doThrow(new RuntimeException("Revision creation failed"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When - Create template (should not throw exception)
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then - Template should be created successfully despite revision failure
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getContent(), result.getContent());

        // Verify template was persisted
        List<MessageTemplate> templates = messageTemplateRepository.findAll();
        assertEquals(1, templates.size(), "Template should be created even when revision fails");

        // Verify no revision was created due to the failure
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findAll();
        assertEquals(0, revisions.size(), "No revision should exist due to creation failure");
    }

    @Test
    void updateTemplate_ShouldContinue_WhenRevisionFailsAndFailOnErrorIsFalse() {
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

        // When - Update template (should not throw exception)
        messageTemplateService.update(template.getId(), updateDTO);

        // Then - Template should be updated successfully despite revision failure
        MessageTemplate updatedTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(updatedTemplate);
        assertEquals("Updated content", updatedTemplate.getContent());
        assertEquals(1, updatedTemplate.getEditCount(), "Edit count should be incremented even when revision fails");
        assertEquals(user.getId(), updatedTemplate.getLastEditedBy().getId(), "LastEditedBy should be set even when revision fails");
    }

    @Test
    void softDeleteTemplate_ShouldContinue_WhenRevisionFailsAndFailOnErrorIsFalse() {
        // Given - Create template first
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // Reset the spy to avoid interference from creation
        reset(messageTemplateRevisionService);
        
        // Configure revision service to fail on delete
        doThrow(new RuntimeException("Revision delete failed"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When - Soft delete template (should not throw exception)
        boolean result = messageTemplateService.softDeleteById(template.getId());

        // Then - Template should be deleted successfully despite revision failure
        assertTrue(result);
        
        MessageTemplate deletedTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(deletedTemplate);
        assertNotNull(deletedTemplate.getDeletedAt(), "Template should be marked as deleted even when revision fails");
        assertEquals(user.getId(), deletedTemplate.getLastEditedBy().getId(), "LastEditedBy should be set even when revision fails");
    }

    @Test
    void restoreTemplate_ShouldContinue_WhenRevisionFailsAndFailOnErrorIsFalse() {
        // Given - Create and soft delete template first
        MessageTemplate template = messageTemplateService.create(createDTO);
        messageTemplateService.softDeleteById(template.getId());
        
        // Reset the spy to avoid interference from previous operations
        reset(messageTemplateRevisionService);
        
        // Configure revision service to fail on restore
        doThrow(new RuntimeException("Revision restore failed"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When - Restore template (should not throw exception)
        boolean result = messageTemplateService.restoreById(template.getId());

        // Then - Template should be restored successfully despite revision failure
        assertTrue(result);
        
        MessageTemplate restoredTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(restoredTemplate);
        assertNull(restoredTemplate.getDeletedAt(), "Template should be restored even when revision fails");
        assertEquals(user.getId(), restoredTemplate.getLastEditedBy().getId(), "LastEditedBy should be set even when revision fails");
    }

    @Test
    void multipleOperationsWithRevisionFailures_ShouldAllContinue_WhenFailOnErrorIsFalse() {
        // Given - Configure revision service to always fail
        doThrow(new RuntimeException("Revision service consistently fails"))
                .when(messageTemplateRevisionService)
                .createRevisionFromTemplate(any(), any(), any());

        // When - Perform multiple operations (none should throw exceptions)
        MessageTemplate template = messageTemplateService.create(createDTO);
        
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

        // Then - All operations should have succeeded despite revision failures
        MessageTemplate finalTemplate = messageTemplateRepository.findById(template.getId()).orElse(null);
        assertNotNull(finalTemplate);
        assertEquals("Second update", finalTemplate.getContent());
        assertEquals(2, finalTemplate.getEditCount(), "Edit count should reflect all updates");
        assertNull(finalTemplate.getDeletedAt(), "Template should be restored");

        // Verify no revisions were created due to consistent failures
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(template.getId());
        assertEquals(0, revisions.size(), "No revisions should exist due to consistent failures");
    }

    @Test
    void createTemplate_ShouldContinueAndCreateRevision_WhenRevisionSucceedsAndFailOnErrorIsFalse() {
        // Given - Normal operation (no mocking of revision service)
        // This verifies that when fail-on-error=false, successful revisions still work

        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then - Both template and revision should be created successfully
        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());

        // Verify template was persisted
        List<MessageTemplate> templates = messageTemplateRepository.findAll();
        assertEquals(1, templates.size());

        // Verify revision was created successfully
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(result.getId());
        assertEquals(1, revisions.size(), "Revision should be created when revision service succeeds");
        
        MessageTemplateRevision revision = revisions.get(0);
        assertEquals(result.getContent(), revision.getContent());
        assertEquals(company.getId(), revision.getCompany().getId());
        assertEquals(user.getId(), revision.getEditedBy().getId());
    }
}