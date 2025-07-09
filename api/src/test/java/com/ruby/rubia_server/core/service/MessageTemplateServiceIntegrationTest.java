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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = {
    "app.message-template.revision.fail-on-error=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class MessageTemplateServiceIntegrationTest extends AbstractIntegrationTest {

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

    private CompanyGroup companyGroup;
    private Company company;
    private User user;
    private CreateMessageTemplateDTO createDTO;

    @BeforeEach
    void setUp() {
        // Create and save a company group
        companyGroup = CompanyGroup.builder()
                .name("Test Group")
                .description("Test Description")
                .isActive(true)
                .build();
        companyGroup = companyGroupRepository.save(companyGroup);

        // Create and save a company
        company = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .description("Test Description")
                .contactEmail("test@example.com")
                .contactPhone("(11) 99999-9999")
                .isActive(true)
                .planType(CompanyPlanType.BASIC)
                .maxUsers(10)
                .maxWhatsappNumbers(1)
                .companyGroup(companyGroup)
                .build();
        company = companyRepository.save(company);

        // Create and save a user
        user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .company(company)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);

        // Mock the CompanyContextUtil
        when(companyContextUtil.getAuthenticatedUser()).thenReturn(user);
        when(companyContextUtil.getAuthenticatedUserId()).thenReturn(user.getId());

        // Create DTO for testing
        createDTO = CreateMessageTemplateDTO.builder()
                .companyId(company.getId())
                .name("Test Template")
                .content("Hello {customerName}, how are you?")
                .isAiGenerated(false)
                .tone("FORMAL")
                .build();
    }

    @Test
    void createMessageTemplate_ShouldCreateTemplateWithRevision_WhenValidData() {
        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getContent(), result.getContent());
        assertEquals(createDTO.getIsAiGenerated(), result.getIsAiGenerated());
        assertEquals(createDTO.getTone(), result.getTone());
        assertEquals(company.getId(), result.getCompany().getId());
        assertEquals(user.getId(), result.getCreatedBy().getId());
        assertEquals(0, result.getEditCount());

        // Verify the template was persisted
        MessageTemplate persistedTemplate = messageTemplateRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedTemplate);
        assertEquals(result.getName(), persistedTemplate.getName());

        // Verify initial revision was created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(result.getId());
        assertEquals(1, revisions.size());
        
        MessageTemplateRevision initialRevision = revisions.get(0);
        assertNotNull(initialRevision.getCompany());
        assertEquals(company.getId(), initialRevision.getCompany().getId());
        assertEquals(result.getId(), initialRevision.getTemplate().getId());
        assertEquals(1, initialRevision.getRevisionNumber());
        assertEquals(createDTO.getContent(), initialRevision.getContent());
        assertEquals(user.getId(), initialRevision.getEditedBy().getId());
    }

    @Test
    void updateMessageTemplate_ShouldCreateRevisionWithCompany_WhenContentChanges() {
        // Given
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        UpdateMessageTemplateDTO updateDTO = UpdateMessageTemplateDTO.builder()
                .name("Updated Template")
                .content("Updated content with {customerName}")
                .tone("INFORMAL")
                .build();

        // When
        messageTemplateService.update(template.getId(), updateDTO);

        // Then
        // Verify revisions were created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateIdOrderByRevisionNumberDesc(template.getId());
        assertEquals(2, revisions.size());
        
        // Check the new revision (should be first due to DESC order)
        MessageTemplateRevision newRevision = revisions.get(0);
        assertNotNull(newRevision.getCompany());
        assertEquals(company.getId(), newRevision.getCompany().getId());
        assertEquals(template.getId(), newRevision.getTemplate().getId());
        assertEquals(2, newRevision.getRevisionNumber());
        assertEquals(updateDTO.getContent(), newRevision.getContent());
        assertEquals(user.getId(), newRevision.getEditedBy().getId());
        
        // Check the initial revision
        MessageTemplateRevision initialRevision = revisions.get(1);
        assertNotNull(initialRevision.getCompany());
        assertEquals(company.getId(), initialRevision.getCompany().getId());
        assertEquals(1, initialRevision.getRevisionNumber());
        assertEquals(createDTO.getContent(), initialRevision.getContent());
    }

    @Test
    void updateMessageTemplate_ShouldNotCreateRevision_WhenContentDoesNotChange() {
        // Given
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        UpdateMessageTemplateDTO updateDTO = UpdateMessageTemplateDTO.builder()
                .name("Updated Template Name Only")
                .content(createDTO.getContent()) // Same content
                .tone("INFORMAL")
                .build();

        // When
        messageTemplateService.update(template.getId(), updateDTO);

        // Then
        // Verify only initial revision exists
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(template.getId());
        assertEquals(1, revisions.size());
        
        MessageTemplateRevision revision = revisions.get(0);
        assertEquals(1, revision.getRevisionNumber());
        assertEquals(createDTO.getContent(), revision.getContent());
    }

    @Test
    void softDeleteById_ShouldCreateRevisionWithCompany_WhenTemplateDeleted() {
        // Given
        MessageTemplate template = messageTemplateService.create(createDTO);

        // When
        boolean result = messageTemplateService.softDeleteById(template.getId());

        // Then
        assertTrue(result);
        
        // Verify deletion revision was created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateIdOrderByRevisionNumberDesc(template.getId());
        assertEquals(2, revisions.size());
        
        // Check the deletion revision (should be first due to DESC order)
        MessageTemplateRevision deletionRevision = revisions.get(0);
        assertNotNull(deletionRevision.getCompany());
        assertEquals(company.getId(), deletionRevision.getCompany().getId());
        assertEquals(template.getId(), deletionRevision.getTemplate().getId());
        assertEquals(2, deletionRevision.getRevisionNumber());
        assertTrue(deletionRevision.getContent().startsWith("[TEMPLATE DELETED]"));
        assertEquals(user.getId(), deletionRevision.getEditedBy().getId());
    }

    @Test
    void restoreById_ShouldCreateRevisionWithCompany_WhenTemplateRestored() {
        // Given
        MessageTemplate template = messageTemplateService.create(createDTO);
        messageTemplateService.softDeleteById(template.getId());

        // When
        boolean result = messageTemplateService.restoreById(template.getId());

        // Then
        assertTrue(result);
        
        // Verify restoration revision was created
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateIdOrderByRevisionNumberDesc(template.getId());
        assertEquals(3, revisions.size());
        
        // Check the restoration revision (should be first due to DESC order)
        MessageTemplateRevision restorationRevision = revisions.get(0);
        assertNotNull(restorationRevision.getCompany());
        assertEquals(company.getId(), restorationRevision.getCompany().getId());
        assertEquals(template.getId(), restorationRevision.getTemplate().getId());
        assertEquals(3, restorationRevision.getRevisionNumber());
        assertTrue(restorationRevision.getContent().startsWith("[TEMPLATE RESTORED]"));
        assertEquals(user.getId(), restorationRevision.getEditedBy().getId());
    }

    @Test
    void createMessageTemplate_ShouldFailWithConstraintViolation_WhenRevisionHasNoCompany() {
        // This test would fail if the company field wasn't being set properly
        // The database constraint would be violated
        
        // Given - using a DTO that would create a template
        CreateMessageTemplateDTO testDTO = CreateMessageTemplateDTO.builder()
                .companyId(company.getId())
                .name("Test Template")
                .content("Test content")
                .isAiGenerated(false)
                .build();

        // When & Then - This should work without database constraint violations
        assertDoesNotThrow(() -> {
            MessageTemplate result = messageTemplateService.create(testDTO);
            assertNotNull(result);
            
            // Verify the revision was created with company
            List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(result.getId());
            assertEquals(1, revisions.size());
            assertNotNull(revisions.get(0).getCompany());
        });
    }

    @Test
    void allRevisions_ShouldHaveCompanyFieldSet_WhenCreatedThroughService() {
        // Given
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // Perform various operations that create revisions
        UpdateMessageTemplateDTO updateDTO = UpdateMessageTemplateDTO.builder()
                .content("Updated content")
                .build();
        messageTemplateService.update(template.getId(), updateDTO);
        
        messageTemplateService.softDeleteById(template.getId());
        messageTemplateService.restoreById(template.getId());

        // When
        List<MessageTemplateRevision> allRevisions = messageTemplateRevisionRepository.findByTemplateId(template.getId());

        // Then
        assertEquals(4, allRevisions.size()); // Initial + Update + Delete + Restore
        
        // Every revision should have company field set
        for (MessageTemplateRevision revision : allRevisions) {
            assertNotNull(revision.getCompany(), "Company should not be null for revision " + revision.getRevisionNumber());
            assertEquals(company.getId(), revision.getCompany().getId(), "Company ID should match for revision " + revision.getRevisionNumber());
        }
    }
}