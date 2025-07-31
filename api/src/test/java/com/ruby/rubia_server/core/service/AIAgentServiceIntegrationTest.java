package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
@Transactional
public class AIAgentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AIAgentService aiAgentService;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private AIModelRepository aiModelRepository;

    private Company testCompany;
    private UUID testCompanyId;
    private AIModel testGPT4Model;
    private AIModel testClaudeModel;

    @BeforeEach
    void setUp() {
        // Clean up data before each test
        aiAgentRepository.deleteAll();
        companyRepository.deleteAll();
        companyGroupRepository.deleteAll();
        aiModelRepository.deleteAll();

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
                .maxAiAgents(10) // Permitir múltiplos agentes para testes
                .build();
        testCompany = companyRepository.save(testCompany);
        testCompanyId = testCompany.getId();

        // Create test AI models
        testGPT4Model = AIModel.builder()
                .name("gpt-4")
                .displayName("GPT-4")
                .provider("OpenAI")
                .capabilities("Conversação avançada, análise de texto, geração de conteúdo")
                .impactDescription("Modelo premium com alta qualidade de resposta")
                .costPer1kTokens(30)
                .performanceLevel("HIGH")
                .isActive(true)
                .sortOrder(1)
                .build();
        testGPT4Model = aiModelRepository.save(testGPT4Model);

        testClaudeModel = AIModel.builder()
                .name("claude-3.5-sonnet")
                .displayName("Claude 3.5 Sonnet")
                .provider("Anthropic")
                .capabilities("Conversação natural, análise detalhada, raciocínio lógico")
                .impactDescription("Modelo equilibrado entre qualidade e custo")
                .costPer1kTokens(15)
                .performanceLevel("MEDIUM")
                .isActive(true)
                .sortOrder(2)
                .build();
        testClaudeModel = aiModelRepository.save(testClaudeModel);
    }

    @Test
    void createAIAgent_ShouldCreateSuccessfully_WithValidCompanyRelationship() {
        // Given
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .description("Assistente virtual especializada em atendimento ao cliente")
                .avatarBase64("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        // When
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // Then
        assertNotNull(createdAgent);
        assertNotNull(createdAgent.getId());
        assertEquals("Sofia", createdAgent.getName());
        assertEquals("Assistente virtual especializada em atendimento ao cliente", createdAgent.getDescription());
        assertEquals("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD", createdAgent.getAvatarBase64());
        assertEquals(testGPT4Model.getId(), createdAgent.getAiModel().getId());
        assertEquals("AMIGAVEL", createdAgent.getTemperament());
        assertEquals(500, createdAgent.getMaxResponseLength());
        assertEquals(BigDecimal.valueOf(0.7), createdAgent.getTemperature());
        assertTrue(createdAgent.getIsActive());
        
        // Timestamps should now be properly set after fix
        assertNotNull(createdAgent.getCreatedAt());
        assertNotNull(createdAgent.getUpdatedAt());

        // Test database relationship
        assertNotNull(createdAgent.getCompany());
        assertEquals(testCompanyId, createdAgent.getCompany().getId());
        assertEquals("Test Company", createdAgent.getCompany().getName());
        assertEquals("test-company", createdAgent.getCompany().getSlug());

        // Verify it's persisted in database
        AIAgent fromDb = aiAgentRepository.findById(createdAgent.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(createdAgent.getName(), fromDb.getName());
        assertEquals(testCompanyId, fromDb.getCompany().getId());
    }

    @Test
    void createAIAgent_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        UUID nonExistentCompanyId = UUID.randomUUID();
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(nonExistentCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .build();

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> aiAgentService.createAIAgent(createDTO));
        
        assertEquals("Company not found with ID: " + nonExistentCompanyId, exception.getMessage());
    }

    @Test
    void createAIAgent_ShouldHandleValidationConstraints() {
        // Test temperature validation
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .temperature(BigDecimal.valueOf(1.5)) // Invalid - exceeds 1.0
                .build();

        // This should be caught by Bean Validation in real application
        // But in service layer, it might pass through
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);
        
        // Verify it was created with the invalid value (shows validation happens at controller level)
        assertNotNull(createdAgent);
        assertEquals(BigDecimal.valueOf(1.5), createdAgent.getTemperature());
    }

    @Test
    void findByCompanyId_ShouldReturnCorrectAgents() {
        // Given - Create multiple agents for different companies
        CreateAIAgentDTO agent1 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .build();

        CreateAIAgentDTO agent2 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Ana")
                .aiModelId(testClaudeModel.getId())
                .temperament("FORMAL")
                .build();

        // Create another company
        CompanyGroup anotherGroup = CompanyGroup.builder()
                .name("Another Company Group")
                .build();
        anotherGroup = companyGroupRepository.save(anotherGroup);

        Company anotherCompany = Company.builder()
                .name("Another Company")
                .slug("another-company")
                .companyGroup(anotherGroup)
                .build();
        anotherCompany = companyRepository.save(anotherCompany);

        CreateAIAgentDTO agent3 = CreateAIAgentDTO.builder()
                .companyId(anotherCompany.getId())
                .name("Carlos")
                .aiModelId(testClaudeModel.getId())
                .temperament("SERIO")
                .build();

        // When
        AIAgent createdAgent1 = aiAgentService.createAIAgent(agent1);
        AIAgent createdAgent2 = aiAgentService.createAIAgent(agent2);
        AIAgent createdAgent3 = aiAgentService.createAIAgent(agent3);

        // Then
        List<AIAgent> testCompanyAgents = aiAgentService.getAIAgentsByCompanyId(testCompanyId);
        List<AIAgent> anotherCompanyAgents = aiAgentService.getAIAgentsByCompanyId(anotherCompany.getId());

        assertEquals(2, testCompanyAgents.size());
        assertEquals(1, anotherCompanyAgents.size());

        // Verify each agent belongs to correct company
        testCompanyAgents.forEach(agent -> {
            assertEquals(testCompanyId, agent.getCompany().getId());
            assertTrue(List.of("Sofia", "Ana").contains(agent.getName()));
        });

        final UUID finalAnotherCompanyId = anotherCompany.getId();
        anotherCompanyAgents.forEach(agent -> {
            assertEquals(finalAnotherCompanyId, agent.getCompany().getId());
            assertEquals("Carlos", agent.getName());
        });
    }

    @Test
    void findActiveByCompanyId_ShouldReturnOnlyActiveAgents() {
        // Given - Create active and inactive agents
        CreateAIAgentDTO activeAgent = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .isActive(true)
                .build();

        CreateAIAgentDTO inactiveAgent = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Ana")
                .aiModelId(testClaudeModel.getId())
                .temperament("FORMAL")
                .isActive(false)
                .build();

        // When
        AIAgent createdActiveAgent = aiAgentService.createAIAgent(activeAgent);
        AIAgent createdInactiveAgent = aiAgentService.createAIAgent(inactiveAgent);

        // Then
        List<AIAgent> activeAgents = aiAgentService.getActiveAIAgentsByCompanyId(testCompanyId);
        
        assertEquals(1, activeAgents.size());
        assertEquals("Sofia", activeAgents.get(0).getName());
        assertTrue(activeAgents.get(0).getIsActive());
    }

    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        CreateAIAgentDTO agent1 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .build();

        CreateAIAgentDTO agent2 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Ana")
                .aiModelId(testClaudeModel.getId())
                .temperament("FORMAL")
                .build();

        // When
        aiAgentService.createAIAgent(agent1);
        aiAgentService.createAIAgent(agent2);

        // Then
        long count = aiAgentService.countAIAgentsByCompanyId(testCompanyId);
        assertEquals(2L, count);
    }

    @Test
    void existsByNameAndCompanyId_ShouldDetectDuplicates() {
        // Given
        CreateAIAgentDTO agent = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .build();

        // When
        aiAgentService.createAIAgent(agent);

        // Then
        assertTrue(aiAgentService.existsByNameAndCompanyId("Sofia", testCompanyId));
        assertFalse(aiAgentService.existsByNameAndCompanyId("Ana", testCompanyId));
        assertFalse(aiAgentService.existsByNameAndCompanyId("Sofia", UUID.randomUUID()));
    }

    @Test
    void deleteAIAgent_ShouldMaintainDatabaseIntegrity() {
        // Given
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .build();

        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);
        UUID agentId = createdAgent.getId();

        // Verify it exists
        assertTrue(aiAgentRepository.existsById(agentId));

        // When
        boolean deleted = aiAgentService.deleteAIAgent(agentId);

        // Then
        assertTrue(deleted);
        assertFalse(aiAgentRepository.existsById(agentId));
        
        // Verify company is still there (cascade should not delete company)
        assertTrue(companyRepository.existsById(testCompanyId));
    }

    @Test
    void createAIAgent_ShouldTestDatabaseConstraints() {
        // Test creating multiple agents with same name for same company
        // This should work based on current schema (no unique constraint)
        CreateAIAgentDTO agent1 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .build();

        CreateAIAgentDTO agent2 = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Sofia") // Same name
                .aiModelId(testClaudeModel.getId())
                .temperament("FORMAL")
                .build();

        // When
        AIAgent createdAgent1 = aiAgentService.createAIAgent(agent1);
        AIAgent createdAgent2 = aiAgentService.createAIAgent(agent2);

        // Then - Both should be created successfully
        assertNotNull(createdAgent1);
        assertNotNull(createdAgent2);
        assertNotEquals(createdAgent1.getId(), createdAgent2.getId());
        assertEquals("Sofia", createdAgent1.getName());
        assertEquals("Sofia", createdAgent2.getName());

        // Verify both exist in database
        List<AIAgent> allAgents = aiAgentService.getAIAgentsByCompanyId(testCompanyId);
        assertEquals(2, allAgents.size());
    }
}