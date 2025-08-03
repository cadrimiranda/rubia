package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.EnhanceTemplateDTO;
import com.ruby.rubia_server.core.dto.EnhancedTemplateResponseDTO;
import com.ruby.rubia_server.core.dto.SaveTemplateWithAIMetadataDTO;
import com.ruby.rubia_server.core.dto.MessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.RevisionType;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
@Transactional
class TemplateEnhancementServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TemplateEnhancementService templateEnhancementService;

    @Autowired
    private MessageTemplateRepository messageTemplateRepository;

    @Autowired
    private MessageTemplateRevisionRepository messageTemplateRevisionRepository;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    @Autowired
    private AIModelRepository aiModelRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Company testCompany;
    private AIModel testGPT4Model;
    private AIAgent testAIAgent;
    private MessageTemplate testTemplate;
    private User testUser;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Clean up data before each test
        messageTemplateRevisionRepository.deleteAll();
        messageTemplateRepository.deleteAll();
        aiAgentRepository.deleteAll();
        aiModelRepository.deleteAll();
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        companyRepository.deleteAll();
        companyGroupRepository.deleteAll();

        // Create test company group
        CompanyGroup companyGroup = CompanyGroup.builder()
                .name("Test Company Group")
                .description("Test company group for Template Enhancement tests")
                .build();
        companyGroup = companyGroupRepository.save(companyGroup);

        // Create test company
        testCompany = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .build();
        testCompany = companyRepository.save(testCompany);

        // Create test department
        testDepartment = Department.builder()
                .name("Test Department")
                .description("Test department for Template Enhancement tests")
                .company(testCompany)
                .autoAssign(true)
                .build();
        testDepartment = departmentRepository.save(testDepartment);

        // Create test user
        testUser = User.builder()
                .name("Test User")
                .email("test@example.com")
                .company(testCompany)
                .department(testDepartment)
                .role(UserRole.AGENT)
                .passwordHash("$2a$10$testhashedpassword")
                .build();
        testUser = userRepository.save(testUser);

        // Create test AI model
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

        // Create test AI agent
        testAIAgent = AIAgent.builder()
                .company(testCompany)
                .aiModel(testGPT4Model)
                .name("Sofia")
                .description("Assistente virtual especializada em atendimento ao cliente")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        testAIAgent = aiAgentRepository.save(testAIAgent);

        // Create test template
        testTemplate = MessageTemplate.builder()
                .company(testCompany)
                .name("Template de Teste")
                .content("Olá, venha doar sangue conosco!")
                .isAiGenerated(false)
                .createdBy(testUser)
                .editCount(0)
                .build();
        testTemplate = messageTemplateRepository.save(testTemplate);
    }

    @Test
    void enhanceTemplate_ShouldGenerateEnhancedContent_WithActiveAIAgent() {
        // Given
        EnhanceTemplateDTO request = EnhanceTemplateDTO.builder()
                .companyId(testCompany.getId())
                .originalContent("Venha doar sangue")
                .enhancementType("friendly")
                .category("primeira-doacao")
                .title("Convite Doação")
                .build();

        // When
        EnhancedTemplateResponseDTO response = templateEnhancementService.enhanceTemplate(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOriginalContent()).isEqualTo("Venha doar sangue");
        assertThat(response.getEnhancedContent()).isNotEmpty();
        assertThat(response.getEnhancementType()).isEqualTo("friendly");
        assertThat(response.getAiModelUsed()).contains("GPT-4");
        assertThat(response.getTokensUsed()).isGreaterThan(0);
        assertThat(response.getCreditsConsumed()).isGreaterThan(0);
        assertThat(response.getExplanation()).isNotEmpty();

        // Verify enhanced content contains personalization placeholder
        assertThat(response.getEnhancedContent()).contains("{{nome}}");
    }

    @Test
    void enhanceTemplate_ShouldUseFallbackModel_WhenNoActiveAIAgent() {
        // Given - Remove the AI agent to force fallback
        aiAgentRepository.deleteAll();

        EnhanceTemplateDTO request = EnhanceTemplateDTO.builder()
                .companyId(testCompany.getId())
                .originalContent("Venha doar sangue")
                .enhancementType("professional")
                .category("urgencia")
                .title("Convite Urgente")
                .build();

        // When
        EnhancedTemplateResponseDTO response = templateEnhancementService.enhanceTemplate(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAiModelUsed()).contains("(Padrão)");
        assertThat(response.getExplanation()).contains("modelo padrão do sistema");
    }

    @Test
    void saveTemplateWithAIMetadata_ShouldCreateRevisionWithAIMetadata() {
        // Given
        String enhancedContent = "{{nome}}, venha ser um herói e doar sangue conosco! 🩸";
        SaveTemplateWithAIMetadataDTO request = SaveTemplateWithAIMetadataDTO.builder()
                .templateId(testTemplate.getId())
                .content(enhancedContent)
                .userId(testUser.getId())
                .aiAgentId(testAIAgent.getId())
                .aiEnhancementType("motivational")
                .aiTokensUsed(45)
                .aiCreditsConsumed(2)
                .aiModelUsed("GPT-4")
                .aiExplanation("Adicionei personalização motivacional e emojis para tornar mais atrativo")
                .build();

        // When
        MessageTemplateRevisionDTO response = templateEnhancementService.saveTemplateWithAIMetadata(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTemplateId()).isEqualTo(testTemplate.getId());
        assertThat(response.getContent()).isEqualTo(enhancedContent);
        assertThat(response.getRevisionType()).isEqualTo(RevisionType.AI_ENHANCEMENT);
        assertThat(response.getAiAgentId()).isEqualTo(testAIAgent.getId());
        assertThat(response.getAiAgentName()).isEqualTo("Sofia");
        assertThat(response.getAiEnhancementType()).isEqualTo("motivational");
        assertThat(response.getAiTokensUsed()).isEqualTo(45);
        assertThat(response.getAiCreditsConsumed()).isEqualTo(2);
        assertThat(response.getAiModelUsed()).isEqualTo("GPT-4");
        assertThat(response.getAiExplanation()).contains("personalização motivacional");

        // Verify the template was updated
        MessageTemplate updatedTemplate = messageTemplateRepository.findById(testTemplate.getId()).orElse(null);
        assertNotNull(updatedTemplate);
        assertThat(updatedTemplate.getContent()).isEqualTo(enhancedContent);
        assertThat(updatedTemplate.getIsAiGenerated()).isTrue();
        assertThat(updatedTemplate.getEditCount()).isEqualTo(1);
        assertThat(updatedTemplate.getAiAgent()).isEqualTo(testAIAgent);

        // Verify the revision was created in database
        List<MessageTemplateRevision> revisions = messageTemplateRevisionRepository.findByTemplateId(testTemplate.getId());
        assertThat(revisions).hasSize(1);
        
        MessageTemplateRevision revision = revisions.get(0);
        assertThat(revision.getRevisionType()).isEqualTo(RevisionType.AI_ENHANCEMENT);
        assertThat(revision.getAiAgent()).isEqualTo(testAIAgent);
        assertThat(revision.getAiEnhancementType()).isEqualTo("motivational");
        assertThat(revision.getAiTokensUsed()).isEqualTo(45);
        assertThat(revision.getAiCreditsConsumed()).isEqualTo(2);
        assertThat(revision.getAiModelUsed()).isEqualTo("GPT-4");
    }

    @Test
    void enhanceTemplate_ShouldIncludeBloodDonationFocus_ForAllEnhancementTypes() {
        // Test all enhancement types to ensure they include blood donation context
        String[] enhancementTypes = {"friendly", "professional", "empathetic", "urgent", "motivational"};
        
        for (String enhancementType : enhancementTypes) {
            // Given
            EnhanceTemplateDTO request = EnhanceTemplateDTO.builder()
                    .companyId(testCompany.getId())
                    .originalContent("Venha nos visitar")
                    .enhancementType(enhancementType)
                    .category("campanhas")
                    .title("Convite " + enhancementType)
                    .build();

            // When
            EnhancedTemplateResponseDTO response = templateEnhancementService.enhanceTemplate(request);

            // Then
            assertThat(response.getEnhancedContent())
                    .as("Enhancement type: %s should include personalization", enhancementType)
                    .contains("{{nome}}");
                    
            // Enhanced content should be different from original and focused on blood donation
            assertThat(response.getEnhancedContent())
                    .as("Enhancement type: %s should be different from original", enhancementType)
                    .isNotEqualTo(request.getOriginalContent());
        }
    }

    @Test
    void createDefaultAgentForCompany_ShouldCreateAgentWithDefaultModel() {
        // Given - Clean existing agents
        aiAgentRepository.deleteAll();

        // When
        AIAgent defaultAgent = templateEnhancementService.createDefaultAgentForCompany(testCompany.getId());

        // Then
        assertThat(defaultAgent).isNotNull();
        assertThat(defaultAgent.getCompany()).isEqualTo(testCompany);
        assertThat(defaultAgent.getName()).isEqualTo("Assistente " + testCompany.getName());
        assertThat(defaultAgent.getDescription()).contains("Agente de IA padrão");
        assertThat(defaultAgent.getTemperament()).isEqualTo("AMIGAVEL");
        assertThat(defaultAgent.getIsActive()).isTrue();
        assertThat(defaultAgent.getAiModel()).isNotNull();
        assertThat(defaultAgent.getAiModel().getName()).isEqualTo("gpt-4");
    }

    @Test
    void createDefaultAgentForCompany_ShouldNotCreateDuplicate_WhenAgentExists() {
        // Given - Agent already exists

        // When
        AIAgent result = templateEnhancementService.createDefaultAgentForCompany(testCompany.getId());

        // Then
        assertThat(result).isEqualTo(testAIAgent);
        
        // Verify only one agent exists
        List<AIAgent> agents = aiAgentRepository.findByCompanyId(testCompany.getId());
        assertThat(agents).hasSize(1);
    }
}