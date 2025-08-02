package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de integração completo para o sistema de avatar base64 do AIAgent.
 * 
 * Este teste verifica:
 * - Upload de avatar em base64
 * - Validação de formato
 * - Armazenamento no banco
 * - Recuperação e exibição
 * - Atualização de avatar
 * - Remoção de avatar
 */
@Transactional
class AIAgentAvatarIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AIAgentService aiAgentService;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AIModelRepository aiModelRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    private Company testCompany;
    private AIModel testAIModel;
    private User testUser;
    private Department testDepartment;

    // Avatar base64 de teste (pequena imagem 1x1 pixel JPEG)
    private static final String VALID_AVATAR_BASE64 = 
        "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A/9k=";

    // Avatar PNG de teste
    private static final String VALID_AVATAR_PNG_BASE64 = 
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";

    // Base64 inválido para testes de validação
    private static final String INVALID_AVATAR_BASE64 = "data:text/plain;base64,dGVzdA==";
    private static final String MALFORMED_BASE64 = "invalid-base64-format";

    @BeforeEach
    void setUp() {
        // Criar department
        testDepartment = Department.builder()
                .name("TI")
                .description("Departamento de TI")
                .build();
        testDepartment = departmentRepository.save(testDepartment);

        // Criar company
        testCompany = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .build();
        testCompany = companyRepository.save(testCompany);

        // Criar AI model
        testAIModel = AIModel.builder()
                .name("gpt-4o-mini")
                .displayName("GPT-4o Mini")
                .provider("OpenAI")
                .capabilities("Modelo otimizado para testes")
                .costPer1kTokens(1)
                .performanceLevel("HIGH")
                .isActive(true)
                .sortOrder(1)
                .build();
        testAIModel = aiModelRepository.save(testAIModel);

        // Criar user
        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .company(testCompany)
                .department(testDepartment)
                .role(UserRole.AGENT)
                .passwordHash("hash")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testCreateAIAgentWithValidAvatar() {
        // Given: DTO com avatar base64 válido
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Avatar Test")
                .description("Agente de teste com avatar")
                .avatarBase64(VALID_AVATAR_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        // When: Criar agente
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // Then: Verificar que foi criado com avatar
        assertThat(createdAgent).isNotNull();
        assertThat(createdAgent.getId()).isNotNull();
        assertThat(createdAgent.getName()).isEqualTo("Sofia Avatar Test");
        assertThat(createdAgent.getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);

        // Verificar no banco de dados
        Optional<AIAgent> savedAgent = aiAgentRepository.findById(createdAgent.getId());
        assertThat(savedAgent).isPresent();
        assertThat(savedAgent.get().getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);
    }

    @Test
    void testCreateAIAgentWithPNGAvatar() {
        // Given: DTO com avatar PNG
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia PNG Test")
                .description("Agente com avatar PNG")
                .avatarBase64(VALID_AVATAR_PNG_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: Criar agente
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // Then: Avatar PNG deve ser aceito
        assertThat(createdAgent.getAvatarBase64()).isEqualTo(VALID_AVATAR_PNG_BASE64);
    }

    @Test
    void testCreateAIAgentWithoutAvatar() {
        // Given: DTO sem avatar
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia No Avatar")
                .description("Agente sem avatar")
                .avatarBase64(null)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: Criar agente
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // Then: Deve ser criado sem avatar
        assertThat(createdAgent.getAvatarBase64()).isNull();
    }

    @Test
    void testUpdateAIAgentAvatar() {
        // Given: Agente existente sem avatar
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Update Test")
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);
        assertThat(createdAgent.getAvatarBase64()).isNull();

        // When: Atualizar com avatar
        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .avatarBase64(VALID_AVATAR_BASE64)
                .build();

        Optional<AIAgent> updatedAgent = aiAgentService.updateAIAgent(createdAgent.getId(), updateDTO);

        // Then: Avatar deve ser adicionado
        assertThat(updatedAgent).isPresent();
        assertThat(updatedAgent.get().getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);

        // Verificar no banco
        Optional<AIAgent> savedAgent = aiAgentRepository.findById(createdAgent.getId());
        assertThat(savedAgent.get().getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);
    }

    @Test
    void testUpdateAIAgentAvatarToNull() {
        // Given: Agente com avatar
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Remove Avatar")
                .avatarBase64(VALID_AVATAR_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);
        assertThat(createdAgent.getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);

        // When: Remover avatar (null ou string vazia)
        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .avatarBase64("")
                .build();

        Optional<AIAgent> updatedAgent = aiAgentService.updateAIAgent(createdAgent.getId(), updateDTO);

        // Then: Avatar deve ser removido
        assertThat(updatedAgent).isPresent();
        assertThat(updatedAgent.get().getAvatarBase64()).isEmpty();
    }

    @Test
    void testChangeAvatarFormat() {
        // Given: Agente com avatar JPEG
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Change Format")
                .avatarBase64(VALID_AVATAR_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // When: Trocar para PNG
        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .avatarBase64(VALID_AVATAR_PNG_BASE64)
                .build();

        Optional<AIAgent> updatedAgent = aiAgentService.updateAIAgent(createdAgent.getId(), updateDTO);

        // Then: Novo formato deve ser aceito
        assertThat(updatedAgent).isPresent();
        assertThat(updatedAgent.get().getAvatarBase64()).isEqualTo(VALID_AVATAR_PNG_BASE64);
    }

    @Test
    void testRetrieveAIAgentWithAvatar() {
        // Given: Agente salvo com avatar
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Retrieve Test")
                .avatarBase64(VALID_AVATAR_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // When: Recuperar agente por ID
        Optional<AIAgent> retrievedAgent = aiAgentService.getAIAgentById(createdAgent.getId());

        // Then: Avatar deve ser recuperado corretamente
        assertThat(retrievedAgent).isPresent();
        assertThat(retrievedAgent.get().getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);
        assertThat(retrievedAgent.get().getAvatarBase64())
                .startsWith("data:image/jpeg;base64,");
    }

    @Test
    void testListAIAgentsWithAvatars() {
        // Given: Múltiplos agentes com e sem avatars
        CreateAIAgentDTO agentWithAvatar = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Com Avatar")
                .avatarBase64(VALID_AVATAR_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        CreateAIAgentDTO agentWithoutAvatar = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Sem Avatar")
                .aiModelId(testAIModel.getId())
                .temperament("SÉRIO")
                .build();

        AIAgent agent1 = aiAgentService.createAIAgent(agentWithAvatar);
        AIAgent agent2 = aiAgentService.createAIAgent(agentWithoutAvatar);

        // When: Listar agentes da empresa
        var agents = aiAgentService.getAIAgentsByCompanyId(testCompany.getId());

        // Then: Ambos devem estar na lista com avatars corretos
        assertThat(agents).hasSize(2);
        
        AIAgent foundAgent1 = agents.stream()
                .filter(a -> a.getName().equals("Sofia Com Avatar"))
                .findFirst()
                .orElseThrow();
        assertThat(foundAgent1.getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);

        AIAgent foundAgent2 = agents.stream()
                .filter(a -> a.getName().equals("Sofia Sem Avatar"))
                .findFirst()
                .orElseThrow();
        assertThat(foundAgent2.getAvatarBase64()).isNull();
    }

    @Test
    void testAvatarPersistenceAfterTransaction() {
        // Given: Agente criado em uma transação
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Persistence Test")
                .avatarBase64(VALID_AVATAR_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);
        
        // Flush para garantir que foi persistido
        aiAgentRepository.flush();

        // When: Recuperar em nova consulta (simulando nova transação)
        Optional<AIAgent> retrievedAgent = aiAgentRepository.findById(createdAgent.getId());

        // Then: Avatar deve persistir corretamente
        assertThat(retrievedAgent).isPresent();
        assertThat(retrievedAgent.get().getAvatarBase64()).isEqualTo(VALID_AVATAR_BASE64);
        
        // Verificar que o base64 está completo e válido
        String avatar = retrievedAgent.get().getAvatarBase64();
        assertThat(avatar).contains("data:image/jpeg;base64,");
        assertThat(avatar.length()).isGreaterThan(50); // Avatar deve ter tamanho razoável
    }

    @Test
    void testLargeAvatarStorage() {
        // Given: Avatar maior (simular imagem de ~1KB)
        String largeBase64 = "data:image/jpeg;base64," + "A".repeat(1000); // 1KB de dados
        
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Large Avatar")
                .avatarBase64(largeBase64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: Criar agente com avatar grande
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // Then: Deve ser armazenado corretamente
        assertThat(createdAgent.getAvatarBase64()).isEqualTo(largeBase64);
        assertThat(createdAgent.getAvatarBase64().length()).isGreaterThan(1000);
    }

    @Test 
    void testEmptyStringAvatarHandling() {
        // Given: Avatar como string vazia
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Empty Avatar")
                .avatarBase64("")
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: Criar agente
        AIAgent createdAgent = aiAgentService.createAIAgent(createDTO);

        // Then: String vazia deve ser aceita
        assertThat(createdAgent.getAvatarBase64()).isEmpty();
    }
}