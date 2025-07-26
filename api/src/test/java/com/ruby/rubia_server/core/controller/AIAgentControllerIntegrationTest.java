package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "logging.level.org.springframework.web=DEBUG",
    "spring.security.enabled=false"
})
@Transactional
public class AIAgentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    private Company testCompany;
    private UUID testCompanyId;

    @BeforeEach
    void setUp() {
        // Clean up data before each test
        aiAgentRepository.deleteAll();
        companyRepository.deleteAll();
        companyGroupRepository.deleteAll();

        // Create test company group
        CompanyGroup companyGroup = CompanyGroup.builder()
                .name("Test Company Group")
                .description("Test company group for AI Agent tests")
                .build();
        companyGroup = companyGroupRepository.save(companyGroup);

        // Create test company
        testCompany = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .build();
        testCompany = companyRepository.save(testCompany);
        testCompanyId = testCompany.getId();
    }

    @Test
    void createAIAgent_ShouldCreateSuccessfully_WhenValidData() throws Exception {
        // Given
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .description("Assistente virtual especializada em atendimento ao cliente")
                .avatarUrl("https://example.com/avatar.jpg")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        // When & Then
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Sofia"))
                .andExpect(jsonPath("$.description").value("Assistente virtual especializada em atendimento ao cliente"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.aiModelType").value("GPT-4"))
                .andExpect(jsonPath("$.temperament").value("AMIGAVEL"))
                .andExpect(jsonPath("$.maxResponseLength").value(500))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.companyId").value(testCompanyId.toString()))
                .andExpect(jsonPath("$.companyName").value("Test Company"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void createAIAgent_ShouldReturnBadRequest_WhenMissingRequiredFields() throws Exception {
        // Given - Missing required name field
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .build();

        // When & Then
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAIAgent_ShouldReturnNotFound_WhenCompanyNotExists() throws Exception {
        // Given
        UUID nonExistentCompanyId = UUID.randomUUID();
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(nonExistentCompanyId)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .build();

        // When & Then
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Company not found")));
    }

    @Test
    void createAIAgent_ShouldValidateTemperatureRange() throws Exception {
        // Given - Invalid temperature (greater than 1.0)
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .temperature(BigDecimal.valueOf(1.5))
                .build();

        // When & Then
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAIAgent_ShouldValidateMaxResponseLength() throws Exception {
        // Given - Invalid maxResponseLength (exceeds limit)
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(15000) // Exceeds max of 10000
                .build();

        // When & Then
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAIAgent_ShouldReturnAgent_WhenExists() throws Exception {
        // Given - Create an AI Agent first
        AIAgent aiAgent = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .description("Test description")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        aiAgent = aiAgentRepository.save(aiAgent);

        // When & Then
        mockMvc.perform(get("/api/ai-agents/{id}", aiAgent.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aiAgent.getId().toString()))
                .andExpect(jsonPath("$.name").value("Sofia"))
                .andExpect(jsonPath("$.description").value("Test description"))
                .andExpect(jsonPath("$.aiModelType").value("GPT-4"))
                .andExpect(jsonPath("$.temperament").value("AMIGAVEL"))
                .andExpect(jsonPath("$.companyId").value(testCompanyId.toString()));
    }

    @Test
    void getAIAgent_ShouldReturnNotFound_WhenNotExists() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When & Then
        mockMvc.perform(get("/api/ai-agents/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAIAgent_ShouldUpdateSuccessfully_WhenValidData() throws Exception {
        // Given - Create an AI Agent first
        AIAgent aiAgent = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .description("Original description")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        aiAgent = aiAgentRepository.save(aiAgent);

        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .name("Sofia Updated")
                .description("Updated description")
                .aiModelType("Claude 3.5")
                .temperament("EMPATICO")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(false)
                .build();

        // When & Then
        mockMvc.perform(put("/api/ai-agents/{id}", aiAgent.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aiAgent.getId().toString()))
                .andExpect(jsonPath("$.name").value("Sofia Updated"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.aiModelType").value("Claude 3.5"))
                .andExpect(jsonPath("$.temperament").value("EMPATICO"))
                .andExpect(jsonPath("$.maxResponseLength").value(800))
                .andExpect(jsonPath("$.temperature").value(0.5))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void deleteAIAgent_ShouldDeleteSuccessfully_WhenExists() throws Exception {
        // Given - Create an AI Agent first
        AIAgent aiAgent = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        aiAgent = aiAgentRepository.save(aiAgent);

        // When & Then
        mockMvc.perform(delete("/api/ai-agents/{id}", aiAgent.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/ai-agents/{id}", aiAgent.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAIAgentsByCompany_ShouldReturnAgentsForCompany() throws Exception {
        // Given - Create multiple AI Agents for the company
        AIAgent agent1 = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        AIAgent agent2 = AIAgent.builder()
                .company(testCompany)
                .name("Ana")
                .aiModelType("Claude 3.5")
                .temperament("FORMAL")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(false)
                .build();

        aiAgentRepository.save(agent1);
        aiAgentRepository.save(agent2);

        // When & Then
        mockMvc.perform(get("/api/ai-agents/company/{companyId}", testCompanyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value(anyOf(equalTo("Sofia"), equalTo("Ana"))))
                .andExpect(jsonPath("$[1].name").value(anyOf(equalTo("Sofia"), equalTo("Ana"))))
                .andExpect(jsonPath("$[0].companyId").value(testCompanyId.toString()))
                .andExpect(jsonPath("$[1].companyId").value(testCompanyId.toString()));
    }

    @Test
    void getActiveAIAgentsByCompany_ShouldReturnOnlyActiveAgents() throws Exception {
        // Given - Create both active and inactive AI Agents
        AIAgent activeAgent = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        AIAgent inactiveAgent = AIAgent.builder()
                .company(testCompany)
                .name("Ana")
                .aiModelType("Claude 3.5")
                .temperament("FORMAL")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(false)
                .build();

        aiAgentRepository.save(activeAgent);
        aiAgentRepository.save(inactiveAgent);

        // When & Then
        mockMvc.perform(get("/api/ai-agents/company/{companyId}/active", testCompanyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sofia"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void countAIAgentsByCompany_ShouldReturnCorrectCount() throws Exception {
        // Given - Create AI Agents for the company
        AIAgent agent1 = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        AIAgent agent2 = AIAgent.builder()
                .company(testCompany)
                .name("Ana")
                .aiModelType("Claude 3.5")
                .temperament("FORMAL")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(false)
                .build();

        aiAgentRepository.save(agent1);
        aiAgentRepository.save(agent2);

        // When & Then
        mockMvc.perform(get("/api/ai-agents/company/{companyId}/count", testCompanyId))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void checkAIAgentExists_ShouldReturnTrue_WhenAgentExistsWithName() throws Exception {
        // Given - Create an AI Agent
        AIAgent aiAgent = AIAgent.builder()
                .company(testCompany)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        aiAgentRepository.save(aiAgent);

        // When & Then
        mockMvc.perform(get("/api/ai-agents/company/{companyId}/exists", testCompanyId)
                .param("name", "Sofia"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkAIAgentExists_ShouldReturnFalse_WhenAgentNotExistsWithName() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/ai-agents/company/{companyId}/exists", testCompanyId)
                .param("name", "NonExistentAgent"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void createAIAgent_ShouldTestDatabaseConstraints() throws Exception {
        // Given - Create first agent
        CreateAIAgentDTO createDTO1 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        // Create first agent successfully
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO1)))
                .andExpect(status().isCreated());

        // Given - Try to create agent with same name for same company (should be allowed based on current schema)
        CreateAIAgentDTO createDTO2 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")  // Same name
                .aiModelType("Claude 3.5")
                .temperament("FORMAL")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(true)
                .build();

        // This should succeed if there's no unique constraint on name+company_id
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO2)))
                .andExpect(status().isCreated());

        // Verify both agents exist
        mockMvc.perform(get("/api/ai-agents/company/{companyId}/count", testCompanyId))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    void createAIAgent_ShouldTestForeignKeyConstraint() throws Exception {
        // Given - Non-existent company ID
        UUID nonExistentCompanyId = UUID.fromString("12345678-1234-1234-1234-123456789abc");
        
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(nonExistentCompanyId)
                .name("Sofia")
                .aiModelType("GPT-4")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        // When & Then - Should fail due to foreign key constraint
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isNotFound());
    }
}