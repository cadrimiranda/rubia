package com.ruby.rubia_server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.*;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import com.ruby.rubia_server.dto.campaign.ProcessCampaignDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@AutoConfigureMockMvc
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CampaignControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompanyContextUtil companyContextUtil;


    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CampaignContactRepository campaignContactRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private CompanyGroup companyGroup;
    private Company company;
    private User user;
    private MessageTemplate messageTemplate;

    @BeforeEach
    void setUp() {
        // Setup company group
        companyGroup = CompanyGroup.builder()
            .name("Test Group")
            .build();
        companyGroup = companyGroupRepository.save(companyGroup);

        // Setup company
        company = Company.builder()
            .name("Test Company")
            .slug("test-company")
            .contactEmail("test@company.com")
            .contactPhone("1199999999")
            .companyGroup(companyGroup)
            .build();
        company = companyRepository.save(company);

        // Setup department first
        Department department = Department.builder()
            .name("Test Department")
            .company(company)
            .build();
        department = departmentRepository.save(department);

        // Setup user
        user = User.builder()
            .name("Test User")
            .email("test@user.com")
            .passwordHash("password")
            .role(UserRole.ADMIN)
            .company(company)
            .department(department)
            .build();
        user = userRepository.save(user);

        // Setup message template
        messageTemplate = MessageTemplate.builder()
            .name("Test Template")
            .content("Test message content")
            .company(company)
            .createdBy(user)
            .build();
        messageTemplate = messageTemplateRepository.save(messageTemplate);

        // Mock company context
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(company.getId());
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldProcessValidExcelFileAndCreateTwoContacts() throws Exception {
        // Arrange
        MockMultipartFile excelFile = new MockMultipartFile(
            "file",
            "planilha_correta.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ClassPathResource("static/planilha_correta.xlsx").getInputStream()
        );

        ProcessCampaignDTO processCampaignDTO = ProcessCampaignDTO.builder()
            .companyId(company.getId())
            .userId(user.getId())
            .name("Test Campaign")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .sourceSystem("TEST")
            .templateIds(List.of(messageTemplate.getId()))
            .build();

        MockMultipartFile dataFile = new MockMultipartFile(
            "data",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(processCampaignDTO)
        );

        // Act & Assert
        String response = mockMvc.perform(multipart("/api/campaigns/process")
                .file(excelFile)
                .file(dataFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andDo(result -> System.out.println("Response: " + result.getResponse().getContentAsString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.campaign.name").value("Test Campaign"))
            .andExpect(jsonPath("$.statistics.created").value(2))
            .andExpect(jsonPath("$.statistics.processed").value(2))
            .andExpect(jsonPath("$.statistics.duplicates").value(0))
            .andExpect(jsonPath("$.statistics.errors").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Verify customers were created
        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(2);

        Customer maura = customers.stream()
            .filter(c -> "MAURA RODRIGUES".equals(c.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(maura.getPhone()).isEqualTo("+5511999999999");
        assertThat(maura.getBirthDate().toString()).startsWith("2003-09-18");

        Customer tarze = customers.stream()
            .filter(c -> "TARZE CARVALHO".equals(c.getName()))
            .findFirst()
            .orElseThrow();
        assertThat(tarze.getPhone()).isEqualTo("+5511999999992");
        assertThat(tarze.getBirthDate().toString()).startsWith("2005-08-16");

        // Verify campaign was created
        List<Campaign> campaigns = campaignRepository.findAll();
        assertThat(campaigns).hasSize(1);
        assertThat(campaigns.get(0).getTotalContacts()).isEqualTo(2);

        // Verify campaign contacts were created
        List<CampaignContact> campaignContacts = campaignContactRepository.findAll();
        assertThat(campaignContacts).hasSize(2);

        // Verify conversations were created
        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(2);
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldRejectExcelFileWithDuplicatePhones() throws Exception {
        // Arrange
        MockMultipartFile excelFile = new MockMultipartFile(
            "file",
            "planilha_incorreta.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ClassPathResource("static/planilha_incorreta.xlsx").getInputStream()
        );

        ProcessCampaignDTO processCampaignDTO = ProcessCampaignDTO.builder()
            .companyId(company.getId())
            .userId(user.getId())
            .name("Test Campaign Incorrect")
            .description("Test Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .sourceSystem("TEST")
            .templateIds(List.of(messageTemplate.getId()))
            .build();

        MockMultipartFile dataFile = new MockMultipartFile(
            "data",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(processCampaignDTO)
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/campaigns/process")
                .file(excelFile)
                .file(dataFile)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.statistics.duplicates").value(greaterThan(0)));

        // Verify only unique contacts were created
        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSizeLessThanOrEqualTo(1);
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldCreateTwoCampaignsWithSameContactsWithoutError() throws Exception {
        // Arrange - First campaign
        MockMultipartFile excelFile1 = new MockMultipartFile(
            "file",
            "planilha_correta.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ClassPathResource("static/planilha_correta.xlsx").getInputStream()
        );

        ProcessCampaignDTO processCampaignDTO1 = ProcessCampaignDTO.builder()
            .companyId(company.getId())
            .userId(user.getId())
            .name("First Campaign")
            .description("First Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .sourceSystem("TEST")
            .templateIds(List.of(messageTemplate.getId()))
            .build();

        MockMultipartFile dataFile1 = new MockMultipartFile(
            "data",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(processCampaignDTO1)
        );

        // Act - First campaign
        mockMvc.perform(multipart("/api/campaigns/process")
                .file(excelFile1)
                .file(dataFile1)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Arrange - Second campaign
        MockMultipartFile excelFile2 = new MockMultipartFile(
            "file",
            "planilha_correta.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ClassPathResource("static/planilha_correta.xlsx").getInputStream()
        );

        ProcessCampaignDTO processCampaignDTO2 = ProcessCampaignDTO.builder()
            .companyId(company.getId())
            .userId(user.getId())
            .name("Second Campaign")
            .description("Second Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .sourceSystem("TEST")
            .templateIds(List.of(messageTemplate.getId()))
            .build();

        MockMultipartFile dataFile2 = new MockMultipartFile(
            "data",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(processCampaignDTO2)
        );

        // Act - Second campaign
        mockMvc.perform(multipart("/api/campaigns/process")
                .file(excelFile2)
                .file(dataFile2)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Assert - Verify both campaigns were created
        List<Campaign> campaigns = campaignRepository.findAll();
        assertThat(campaigns).hasSize(2);
        assertThat(campaigns.stream().map(Campaign::getName))
            .containsExactlyInAnyOrder("First Campaign", "Second Campaign");

        // Verify customers exist (should be same 2 customers)
        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(2);

        // Verify campaign contacts exist for both campaigns
        List<CampaignContact> campaignContacts = campaignContactRepository.findAll();
        assertThat(campaignContacts).hasSize(4); // 2 contacts × 2 campaigns

        // Verify conversations exist for both campaigns
        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(4); // 2 contacts × 2 campaigns
    }

    @Test
    @WithMockUser
    @Transactional
    void shouldUpdateContactAndCreateTwoCampaigns() throws Exception {
        // Arrange - First campaign with original data
        MockMultipartFile excelFile1 = new MockMultipartFile(
            "file",
            "planilha_correta.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ClassPathResource("static/planilha_correta.xlsx").getInputStream()
        );

        ProcessCampaignDTO processCampaignDTO1 = ProcessCampaignDTO.builder()
            .companyId(company.getId())
            .userId(user.getId())
            .name("Original Campaign")
            .description("Original Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .sourceSystem("TEST")
            .templateIds(List.of(messageTemplate.getId()))
            .build();

        MockMultipartFile dataFile1 = new MockMultipartFile(
            "data",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(processCampaignDTO1)
        );

        // Act - First campaign
        mockMvc.perform(multipart("/api/campaigns/process")
                .file(excelFile1)
                .file(dataFile1)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Arrange - Second campaign with updated data
        MockMultipartFile excelFile2 = new MockMultipartFile(
            "file",
            "planilha_correta_update.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new ClassPathResource("static/planilha_correta_update.xlsx").getInputStream()
        );

        ProcessCampaignDTO processCampaignDTO2 = ProcessCampaignDTO.builder()
            .companyId(company.getId())
            .userId(user.getId())
            .name("Updated Campaign")
            .description("Updated Description")
            .startDate(LocalDate.now())
            .endDate(LocalDate.now().plusDays(30))
            .sourceSystem("TEST")
            .templateIds(List.of(messageTemplate.getId()))
            .build();

        MockMultipartFile dataFile2 = new MockMultipartFile(
            "data",
            "",
            "application/json",
            objectMapper.writeValueAsBytes(processCampaignDTO2)
        );

        // Act - Second campaign with updates
        mockMvc.perform(multipart("/api/campaigns/process")
                .file(excelFile2)
                .file(dataFile2)
                .contentType(MediaType.MULTIPART_FORM_DATA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        // Assert - Verify MAURA RODRIGUES was updated
        List<Customer> customers = customerRepository.findAll();
        Customer maura = customers.stream()
            .filter(c -> "MAURA RODRIGUES".equals(c.getName()))
            .findFirst()
            .orElseThrow();
        
        assertThat(maura.getPhone()).isEqualTo("+5511999999999");
        assertThat(maura.getBirthDate().toString()).startsWith("2003-09-18");

        // Verify both campaigns were created
        List<Campaign> campaigns = campaignRepository.findAll();
        assertThat(campaigns).hasSize(2);
        assertThat(campaigns.stream().map(Campaign::getName))
            .containsExactlyInAnyOrder("Original Campaign", "Updated Campaign");

        // Verify campaign contacts and conversations exist for both campaigns
        List<CampaignContact> campaignContacts = campaignContactRepository.findAll();
        assertThat(campaignContacts).hasSize(4); // 2 contacts × 2 campaigns

        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(4); // 2 contacts × 2 campaigns
    }
}