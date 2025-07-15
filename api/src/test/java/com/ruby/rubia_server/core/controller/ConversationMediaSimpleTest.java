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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

/**
 * Teste simples para verificar se a estrutura básica está funcionando
 */
@AutoConfigureMockMvc
@Transactional
class ConversationMediaSimpleTest extends AbstractIntegrationTest {

    @MockBean
    private CompanyContextUtil companyContextUtil;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private ConversationMediaRepository mediaRepository;

    @Test
    void contextLoads() {
        assertNotNull(companyRepository);
        assertNotNull(mediaRepository);
    }

    @Test
    void shouldCreateBasicTestData() {
        // Create test company
        CompanyGroup group = CompanyGroup.builder()
                .name("Test Group")
                .build();
        companyGroupRepository.save(group);

        Company company = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .contactEmail("test@test.com")
                .companyGroup(group)
                .build();
        companyRepository.save(company);

        assertNotNull(company.getId());
        assertTrue(companyRepository.existsById(company.getId()));
    }

    @Test
    void shouldMockCompanyContext() {
        UUID testCompanyId = UUID.randomUUID();
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(testCompanyId);
        
        UUID result = companyContextUtil.getCurrentCompanyId();
        assertEquals(testCompanyId, result);
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = {"AGENT"})
    void shouldTestMediaEndpointExists() throws Exception {
        UUID conversationId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/conversations/{conversationId}/media", conversationId))
                .andDo(print())
                .andExpect(status().isBadRequest()); // Esperamos 400 pois conversa não existe
    }

    @Test
    @WithMockUser(username = "test@test.com", roles = {"AGENT"})
    void shouldTestMediaUploadEndpointExists() throws Exception {
        UUID conversationId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "test.jpg", 
            "image/jpeg", 
            "test content".getBytes()
        );
        
        mockMvc.perform(multipart("/api/conversations/{conversationId}/media", conversationId)
                .file(file)
                .param("mediaType", "IMAGE"))
                .andDo(print()); // Vamos ver qual é o status real
    }
}