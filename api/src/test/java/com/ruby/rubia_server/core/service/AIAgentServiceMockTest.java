package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.AIModel;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.AIModelRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class AIAgentServiceMockTest {

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AIModelRepository aiModelRepository;

    @InjectMocks
    private AIAgentService aiAgentService;

    private Company company;
    private CompanyGroup companyGroup;
    private AIAgent aiAgent;
    private AIModel testGPT4Model;
    private AIModel testClaudeModel;
    private CreateAIAgentDTO createDTO;
    private UpdateAIAgentDTO updateDTO;
    private UUID companyId;
    private UUID companyGroupId;
    private UUID aiAgentId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        companyGroupId = UUID.randomUUID();
        aiAgentId = UUID.randomUUID();

        // Create test AI models
        testGPT4Model = AIModel.builder()
                .id(UUID.randomUUID())
                .name("gpt-4")
                .displayName("GPT-4")
                .provider("OpenAI")
                .build();

        testClaudeModel = AIModel.builder()
                .id(UUID.randomUUID())
                .name("claude-3.5-sonnet")
                .displayName("Claude 3.5 Sonnet")
                .provider("Anthropic")
                .build();

        companyGroup = CompanyGroup.builder()
                .id(companyGroupId)
                .name("Test Company Group")
                .build();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .maxAiAgents(5) // Permitir múltiplos agentes para teste
                .build();

        createDTO = CreateAIAgentDTO.builder()
                .companyId(companyId)
                .name("Test AI Agent")
                .description("Test Description")
                .avatarBase64("data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD")
                .aiModelId(testGPT4Model.getId())
                .temperament("NORMAL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        updateDTO = UpdateAIAgentDTO.builder()
                .name("Updated AI Agent")
                .description("Updated Description")
                .aiModelId(testClaudeModel.getId()) // Usar um modelo válido
                .temperament("EMPATICO")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(false)
                .build();

        aiAgent = AIAgent.builder()
                .id(aiAgentId)
                .company(company)
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .avatarBase64(createDTO.getAvatarBase64())
                .aiModel(testGPT4Model)
                .temperament(createDTO.getTemperament())
                .maxResponseLength(createDTO.getMaxResponseLength())
                .temperature(createDTO.getTemperature())
                .isActive(createDTO.getIsActive())
                .build();
    }

    @Test
    void createAIAgent_ShouldCreateAndReturnAIAgent_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(aiModelRepository.findById(testGPT4Model.getId())).thenReturn(Optional.of(testGPT4Model));
        when(aiAgentRepository.countByCompanyId(companyId)).thenReturn(0L); // Nenhum agente existente
        when(aiAgentRepository.save(any(AIAgent.class))).thenReturn(aiAgent);
        when(aiAgentRepository.findById(aiAgent.getId())).thenReturn(Optional.of(aiAgent));
        doNothing().when(aiAgentRepository).flush();

        // When
        AIAgent result = aiAgentService.createAIAgent(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(aiAgent.getId(), result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getDescription(), result.getDescription());
        assertEquals(testGPT4Model.getId(), result.getAiModel().getId());
        assertEquals(createDTO.getTemperament(), result.getTemperament());
        assertEquals(company.getId(), result.getCompany().getId());

        verify(companyRepository).findById(companyId);
        verify(aiModelRepository).findById(testGPT4Model.getId());
        verify(aiAgentRepository).countByCompanyId(companyId);
        verify(aiAgentRepository).save(any(AIAgent.class));
        verify(aiAgentRepository).flush();
        verify(aiAgentRepository).findById(aiAgent.getId());
    }

    @Test
    void createAIAgent_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> aiAgentService.createAIAgent(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(aiAgentRepository, never()).save(any(AIAgent.class));
    }

    @Test
    void getAIAgentById_ShouldReturnAIAgent_WhenExists() {
        // Given
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));

        // When
        Optional<AIAgent> result = aiAgentService.getAIAgentById(aiAgentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(aiAgent.getId(), result.get().getId());
        assertEquals(aiAgent.getName(), result.get().getName());
        
        verify(aiAgentRepository).findById(aiAgentId);
    }

    @Test
    void getAIAgentById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When
        Optional<AIAgent> result = aiAgentService.getAIAgentById(aiAgentId);

        // Then
        assertTrue(result.isEmpty());
        verify(aiAgentRepository).findById(aiAgentId);
    }

    @Test
    void getAllAIAgents_ShouldReturnPagedResults() {
        // Given
        List<AIAgent> aiAgents = List.of(aiAgent);
        Page<AIAgent> page = new PageImpl<>(aiAgents);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(aiAgentRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<AIAgent> result = aiAgentService.getAllAIAgents(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(aiAgent.getId(), result.getContent().get(0).getId());
        
        verify(aiAgentRepository).findAll(pageable);
    }

    @Test
    void getAIAgentsByCompanyId_ShouldReturnAgentsForCompany() {
        // Given
        List<AIAgent> aiAgents = List.of(aiAgent);
        when(aiAgentRepository.findByCompanyId(companyId)).thenReturn(aiAgents);

        // When
        List<AIAgent> result = aiAgentService.getAIAgentsByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiAgent.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(aiAgentRepository).findByCompanyId(companyId);
    }

    @Test
    void getActiveAIAgentsByCompanyId_ShouldReturnOnlyActiveAgents() {
        // Given
        List<AIAgent> activeAgents = List.of(aiAgent);
        when(aiAgentRepository.findActiveByCompanyId(companyId)).thenReturn(activeAgents);

        // When
        List<AIAgent> result = aiAgentService.getActiveAIAgentsByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiAgent.getId(), result.get(0).getId());
        
        verify(aiAgentRepository).findActiveByCompanyId(companyId);
    }

    @Test
    void updateAIAgent_ShouldUpdateAndReturnAIAgent_WhenValidData() {
        // Given
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(aiModelRepository.findById(testClaudeModel.getId())).thenReturn(Optional.of(testClaudeModel));
        when(aiAgentRepository.save(any(AIAgent.class))).thenReturn(aiAgent);

        // When
        Optional<AIAgent> result = aiAgentService.updateAIAgent(aiAgentId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        AIAgent updated = result.get();
        assertEquals(aiAgent.getId(), updated.getId());
        
        verify(aiAgentRepository).findById(aiAgentId);
        verify(aiModelRepository).findById(testClaudeModel.getId());
        verify(aiAgentRepository).save(any(AIAgent.class));
    }

    @Test
    void updateAIAgent_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When
        Optional<AIAgent> result = aiAgentService.updateAIAgent(aiAgentId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(aiAgentRepository).findById(aiAgentId);
        verify(aiAgentRepository, never()).save(any(AIAgent.class));
    }

    @Test
    void deleteAIAgent_ShouldReturnTrue_WhenExists() {
        // Given
        when(aiAgentRepository.existsById(aiAgentId)).thenReturn(true);

        // When
        boolean result = aiAgentService.deleteAIAgent(aiAgentId);

        // Then
        assertTrue(result);
        
        verify(aiAgentRepository).existsById(aiAgentId);
        verify(aiAgentRepository).deleteById(aiAgentId);
    }

    @Test
    void deleteAIAgent_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(aiAgentRepository.existsById(aiAgentId)).thenReturn(false);

        // When
        boolean result = aiAgentService.deleteAIAgent(aiAgentId);

        // Then
        assertFalse(result);
        
        verify(aiAgentRepository).existsById(aiAgentId);
        verify(aiAgentRepository, never()).deleteById(aiAgentId);
    }

    @Test
    void countAIAgentsByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(aiAgentRepository.countByCompanyId(companyId)).thenReturn(2L);

        // When
        long count = aiAgentService.countAIAgentsByCompanyId(companyId);

        // Then
        assertEquals(2L, count);
        verify(aiAgentRepository).countByCompanyId(companyId);
    }

    @Test
    void existsByNameAndCompanyId_ShouldReturnTrue_WhenExists() {
        // Given
        String agentName = "Test Agent";
        when(aiAgentRepository.existsByNameAndCompanyId(agentName, companyId)).thenReturn(true);

        // When
        boolean exists = aiAgentService.existsByNameAndCompanyId(agentName, companyId);

        // Then
        assertTrue(exists);
        verify(aiAgentRepository).existsByNameAndCompanyId(agentName, companyId);
    }

    @Test
    void existsByNameAndCompanyId_ShouldReturnFalse_WhenNotExists() {
        // Given
        String agentName = "Non-existent Agent";
        when(aiAgentRepository.existsByNameAndCompanyId(agentName, companyId)).thenReturn(false);

        // When
        boolean exists = aiAgentService.existsByNameAndCompanyId(agentName, companyId);

        // Then
        assertFalse(exists);
        verify(aiAgentRepository).existsByNameAndCompanyId(agentName, companyId);
    }

    @Test
    void getAIAgentsByModelType_ShouldReturnCorrectAgents() {
        // Given
        UUID modelId = testGPT4Model.getId();
        List<AIAgent> agents = List.of(aiAgent);
        when(aiAgentRepository.findByAiModelId(modelId)).thenReturn(agents);

        // When
        List<AIAgent> result = aiAgentService.getAIAgentsByModelId(modelId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiAgent.getId(), result.get(0).getId());
        verify(aiAgentRepository).findByAiModelId(modelId);
    }

    @Test
    void getAIAgentsByTemperament_ShouldReturnCorrectAgents() {
        // Given
        String temperament = "NORMAL";
        List<AIAgent> agents = List.of(aiAgent);
        when(aiAgentRepository.findByTemperament(temperament)).thenReturn(agents);

        // When
        List<AIAgent> result = aiAgentService.getAIAgentsByTemperament(temperament);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiAgent.getId(), result.get(0).getId());
        verify(aiAgentRepository).findByTemperament(temperament);
    }
}