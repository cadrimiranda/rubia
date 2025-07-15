package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.*;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Testes de integração críticos para ConversationMedia focados em segurança multi-tenant
 * e validação de acesso.
 * 
 * PRIORIDADE: CRÍTICA - Estes testes cobrem os principais riscos de segurança:
 * 1. Isolamento entre empresas (maior risco de vazamento de dados)
 * 2. Validação de propriedade de conversas
 * 3. Validação de tipos de arquivo maliciosos
 */
@AutoConfigureMockMvc
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ConversationMediaControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private ConversationMediaRepository mediaRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    // Test data
    private Company companyA;
    private Company companyB;
    private Department departmentA;
    private Department departmentB;
    private Customer customerA;
    private Customer customerB;
    private User userA;
    private User userB;
    private Conversation conversationA;
    private Conversation conversationB;
    private ConversationMedia mediaA;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create test companies
        CompanyGroup group = CompanyGroup.builder()
                .name("Test Group")
                .build();
        companyGroupRepository.save(group);

        companyA = Company.builder()
                .name("Company A")
                .slug("company-a")
                .contactEmail("companya@test.com")
                .companyGroup(group)
                .build();
        companyRepository.save(companyA);

        companyB = Company.builder()
                .name("Company B")
                .slug("company-b")
                .contactEmail("companyb@test.com")
                .companyGroup(group)
                .build();
        companyRepository.save(companyB);

        // Create test departments
        departmentA = Department.builder()
                .name("Department A")
                .description("Test department for company A")
                .company(companyA)
                .build();
        departmentRepository.save(departmentA);

        departmentB = Department.builder()
                .name("Department B")
                .description("Test department for company B")
                .company(companyB)
                .build();
        departmentRepository.save(departmentB);

        // Create test customers
        customerA = Customer.builder()
                .name("Customer A")
                .phone("+5511999999999")
                .company(companyA)
                .build();
        customerRepository.save(customerA);

        customerB = Customer.builder()
                .name("Customer B")
                .phone("+5511888888888")
                .company(companyB)
                .build();
        customerRepository.save(customerB);

        // Create test users
        userA = User.builder()
                .name("User A")
                .email("usera@test.com")
                .passwordHash("hashedpassword")
                .role(UserRole.AGENT)
                .company(companyA)
                .department(departmentA)
                .build();
        userRepository.save(userA);

        userB = User.builder()
                .name("User B")
                .email("userb@test.com")
                .passwordHash("hashedpassword")
                .role(UserRole.AGENT)
                .company(companyB)
                .department(departmentB)
                .build();
        userRepository.save(userB);

        // Create test conversations
        conversationA = Conversation.builder()
                .assignedUser(userA)
                .company(companyA)
                .status(ConversationStatus.ENTRADA)
                .channel(Channel.WHATSAPP)
                .build();
        conversationRepository.save(conversationA);

        conversationB = Conversation.builder()
                .assignedUser(userB)
                .company(companyB)
                .status(ConversationStatus.ENTRADA)
                .channel(Channel.WHATSAPP)
                .build();
        conversationRepository.save(conversationB);

        // Create conversation participants
        ConversationParticipant participantA = ConversationParticipant.builder()
                .conversation(conversationA)
                .customer(customerA)
                .company(companyA)
                .isActive(true)
                .build();
        participantRepository.save(participantA);

        ConversationParticipant participantB = ConversationParticipant.builder()
                .conversation(conversationB)
                .customer(customerB)
                .company(companyB)
                .isActive(true)
                .build();
        participantRepository.save(participantB);

        // Create test media for Company A
        mediaA = new ConversationMedia();
        mediaA.setCompany(companyA);
        mediaA.setConversation(conversationA);
        mediaA.setFileUrl("/uploads/test-image.jpg");
        mediaA.setMediaType(com.ruby.rubia_server.core.enums.MediaType.IMAGE);
        mediaA.setMimeType("image/jpeg");
        mediaA.setOriginalFileName("test-image.jpg");
        mediaA.setFileSizeBytes(1024L);
        mediaA.setUploadedByUser(userA);
        mediaRepository.save(mediaA);
        
        // Setup default mock - individual tests can override this
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyA.getId());
        when(companyContextUtil.getAuthenticatedUserId()).thenReturn(userA.getId());
    }

    // ==========================================================================
    // 🔴 TESTE CRÍTICO 1: Isolamento Multi-tenant - Upload de Mídia
    // ==========================================================================
    
    @Test
    @WithMockUser(username = "usera@test.com", roles = {"AGENT"})
    void shouldNotAllowMediaUploadToConversationFromDifferentCompany() throws Exception {
        // Given: User from Company A tries to upload media to conversation from Company B
        // Note: Mock setup is in @BeforeEach with companyA.getId()
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            "test image content".getBytes()
        );
        
        // When & Then: Should be rejected with 400 (conversation not found for this company)
        mockMvc.perform(multipart("/api/conversations/{conversationId}/media", conversationB.getId())
                .file(file)
                .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest());
        
        // Verify no media was created for the cross-company attempt
        long mediaCount = mediaRepository.countByConversationId(conversationB.getId());
        assertEquals(0, mediaCount);
    }

    // ==========================================================================
    // 🔴 TESTE CRÍTICO 2: Isolamento Multi-tenant - Listagem de Mídia
    // ==========================================================================
    
    @Test
    @WithMockUser(username = "userb@test.com", roles = {"AGENT"})
    void shouldNotAllowAccessToMediaFromDifferentCompany() throws Exception {
        // Given: User from Company B tries to access media from Company A's conversation
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyB.getId());
        
        // When & Then: Should be rejected with 400 (conversation not found for this company)
        mockMvc.perform(get("/api/conversations/{conversationId}/media", conversationA.getId()))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // 🔴 TESTE CRÍTICO 3: Validação de Arquivos Maliciosos
    // ==========================================================================
    
    @Test
    @WithMockUser(username = "usera@test.com", roles = {"AGENT"})
    void shouldRejectExecutableFiles() throws Exception {
        // Given: User tries to upload executable file
        // Note: Mock setup is in @BeforeEach with companyA.getId()
        
        MockMultipartFile maliciousFile = new MockMultipartFile(
            "file", 
            "malware.exe", 
            "application/octet-stream", 
            "fake executable content".getBytes()
        );
        
        // When & Then: Should be rejected
        // Note: This test assumes validation exists at controller/service level
        // If not implemented yet, this test will help identify the security gap
        mockMvc.perform(multipart("/api/conversations/{conversationId}/media", conversationA.getId())
                .file(maliciousFile)
                .param("mediaType", "DOCUMENT"))
                .andExpect(status().isBadRequest()); // Or whatever status the validation returns
    }
    
    @Test
    @WithMockUser(username = "usera@test.com", roles = {"AGENT"})
    void shouldRejectFilesWithMultipleExtensions() throws Exception {
        // Given: User tries to upload file with double extension (common malware technique)
        // Note: Mock setup is in @BeforeEach with companyA.getId()
        
        MockMultipartFile suspiciousFile = new MockMultipartFile(
            "file", 
            "document.pdf.exe", 
            "application/pdf", 
            "fake pdf content".getBytes()
        );
        
        // When & Then: Should be rejected
        mockMvc.perform(multipart("/api/conversations/{conversationId}/media", conversationA.getId())
                .file(suspiciousFile)
                .param("mediaType", "DOCUMENT"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // 🔴 TESTE CRÍTICO 4: Validação de Propriedade da Conversa
    // ==========================================================================
    
    @Test
    @WithMockUser(username = "usera@test.com", roles = {"AGENT"})
    void shouldRejectUploadToNonExistentConversation() throws Exception {
        // Given: User tries to upload to non-existent conversation
        // Note: Mock setup is in @BeforeEach with companyA.getId()
        
        UUID nonExistentConversationId = UUID.randomUUID();
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            "test content".getBytes()
        );
        
        // When & Then: Should be rejected with 400
        mockMvc.perform(multipart("/api/conversations/{conversationId}/media", nonExistentConversationId)
                .file(file)
                .param("mediaType", "IMAGE"))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================================
    // ✅ TESTE POSITIVO: Upload Válido
    // ==========================================================================
    
    @Test
    @Transactional
    @WithMockUser(username = "usera@test.com", roles = {"AGENT"})
    void shouldSuccessfullyUploadValidImageToOwnConversation() throws Exception {
        // Given: User uploads valid image to their own company's conversation
        // Note: Mock setup is in @BeforeEach with companyA.getId()
        
        MockMultipartFile validFile = new MockMultipartFile(
            "file", 
            "profile.jpg", 
            "image/jpeg", 
            "valid jpeg content".getBytes()
        );
        
        // When & Then: Should succeed
        mockMvc.perform(multipart("/api/conversations/{conversationId}/media", conversationA.getId())
                .file(validFile)
                .param("mediaType", "IMAGE"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.originalFileName").value("profile.jpg"))
                .andExpect(jsonPath("$.mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.conversationId").value(conversationA.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(companyA.getId().toString()));
        
        // Verify media was actually created
        long mediaCount = mediaRepository.countByConversationId(conversationA.getId());
        assertEquals(2, mediaCount); // 1 existing + 1 new
    }

    // ==========================================================================
    // ✅ TESTE POSITIVO: Listagem Válida
    // ==========================================================================
    
    @Test
    @WithMockUser(username = "usera@test.com", roles = {"AGENT"})
    void shouldSuccessfullyListMediaFromOwnConversation() throws Exception {
        // Given: User requests media from their own company's conversation
        // Note: Mock setup is in @BeforeEach with companyA.getId()
        
        // When & Then: Should succeed and return only media from the correct company
        mockMvc.perform(get("/api/conversations/{conversationId}/media", conversationA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(mediaA.getId().toString()))
                .andExpect(jsonPath("$[0].conversationId").value(conversationA.getId().toString()))
                .andExpect(jsonPath("$[0].companyId").value(companyA.getId().toString()));
    }

    // ==========================================================================
    // Helper method for assertion
    // ==========================================================================
    
    private void assertEquals(long expected, long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}