package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.AIEnhancementResult;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.repository.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
@Transactional
public class AIAgentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AIAgentService aiAgentService;

    @MockBean
    private OpenAIService openAIService;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private AIModelRepository aiModelRepository;

    @Autowired
    private MessageEnhancementAuditRepository auditRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Company testCompany;
    private UUID testCompanyId;
    private AIModel testGPT4Model;
    private AIModel testClaudeModel;
    private Department testDepartment;
    private User testUser;
    private Customer testCustomer;
    private Conversation testConversation;

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

        // Create test AI models - use existing or create new
        testGPT4Model = aiModelRepository.findByName("gpt-4o-mini")
                .orElseGet(() -> {
                    AIModel model = AIModel.builder()
                            .name("gpt-4o-mini")
                            .displayName("GPT-4o Mini")
                            .provider("OpenAI")
                            .capabilities("Conversação avançada, análise de texto, geração de conteúdo")
                            .impactDescription("Modelo padrão com boa qualidade e custo eficiente")
                            .costPer1kTokens(1)
                            .performanceLevel("HIGH")
                            .isActive(true)
                            .sortOrder(1)
                            .build();
                    return aiModelRepository.save(model);
                });

        testClaudeModel = aiModelRepository.findByName("claude-3.5-sonnet")
                .orElseGet(() -> {
                    AIModel model = AIModel.builder()
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
                    return aiModelRepository.save(model);
                });

        // Create test department
        testDepartment = Department.builder()
                .name("Test Department")
                .company(testCompany)
                .build();
        testDepartment = departmentRepository.save(testDepartment);

        // Create test user
        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .company(testCompany)
                .department(testDepartment)
                .role(UserRole.ADMIN)
                .passwordHash("hash")
                .build();
        testUser = userRepository.save(testUser);

        // Create test customer and conversation for enhancement tests
        testCustomer = Customer.builder()
                .name("Test Customer")
                .phone("5511999887766")
                .company(testCompany)
                .build();
        testCustomer = customerRepository.save(testCustomer);

        testConversation = Conversation.builder()
                .company(testCompany)
                .channel(Channel.WHATSAPP)
                .status(ConversationStatus.ENTRADA)
                .priority(1)
                .build();
        testConversation = conversationRepository.save(testConversation);

        // Setup OpenAI mocks
        setupOpenAIMocks();
    }

    private void setupOpenAIMocks() {
        // Mock successful OpenAI enhancement
        when(openAIService.enhanceTemplateWithPayload(anyString(), anyString(), anyDouble(), anyInt()))
                .thenReturn(AIEnhancementResult.builder()
                        .enhancedMessage("Olá! 😊 Como posso ajudar você hoje?")
                        .systemMessage("Você é um especialista em captação de doadores...")
                        .userMessage("Melhore esta mensagem: Olá")
                        .fullPayloadJson("{\"model\":\"gpt-4o-mini\",\"temperature\":0.7}")
                        .modelUsed("gpt-4o-mini")
                        .temperatureUsed(0.7)
                        .maxTokensUsed(150)
                        .tokensUsed(45)
                        .build());
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

    @Test
    void enhanceMessage_ShouldCreateCompleteAuditTrail() {
        // Given
        CreateAIAgentDTO agentDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Enhancement Agent")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        AIAgent agent = aiAgentService.createAIAgent(agentDTO);
        String originalMessage = "Olá";
        String userAgent = "Mozilla/5.0 Test Browser";
        String ipAddress = "192.168.1.100";

        // When
        String enhancedMessage = aiAgentService.enhanceMessage(
                testCompanyId,
                originalMessage,
                testUser.getId(),
                testConversation.getId(),
                userAgent,
                ipAddress
        );

        // Then
        assertThat(enhancedMessage).isEqualTo("Olá! 😊 Como posso ajudar você hoje?");

        // Verify OpenAI service was called
        verify(openAIService).enhanceTemplateWithPayload(
                contains("Use um tom caloroso, acolhedor e amigável"),
                eq("gpt-4o-mini"),
                eq(0.7),
                eq(500)
        );

        // Verify audit was created
        List<MessageEnhancementAudit> audits = auditRepository.findByCompanyId(testCompanyId, Pageable.unpaged()).getContent();
        assertThat(audits).hasSize(1);
        
        MessageEnhancementAudit audit = audits.get(0);
        assertThat(audit.getOriginalMessage()).isEqualTo(originalMessage);
        assertThat(audit.getEnhancedMessage()).isEqualTo("Olá! 😊 Como posso ajudar você hoje?");
        assertThat(audit.getTemperamentUsed()).isEqualTo("AMIGAVEL");
        assertThat(audit.getAiModelUsed()).isEqualTo("gpt-4o-mini");
        assertThat(audit.getTemperatureUsed()).isEqualTo(0.7);
        assertThat(audit.getTokensConsumed()).isEqualTo(45);
        assertThat(audit.getSuccess()).isTrue();
        assertThat(audit.getUserAgent()).isEqualTo(userAgent);
        assertThat(audit.getIpAddress()).isEqualTo(ipAddress);
        assertThat(audit.getOpenaiSystemMessage()).contains("especialista em captação");
        assertThat(audit.getOpenaiUserMessage()).isEqualTo("Melhore esta mensagem: Olá");
        assertThat(audit.getOpenaiFullPayload()).contains("gpt-4o-mini");
        assertThat(audit.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(audit.getAiAgent().getId()).isEqualTo(agent.getId());
        assertThat(audit.getConversationId()).isEqualTo(testConversation.getId());
    }

    @Test
    void enhanceMessage_ShouldHandleOpenAIFailuresWithErrorAudit() {
        // Given
        CreateAIAgentDTO agentDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Failing Agent")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .isActive(true)
                .build();

        AIAgent agent = aiAgentService.createAIAgent(agentDTO);
        String originalMessage = "Test message";
        
        // Mock OpenAI failure
        when(openAIService.enhanceTemplateWithPayload(anyString(), anyString(), anyDouble(), anyInt()))
                .thenThrow(new RuntimeException("OpenAI API Error"));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            aiAgentService.enhanceMessage(
                    testCompanyId,
                    originalMessage,
                    testUser.getId(),
                    testConversation.getId(),
                    "Test Browser",
                    "192.168.1.1"
            );
        });

        // Verify error audit was created
        List<MessageEnhancementAudit> audits = auditRepository.findByCompanyId(testCompanyId, Pageable.unpaged()).getContent();
        assertThat(audits).hasSize(1);
        
        MessageEnhancementAudit audit = audits.get(0);
        assertThat(audit.getSuccess()).isFalse();
        assertThat(audit.getErrorMessage()).contains("OpenAI API Error");
        assertThat(audit.getOriginalMessage()).isEqualTo(originalMessage);
        assertThat(audit.getEnhancedMessage()).isNull();
    }

    @Test
    void enhanceMessage_ShouldThrowExceptionWhenNoActiveAgents() {
        // Given - Create only inactive agent
        CreateAIAgentDTO agentDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Inactive Agent")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .isActive(false)
                .build();

        aiAgentService.createAIAgent(agentDTO);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiAgentService.enhanceMessage(
                    testCompanyId,
                    "Test message",
                    testUser.getId(),
                    testConversation.getId(),
                    "Test Browser",
                    "192.168.1.1"
            );
        });

        assertThat(exception.getMessage()).contains("Nenhum agente IA ativo encontrado");
    }

    @Test
    void enhanceMessage_ShouldIsolateByCompany() {
        // Given
        CreateAIAgentDTO agentDTO = CreateAIAgentDTO.builder()
                .companyId(testCompanyId)
                .name("Test Agent")
                .aiModelId(testGPT4Model.getId())
                .temperament("AMIGAVEL")
                .isActive(true)
                .build();

        aiAgentService.createAIAgent(agentDTO);

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

        final UUID finalAnotherCompanyId = anotherCompany.getId();

        // When & Then - Try to enhance with different company ID should fail
        assertThrows(RuntimeException.class, () -> {
            aiAgentService.enhanceMessage(
                    finalAnotherCompanyId, // Different company
                    "Test message",
                    testUser.getId(), // User from testCompany
                    testConversation.getId(),
                    "Test Browser",
                    "192.168.1.1"
            );
        });
    }
}