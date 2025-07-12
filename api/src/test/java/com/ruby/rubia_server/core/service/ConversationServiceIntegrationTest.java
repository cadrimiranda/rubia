package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.ConversationType;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ConversationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    private Company testCompany;
    private Customer testCustomer;
    private User testUser;
    private Department testDepartment;
    private CompanyGroup testCompanyGroup;

    @BeforeEach
    void setUp() {
        testCompanyGroup = createTestCompanyGroup();
        testCompany = createTestCompany();
        testDepartment = createTestDepartment();
        testCustomer = createTestCustomer();
        testUser = createTestUser();
    }

    @Test
    void shouldCreateConversationSuccessfully() {
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        ConversationDTO created = conversationService.create(createDTO, testCompany.getId());

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(ConversationStatus.ENTRADA);
        assertThat(created.getChannel()).isEqualTo(Channel.WHATSAPP);
    }

    @Test
    void shouldFindConversationById() {
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        ConversationDTO created = conversationService.create(createDTO, testCompany.getId());

        ConversationDTO found = conversationService.findById(created.getId(), testCompany.getId());

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getStatus()).isEqualTo(ConversationStatus.ENTRADA);
    }

    @Test
    void shouldChangeConversationStatus() {
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        ConversationDTO created = conversationService.create(createDTO, testCompany.getId());

        ConversationDTO updated = conversationService.changeStatus(created.getId(), ConversationStatus.ESPERANDO, testCompany.getId());

        assertThat(updated.getStatus()).isEqualTo(ConversationStatus.ESPERANDO);
    }

    @Test
    void shouldAssignConversationToUser() {
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        ConversationDTO created = conversationService.create(createDTO, testCompany.getId());

        ConversationDTO assigned = conversationService.assignToUser(created.getId(), testUser.getId(), testCompany.getId());

        assertThat(assigned.getAssignedUserId()).isEqualTo(testUser.getId());
        assertThat(assigned.getStatus()).isEqualTo(ConversationStatus.ESPERANDO);
    }

    @Test
    void shouldFindConversationsByStatusAndCompany() {
        CreateConversationDTO createDTO1 = new CreateConversationDTO();
        createDTO1.setCustomerId(testCustomer.getId());
        createDTO1.setChannel(Channel.WHATSAPP);
        createDTO1.setPriority(1);

        CreateConversationDTO createDTO2 = new CreateConversationDTO();
        createDTO2.setCustomerId(testCustomer.getId());
        createDTO2.setChannel(Channel.WHATSAPP);
        createDTO2.setPriority(1);

        conversationService.create(createDTO1, testCompany.getId());
        ConversationDTO conv2 = conversationService.create(createDTO2, testCompany.getId());
        conversationService.changeStatus(conv2.getId(), ConversationStatus.ESPERANDO, testCompany.getId());

        List<ConversationDTO> entradaConversations = conversationService.findByStatusAndCompany(
                ConversationStatus.ENTRADA, testCompany.getId());

        assertThat(entradaConversations).hasSize(1);
        assertThat(entradaConversations.get(0).getStatus()).isEqualTo(ConversationStatus.ENTRADA);
    }

    @Test
    void shouldCountConversationsByStatusAndCompany() {
        CreateConversationDTO createDTO1 = new CreateConversationDTO();
        createDTO1.setCustomerId(testCustomer.getId());
        createDTO1.setChannel(Channel.WHATSAPP);
        createDTO1.setPriority(1);

        CreateConversationDTO createDTO2 = new CreateConversationDTO();
        createDTO2.setCustomerId(testCustomer.getId());
        createDTO2.setChannel(Channel.WHATSAPP);
        createDTO2.setPriority(1);

        conversationService.create(createDTO1, testCompany.getId());
        conversationService.create(createDTO2, testCompany.getId());

        long count = conversationService.countByStatusAndCompany(ConversationStatus.ENTRADA, testCompany.getId());

        assertThat(count).isEqualTo(2);
    }

    private CompanyGroup createTestCompanyGroup() {
        CompanyGroup group = new CompanyGroup();
        group.setName("Test Group");
        group.setDescription("Test Company Group");
        return companyGroupRepository.save(group);
    }

    private Company createTestCompany() {
        Company company = new Company();
        company.setName("Test Company");
        company.setSlug("test-company");
        company.setContactEmail("test@company.com");
        company.setContactPhone("123456789");
        company.setCompanyGroup(testCompanyGroup);
        return companyRepository.save(company);
    }

    private Department createTestDepartment() {
        Department department = new Department();
        department.setName("Test Department");
        department.setCompany(testCompany);
        return departmentRepository.save(department);
    }

    private Customer createTestCustomer() {
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setPhone("123456789");
        customer.setCompany(testCompany);
        return customerRepository.save(customer);
    }

    private User createTestUser() {
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@user.com");
        user.setPasswordHash("hashedpassword");
        user.setRole(UserRole.AGENT);
        user.setCompany(testCompany);
        user.setDepartment(testDepartment);
        return userRepository.save(user);
    }
}