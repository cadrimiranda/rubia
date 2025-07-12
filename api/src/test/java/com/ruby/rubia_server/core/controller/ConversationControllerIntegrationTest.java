package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@AutoConfigureWebMvc
@Transactional
class ConversationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyContextUtil companyContextUtil;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ConversationRepository conversationRepository;

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

        // Mock CompanyContextUtil para retornar nossa empresa de teste
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(testCompany.getId());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldCreateConversationSuccessfully() throws Exception {
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ENTRADA"))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.customerId").value(testCustomer.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(testCompany.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldReturnBadRequestForInvalidConversation() throws Exception {
        CreateConversationDTO createDTO = new CreateConversationDTO();
        // customerId é obrigatório, mas não está sendo definido

        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldFindConversationById() throws Exception {
        // Criar uma conversa primeiro via service
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        String createResponse = mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extrair o ID da resposta
        String conversationId = objectMapper.readTree(createResponse).get("id").asText();

        // Buscar a conversa criada
        mockMvc.perform(get("/api/conversations/{id}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId))
                .andExpect(jsonPath("$.status").value("ENTRADA"))
                .andExpect(jsonPath("$.customerId").value(testCustomer.getId().toString()));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldReturnNotFoundForNonexistentConversation() throws Exception {
        UUID nonexistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/conversations/{id}", nonexistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldFindConversationsByStatus() throws Exception {
        // Criar algumas conversas
        CreateConversationDTO createDTO1 = new CreateConversationDTO();
        createDTO1.setCustomerId(testCustomer.getId());
        createDTO1.setChannel(Channel.WHATSAPP);
        createDTO1.setPriority(1);

        CreateConversationDTO createDTO2 = new CreateConversationDTO();
        createDTO2.setCustomerId(testCustomer.getId());
        createDTO2.setChannel(Channel.INSTAGRAM);
        createDTO2.setPriority(2);

        // Criar as conversas
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO2)))
                .andExpect(status().isCreated());

        // Buscar conversas por status
        mockMvc.perform(get("/api/conversations")
                .param("status", "ENTRADA")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].status").value("ENTRADA"))
                .andExpect(jsonPath("$.content[1].status").value("ENTRADA"));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldAssignConversationToUser() throws Exception {
        // Criar uma conversa
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        String createResponse = mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String conversationId = objectMapper.readTree(createResponse).get("id").asText();

        // Atribuir a conversa ao usuário
        mockMvc.perform(put("/api/conversations/{id}/assign/{userId}", conversationId, testUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedUserId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.status").value("ESPERANDO"));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldChangeConversationStatus() throws Exception {
        // Criar uma conversa
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(testCustomer.getId());
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        String createResponse = mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String conversationId = objectMapper.readTree(createResponse).get("id").asText();

        // Alterar o status da conversa
        mockMvc.perform(put("/api/conversations/{id}/status", conversationId)
                .param("status", "FINALIZADOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADOS"));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    void shouldCountConversationsByStatus() throws Exception {
        // Criar algumas conversas
        CreateConversationDTO createDTO1 = new CreateConversationDTO();
        createDTO1.setCustomerId(testCustomer.getId());
        createDTO1.setChannel(Channel.WHATSAPP);

        CreateConversationDTO createDTO2 = new CreateConversationDTO();
        createDTO2.setCustomerId(testCustomer.getId());
        createDTO2.setChannel(Channel.INSTAGRAM);

        // Criar as conversas
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO2)))
                .andExpect(status().isCreated());

        // Contar conversas por status
        mockMvc.perform(get("/api/conversations/stats/count")
                .param("status", "ENTRADA"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
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