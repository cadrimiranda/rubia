package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.enums.CompanyPlanType;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Simplified integration tests focusing on basic transaction behavior
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

    @Autowired
    private DepartmentRepository departmentRepository;

    @MockBean
    private CompanyContextUtil companyContextUtil;

    private CompanyGroup companyGroup;
    private Company company;
    private Department department;
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

        // Create department
        department = new Department();
        department.setName("Test Department");
        department.setDescription("Test Department Description");
        department.setAutoAssign(true);
        department.setCompany(company);
        department.setCreatedAt(LocalDateTime.now());
        department.setUpdatedAt(LocalDateTime.now());
        department = departmentRepository.save(department);

        // Create user
        user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$abc123");
        user.setRole(UserRole.ADMIN);
        user.setCompany(company);
        user.setDepartment(department);
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
    void createTemplate_ShouldSucceed_WhenValidData() {
        // When
        MessageTemplate template = messageTemplateService.create(createDTO);
        
        // Then
        assertNotNull(template);
        assertNotNull(template.getId());
        assertEquals(createDTO.getName(), template.getName());
        assertEquals(createDTO.getContent(), template.getContent());
    }

    @Test
    void createTemplate_ShouldHandleNoAuthenticatedUser_Gracefully() {
        // Given - Mock authentication to fail
        when(companyContextUtil.getAuthenticatedUser()).thenThrow(new IllegalStateException("No authenticated user"));

        // When
        MessageTemplate result = messageTemplateService.create(createDTO);

        // Then - Template should be created without revision
        assertNotNull(result);
        assertEquals(createDTO.getName(), result.getName());
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
    }
}