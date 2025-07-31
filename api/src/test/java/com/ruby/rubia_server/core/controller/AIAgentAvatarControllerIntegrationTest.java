package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de integração do controller para validação de avatars base64.
 * 
 * Testa:
 * - Validações de formato base64
 * - Criação de agente com avatar
 * - Atualização de avatar
 * - Retorno correto do avatar na API
 * - Códigos de erro para formatos inválidos
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AIAgentAvatarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private AIModelRepository aiModelRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    private Company testCompany;
    private AIModel testAIModel;
    private User testUser;
    private Department testDepartment;

    // Avatars de teste
    private static final String VALID_JPEG_AVATAR = 
        "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwA/8A/9k=";

    private static final String VALID_PNG_AVATAR = 
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";

    private static final String VALID_GIF_AVATAR = 
        "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

    private static final String INVALID_FORMAT_AVATAR = "data:text/plain;base64,dGVzdA==";
    private static final String MALFORMED_BASE64 = "not-base64-at-all";
    private static final String MISSING_HEADER = "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAE=";

    @BeforeEach
    void setUp() {
        // Criar entities de teste
        testDepartment = Department.builder()
                .name("TI")
                .description("Departamento de TI")
                .build();
        testDepartment = departmentRepository.save(testDepartment);

        testCompany = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .build();
        testCompany = companyRepository.save(testCompany);

        testAIModel = AIModel.builder()
                .name("gpt-4o-mini")
                .displayName("GPT-4o Mini")
                .provider("OpenAI")
                .capabilities("Modelo de teste")
                .costPer1kTokens(1)
                .performanceLevel("HIGH")
                .isActive(true)
                .sortOrder(1)
                .build();
        testAIModel = aiModelRepository.save(testAIModel);

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
    void testCreateAIAgentWithValidJPEGAvatar() throws Exception {
        // Given: DTO com avatar JPEG válido
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia JPEG Test")
                .description("Teste com avatar JPEG")
                .avatarBase64(VALID_JPEG_AVATAR)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: Deve ser criado com sucesso
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Sofia JPEG Test"))
                .andExpect(jsonPath("$.avatarBase64").value(VALID_JPEG_AVATAR))
                .andExpect(jsonPath("$.aiModelDisplayName").value("GPT-4o Mini"))
                .andExpect(jsonPath("$.temperament").value("AMIGAVEL"));
    }

    @Test
    void testCreateAIAgentWithValidPNGAvatar() throws Exception {
        // Given: DTO com avatar PNG válido
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia PNG Test")
                .avatarBase64(VALID_PNG_AVATAR)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: PNG deve ser aceito
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatarBase64").value(VALID_PNG_AVATAR));
    }

    @Test
    void testCreateAIAgentWithValidGIFAvatar() throws Exception {
        // Given: DTO com avatar GIF válido
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia GIF Test")
                .avatarBase64(VALID_GIF_AVATAR)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: GIF deve ser aceito
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatarBase64").value(VALID_GIF_AVATAR));
    }

    @Test
    void testCreateAIAgentWithInvalidFormatAvatar() throws Exception {
        // Given: DTO com formato de arquivo não suportado
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Invalid Format")
                .avatarBase64(INVALID_FORMAT_AVATAR) // text/plain não é suportado
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: Deve retornar erro de validação
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCreateAIAgentWithMalformedBase64() throws Exception {
        // Given: DTO com base64 malformado
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Malformed")
                .avatarBase64(MALFORMED_BASE64)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: Deve retornar erro de validação
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCreateAIAgentWithMissingDataHeader() throws Exception {
        // Given: DTO com base64 sem header data:image
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Missing Header")
                .avatarBase64(MISSING_HEADER)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: Deve retornar erro de validação
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCreateAIAgentWithEmptyAvatar() throws Exception {
        // Given: DTO com avatar vazio (permitido)
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Empty Avatar")
                .avatarBase64("")
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: String vazia deve ser aceita
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatarBase64").value(""));
    }

    @Test
    void testCreateAIAgentWithNullAvatar() throws Exception {
        // Given: DTO sem avatar
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia No Avatar")
                .avatarBase64(null)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        // When: POST /api/ai-agents
        mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                // Then: Null deve ser aceito
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.avatarBase64").doesNotExist());
    }

    @Test
    void testUpdateAIAgentAvatar() throws Exception {
        // Given: Agente existente
        AIAgent existingAgent = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia Update Test")
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        existingAgent = aiAgentRepository.save(existingAgent);

        // When: Atualizar com avatar
        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .avatarBase64(VALID_JPEG_AVATAR)
                .build();

        mockMvc.perform(put("/api/ai-agents/{id}", existingAgent.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                // Then: Avatar deve ser atualizado
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingAgent.getId().toString()))
                .andExpect(jsonPath("$.avatarBase64").value(VALID_JPEG_AVATAR));
    }

    @Test
    void testUpdateAIAgentWithInvalidAvatar() throws Exception {
        // Given: Agente existente
        AIAgent existingAgent = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia Invalid Update")
                .avatarBase64(VALID_JPEG_AVATAR)
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        existingAgent = aiAgentRepository.save(existingAgent);

        // When: Tentar atualizar com avatar inválido
        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .avatarBase64(INVALID_FORMAT_AVATAR)
                .build();

        mockMvc.perform(put("/api/ai-agents/{id}", existingAgent.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                // Then: Deve retornar erro de validação
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testRemoveAIAgentAvatar() throws Exception {
        // Given: Agente com avatar
        AIAgent existingAgent = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia Remove Avatar")
                .avatarBase64(VALID_JPEG_AVATAR)
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        existingAgent = aiAgentRepository.save(existingAgent);

        // When: Remover avatar (string vazia)
        UpdateAIAgentDTO updateDTO = UpdateAIAgentDTO.builder()
                .avatarBase64("")
                .build();

        mockMvc.perform(put("/api/ai-agents/{id}", existingAgent.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                // Then: Avatar deve ser removido
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarBase64").value(""));
    }

    @Test
    void testGetAIAgentWithAvatar() throws Exception {
        // Given: Agente salvo com avatar
        AIAgent agentWithAvatar = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia Get Test")
                .avatarBase64(VALID_PNG_AVATAR)
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        agentWithAvatar = aiAgentRepository.save(agentWithAvatar);

        // When: GET /api/ai-agents/{id}
        mockMvc.perform(get("/api/ai-agents/{id}", agentWithAvatar.getId()))
                // Then: Avatar deve ser retornado
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agentWithAvatar.getId().toString()))
                .andExpect(jsonPath("$.name").value("Sofia Get Test"))
                .andExpect(jsonPath("$.avatarBase64").value(VALID_PNG_AVATAR))
                .andExpect(jsonPath("$.avatarBase64").value(startsWith("data:image/png;base64,")));
    }

    @Test
    void testListAIAgentsWithAvatars() throws Exception {
        // Given: Múltiplos agentes com diferentes tipos de avatar
        AIAgent agent1 = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia JPEG")
                .avatarBase64(VALID_JPEG_AVATAR)
                .temperament("AMIGAVEL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        AIAgent agent2 = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia PNG")
                .avatarBase64(VALID_PNG_AVATAR)
                .temperament("SÉRIO")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        AIAgent agent3 = AIAgent.builder()
                .company(testCompany)
                .aiModel(testAIModel)
                .name("Sofia No Avatar")
                .temperament("EMPATICO")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        aiAgentRepository.save(agent1);
        aiAgentRepository.save(agent2);
        aiAgentRepository.save(agent3);

        // When: GET /api/ai-agents/company/{companyId}
        mockMvc.perform(get("/api/ai-agents/company/{companyId}", testCompany.getId()))
                // Then: Todos os agentes devem ser retornados com avatars corretos
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.name=='Sofia JPEG')].avatarBase64").value(hasItem(VALID_JPEG_AVATAR)))
                .andExpect(jsonPath("$[?(@.name=='Sofia PNG')].avatarBase64").value(hasItem(VALID_PNG_AVATAR)))
                .andExpect(jsonPath("$[?(@.name=='Sofia No Avatar')].avatarBase64").value(hasItem(nullValue())));
    }

    @Test
    void testAvatarDataIntegrityAfterMultipleOperations() throws Exception {
        // Given: Agente criado
        CreateAIAgentDTO createDTO = CreateAIAgentDTO.builder()
                .companyId(testCompany.getId())
                .name("Sofia Integrity Test")
                .avatarBase64(VALID_JPEG_AVATAR)
                .aiModelId(testAIModel.getId())
                .temperament("AMIGAVEL")
                .build();

        String response = mockMvc.perform(post("/api/ai-agents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String agentId = objectMapper.readTree(response).get("id").asText();

        // When: Múltiplas operações (update para PNG, depois GIF, depois remover)
        UpdateAIAgentDTO updateToPNG = UpdateAIAgentDTO.builder()
                .avatarBase64(VALID_PNG_AVATAR)
                .build();

        mockMvc.perform(put("/api/ai-agents/{id}", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateToPNG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarBase64").value(VALID_PNG_AVATAR));

        UpdateAIAgentDTO updateToGIF = UpdateAIAgentDTO.builder()
                .avatarBase64(VALID_GIF_AVATAR)
                .build();

        mockMvc.perform(put("/api/ai-agents/{id}", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateToGIF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarBase64").value(VALID_GIF_AVATAR));

        UpdateAIAgentDTO removeAvatar = UpdateAIAgentDTO.builder()
                .avatarBase64("")
                .build();

        mockMvc.perform(put("/api/ai-agents/{id}", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(removeAvatar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarBase64").value(""));

        // Then: Estado final deve ser correto
        mockMvc.perform(get("/api/ai-agents/{id}", agentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarBase64").value(""))
                .andExpect(jsonPath("$.name").value("Sofia Integrity Test"));
    }
}