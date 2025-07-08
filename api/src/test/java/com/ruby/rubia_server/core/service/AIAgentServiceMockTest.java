package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
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

@ExtendWith(MockitoExtension.class)
class AIAgentServiceMockTest {

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private AIAgentService aiAgentService;

    private Company company;
    private CompanyGroup companyGroup;
    private AIAgent aiAgent;
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

        companyGroup = CompanyGroup.builder()
                .id(companyGroupId)
                .name("Test Company Group")
                .build();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .build();

        createDTO = CreateAIAgentDTO.builder()
                .companyId(companyId)
                .name("Test AI Agent")
                .description("Test Description")
                .avatarUrl("https://example.com/avatar.jpg")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();

        updateDTO = UpdateAIAgentDTO.builder()
                .name("Updated AI Agent")
                .description("Updated Description")
                .aiModelType("Claude-3.5")
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
                .avatarUrl(createDTO.getAvatarUrl())
                .aiModelType(createDTO.getAiModelType())
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
        when(aiAgentRepository.save(any(AIAgent.class))).thenReturn(aiAgent);

        // When
        AIAgent result = aiAgentService.createAIAgent(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(aiAgent.getId(), result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getDescription(), result.getDescription());
        assertEquals(createDTO.getAiModelType(), result.getAiModelType());
        assertEquals(createDTO.getTemperament(), result.getTemperament());
        assertEquals(company.getId(), result.getCompany().getId());

        verify(companyRepository).findById(companyId);
        verify(aiAgentRepository).save(any(AIAgent.class));
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
        when(aiAgentRepository.save(any(AIAgent.class))).thenReturn(aiAgent);

        // When
        Optional<AIAgent> result = aiAgentService.updateAIAgent(aiAgentId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        AIAgent updated = result.get();
        assertEquals(aiAgent.getId(), updated.getId());
        
        verify(aiAgentRepository).findById(aiAgentId);
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
        String modelType = "GPT-4";
        List<AIAgent> agents = List.of(aiAgent);
        when(aiAgentRepository.findByAiModelType(modelType)).thenReturn(agents);

        // When
        List<AIAgent> result = aiAgentService.getAIAgentsByModelType(modelType);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiAgent.getId(), result.get(0).getId());
        verify(aiAgentRepository).findByAiModelType(modelType);
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