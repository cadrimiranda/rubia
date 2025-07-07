package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.CreateAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateAIAgentDTO;
import com.ruby.rubia_server.core.entity.AIAgent;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AIAgentServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AIAgentService aiAgentService;

    @Autowired
    private AIAgentRepository aiAgentRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    private Company company;
    private CreateAIAgentDTO createDTO;
    private UpdateAIAgentDTO updateDTO;

    @BeforeEach
    void setUp() {
        // Clean database
        aiAgentRepository.deleteAll();
        companyRepository.deleteAll();
        companyGroupRepository.deleteAll();

        // Create test company group and company
        CompanyGroup companyGroup = CompanyGroup.builder()
                .name("Test Company Group")
                .description("Test Description")
                .isActive(true)
                .build();
        companyGroup = companyGroupRepository.save(companyGroup);

        company = Company.builder()
                .name("Test Company")
                .slug("test-company")
                .description("Test Description")
                .isActive(true)
                .companyGroup(companyGroup)
                .build();
        company = companyRepository.save(company);

        // Create DTOs
        createDTO = CreateAIAgentDTO.builder()
                .companyId(company.getId())
                .name("Test AI Agent")
                .description("Test AI Agent Description")
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
                .avatarUrl("https://example.com/updated-avatar.jpg")
                .aiModelType("Claude-3.5")
                .temperament("EMPATICO")
                .maxResponseLength(800)
                .temperature(BigDecimal.valueOf(0.5))
                .isActive(false)
                .build();
    }

    @Test
    void createAIAgent_ShouldCreateAndReturnAIAgent_WhenValidData() {
        // When
        AIAgent result = aiAgentService.createAIAgent(createDTO);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(createDTO.getName(), result.getName());
        assertEquals(createDTO.getDescription(), result.getDescription());
        assertEquals(createDTO.getAvatarUrl(), result.getAvatarUrl());
        assertEquals(createDTO.getAiModelType(), result.getAiModelType());
        assertEquals(createDTO.getTemperament(), result.getTemperament());
        assertEquals(createDTO.getMaxResponseLength(), result.getMaxResponseLength());
        assertEquals(createDTO.getTemperature(), result.getTemperature());
        assertEquals(createDTO.getIsActive(), result.getIsActive());
        assertEquals(company.getId(), result.getCompany().getId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        // Verify it was saved to database
        Optional<AIAgent> saved = aiAgentRepository.findById(result.getId());
        assertTrue(saved.isPresent());
        assertEquals(result.getName(), saved.get().getName());
    }

    @Test
    void createAIAgent_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        CreateAIAgentDTO invalidDTO = CreateAIAgentDTO.builder()
                .companyId(UUID.randomUUID()) // Non-existent company
                .name("Test AI Agent")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .build();

        // When & Then
        assertThrows(RuntimeException.class, () -> aiAgentService.createAIAgent(invalidDTO));
    }

    @Test
    void getAIAgentById_ShouldReturnAIAgent_WhenExists() {
        // Given
        AIAgent aiAgent = AIAgent.builder()
                .company(company)
                .name("Test AI Agent")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        aiAgent = aiAgentRepository.save(aiAgent);

        // When
        Optional<AIAgent> result = aiAgentService.getAIAgentById(aiAgent.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(aiAgent.getId(), result.get().getId());
        assertEquals(aiAgent.getName(), result.get().getName());
    }

    @Test
    void getAIAgentById_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<AIAgent> result = aiAgentService.getAIAgentById(UUID.randomUUID());

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void getAllAIAgents_ShouldReturnPagedResults() {
        // Given
        AIAgent agent1 = AIAgent.builder()
                .company(company)
                .name("AI Agent 1")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        AIAgent agent2 = AIAgent.builder()
                .company(company)
                .name("AI Agent 2")
                .aiModelType("Claude-3.5")
                .temperament("EMPATICO")
                .isActive(true)
                .build();
        aiAgentRepository.saveAll(List.of(agent1, agent2));

        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<AIAgent> result = aiAgentService.getAllAIAgents(pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    @Test
    void getAIAgentsByCompanyId_ShouldReturnAgentsForCompany() {
        // Given
        AIAgent agent1 = AIAgent.builder()
                .company(company)
                .name("AI Agent 1")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        aiAgentRepository.save(agent1);

        // Create another company with different agent
        Company anotherCompany = Company.builder()
                .name("Another Company")
                .slug("another-company")
                .isActive(true)
                .companyGroup(company.getCompanyGroup())
                .build();
        anotherCompany = companyRepository.save(anotherCompany);

        AIAgent agent2 = AIAgent.builder()
                .company(anotherCompany)
                .name("AI Agent 2")
                .aiModelType("Claude-3.5")
                .temperament("EMPATICO")
                .isActive(true)
                .build();
        aiAgentRepository.save(agent2);

        // When
        List<AIAgent> result = aiAgentService.getAIAgentsByCompanyId(company.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(agent1.getId(), result.get(0).getId());
        assertEquals(company.getId(), result.get(0).getCompany().getId());
    }

    @Test
    void getActiveAIAgentsByCompanyId_ShouldReturnOnlyActiveAgents() {
        // Given
        AIAgent activeAgent = AIAgent.builder()
                .company(company)
                .name("Active AI Agent")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        AIAgent inactiveAgent = AIAgent.builder()
                .company(company)
                .name("Inactive AI Agent")
                .aiModelType("Claude-3.5")
                .temperament("EMPATICO")
                .isActive(false)
                .build();
        aiAgentRepository.saveAll(List.of(activeAgent, inactiveAgent));

        // When
        List<AIAgent> result = aiAgentService.getActiveAIAgentsByCompanyId(company.getId());

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(activeAgent.getId(), result.get(0).getId());
        assertTrue(result.get(0).getIsActive());
    }

    @Test
    void updateAIAgent_ShouldUpdateAndReturnAIAgent_WhenValidData() {
        // Given
        AIAgent aiAgent = AIAgent.builder()
                .company(company)
                .name("Original Name")
                .description("Original Description")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .maxResponseLength(500)
                .temperature(BigDecimal.valueOf(0.7))
                .isActive(true)
                .build();
        aiAgent = aiAgentRepository.save(aiAgent);

        // When
        Optional<AIAgent> result = aiAgentService.updateAIAgent(aiAgent.getId(), updateDTO);

        // Then
        assertTrue(result.isPresent());
        AIAgent updated = result.get();
        assertEquals(updateDTO.getName(), updated.getName());
        assertEquals(updateDTO.getDescription(), updated.getDescription());
        assertEquals(updateDTO.getAvatarUrl(), updated.getAvatarUrl());
        assertEquals(updateDTO.getAiModelType(), updated.getAiModelType());
        assertEquals(updateDTO.getTemperament(), updated.getTemperament());
        assertEquals(updateDTO.getMaxResponseLength(), updated.getMaxResponseLength());
        assertEquals(updateDTO.getTemperature(), updated.getTemperature());
        assertEquals(updateDTO.getIsActive(), updated.getIsActive());

        // Verify it was updated in database
        Optional<AIAgent> savedUpdated = aiAgentRepository.findById(aiAgent.getId());
        assertTrue(savedUpdated.isPresent());
        assertEquals(updateDTO.getName(), savedUpdated.get().getName());
    }

    @Test
    void updateAIAgent_ShouldReturnEmpty_WhenNotExists() {
        // When
        Optional<AIAgent> result = aiAgentService.updateAIAgent(UUID.randomUUID(), updateDTO);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteAIAgent_ShouldReturnTrue_WhenExists() {
        // Given
        AIAgent aiAgent = AIAgent.builder()
                .company(company)
                .name("Test AI Agent")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        aiAgent = aiAgentRepository.save(aiAgent);

        // When
        boolean result = aiAgentService.deleteAIAgent(aiAgent.getId());

        // Then
        assertTrue(result);
        
        // Verify it was deleted from database
        Optional<AIAgent> deleted = aiAgentRepository.findById(aiAgent.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    void deleteAIAgent_ShouldReturnFalse_WhenNotExists() {
        // When
        boolean result = aiAgentService.deleteAIAgent(UUID.randomUUID());

        // Then
        assertFalse(result);
    }

    @Test
    void countAIAgentsByCompanyId_ShouldReturnCorrectCount() {
        // Given
        AIAgent agent1 = AIAgent.builder()
                .company(company)
                .name("AI Agent 1")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        AIAgent agent2 = AIAgent.builder()
                .company(company)
                .name("AI Agent 2")
                .aiModelType("Claude-3.5")
                .temperament("EMPATICO")
                .isActive(true)
                .build();
        aiAgentRepository.saveAll(List.of(agent1, agent2));

        // When
        long count = aiAgentService.countAIAgentsByCompanyId(company.getId());

        // Then
        assertEquals(2, count);
    }

    @Test
    void existsByNameAndCompanyId_ShouldReturnTrue_WhenExists() {
        // Given
        AIAgent aiAgent = AIAgent.builder()
                .company(company)
                .name("Unique AI Agent")
                .aiModelType("GPT-4")
                .temperament("NORMAL")
                .isActive(true)
                .build();
        aiAgentRepository.save(aiAgent);

        // When
        boolean exists = aiAgentService.existsByNameAndCompanyId("Unique AI Agent", company.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByNameAndCompanyId_ShouldReturnFalse_WhenNotExists() {
        // When
        boolean exists = aiAgentService.existsByNameAndCompanyId("Non-existent AI Agent", company.getId());

        // Then
        assertFalse(exists);
    }
}