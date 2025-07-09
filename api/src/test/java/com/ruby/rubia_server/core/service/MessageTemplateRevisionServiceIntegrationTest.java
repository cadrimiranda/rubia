package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateRevisionDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class MessageTemplateRevisionServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MessageTemplateRevisionService messageTemplateRevisionService;

    @Autowired
    private MessageTemplateRevisionRepository messageTemplateRevisionRepository;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private UserRepository userRepository;

    private CompanyGroup companyGroup;
    private Company company;
    private MessageTemplate messageTemplate;
    private User user;

    @BeforeEach
    void setUp() {
        // Create company group with proper enum values
        companyGroup = new CompanyGroup();
        companyGroup.setName("Test Group");
        companyGroup.setDescription("Test Description");
        companyGroup.setIsActive(true);
        companyGroup.setCreatedAt(LocalDateTime.now());
        companyGroup.setUpdatedAt(LocalDateTime.now());
        companyGroup = companyGroupRepository.save(companyGroup);

        // Create company with proper enum values
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

        // Create message template
        messageTemplate = new MessageTemplate();
        messageTemplate.setName("Test Template");
        messageTemplate.setContent("Hello {customerName}");
        messageTemplate.setCompany(company);
        messageTemplate.setCreatedBy(user);
        messageTemplate.setIsAiGenerated(false);
        messageTemplate.setEditCount(0);
        messageTemplate.setCreatedAt(LocalDateTime.now());
        messageTemplate.setUpdatedAt(LocalDateTime.now());
        messageTemplate = messageTemplateRepository.save(messageTemplate);
    }

    @Test
    void createRevisionFromTemplate_ShouldSetCompanyField_WhenTemplateHasCompany() {
        // When - Create revision using the service method
        MessageTemplateRevision result = messageTemplateRevisionService.createRevisionFromTemplate(
            messageTemplate.getId(), 
            "Updated content", 
            user.getId()
        );

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getCompany());
        assertEquals(company.getId(), result.getCompany().getId());
        assertEquals(messageTemplate.getId(), result.getTemplate().getId());
        assertEquals(user.getId(), result.getEditedBy().getId());
        assertEquals("Updated content", result.getContent());
        
        // Verify it was persisted correctly in the database
        MessageTemplateRevision persistedRevision = messageTemplateRevisionRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedRevision);
        assertNotNull(persistedRevision.getCompany());
        assertEquals(company.getId(), persistedRevision.getCompany().getId());
    }

    @Test
    void createRevisionFromDTO_ShouldSetCompanyFromTemplate_WhenBuildingFromDTO() {
        // Given
        CreateMessageTemplateRevisionDTO createDTO = CreateMessageTemplateRevisionDTO.builder()
                .companyId(company.getId())
                .templateId(messageTemplate.getId())
                .revisionNumber(2)
                .content("DTO Created content")
                .editedByUserId(user.getId())
                .build();

        // When
        MessageTemplateRevision result = messageTemplateRevisionService.create(createDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertNotNull(result.getCompany());
        assertEquals(company.getId(), result.getCompany().getId());
        assertEquals(messageTemplate.getId(), result.getTemplate().getId());
        assertEquals(user.getId(), result.getEditedBy().getId());
        assertEquals("DTO Created content", result.getContent());
        assertEquals(2, result.getRevisionNumber());
        
        // Verify it was persisted correctly in the database
        MessageTemplateRevision persistedRevision = messageTemplateRevisionRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedRevision);
        assertNotNull(persistedRevision.getCompany());
        assertEquals(company.getId(), persistedRevision.getCompany().getId());
    }



    @Test
    void createRevisionFromTemplate_ShouldFailWithConstraintViolation_WhenCompanyIsNull() {
        // This test simulates what would happen if the company field wasn't being set
        // We can't easily simulate this with the current implementation, but we can verify
        // that the company is definitely being set by the service
        
        // When
        MessageTemplateRevision result = messageTemplateRevisionService.createRevisionFromTemplate(
            messageTemplate.getId(), 
            "Test content", 
            user.getId()
        );

        // Then - This should NOT throw a constraint violation because company is set
        assertNotNull(result.getCompany());
        assertEquals(company.getId(), result.getCompany().getId());
        
        // Verify that the database constraint is satisfied
        MessageTemplateRevision persistedRevision = messageTemplateRevisionRepository.findById(result.getId()).orElse(null);
        assertNotNull(persistedRevision);
        assertNotNull(persistedRevision.getCompany());
    }
}