package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateAILogDTO;
import com.ruby.rubia_server.core.dto.UpdateAILogDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.AILogStatus;
import com.ruby.rubia_server.core.repository.AILogRepository;
import com.ruby.rubia_server.core.repository.AIAgentRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.ConversationRepository;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.repository.MessageTemplateRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AILogServiceMockTest {

    @Mock
    private AILogRepository aiLogRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private MessageTemplateRepository messageTemplateRepository;

    @InjectMocks
    private AILogService aiLogService;

    private Company company;
    private AIAgent aiAgent;
    private User user;
    private Conversation conversation;
    private Message message;
    private MessageTemplate messageTemplate;
    private AILog aiLog;
    private CreateAILogDTO createDTO;
    private UpdateAILogDTO updateDTO;
    private UUID companyId;
    private UUID aiAgentId;
    private UUID userId;
    private UUID conversationId;
    private UUID messageId;
    private UUID messageTemplateId;
    private UUID aiLogId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        aiAgentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        messageTemplateId = UUID.randomUUID();
        aiLogId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        aiAgent = AIAgent.builder()
                .id(aiAgentId)
                .company(company)
                .name("Test AI Agent")
                .build();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .email("test@example.com")
                .company(company)
                .build();

        conversation = Conversation.builder()
                .id(conversationId)
                .company(company)
                .build();

        message = Message.builder()
                .id(messageId)
                .conversation(conversation)
                .content("Test message")
                .build();

        messageTemplate = MessageTemplate.builder()
                .id(messageTemplateId)
                .company(company)
                .name("Test Template")
                .build();

        createDTO = CreateAILogDTO.builder()
                .companyId(companyId)
                .aiAgentId(aiAgentId)
                .userId(userId)
                .conversationId(conversationId)
                .messageId(messageId)
                .messageTemplateId(messageTemplateId)
                .requestPrompt("Test prompt")
                .rawResponse("Test raw response")
                .processedResponse("Test processed response")
                .tokensUsedInput(100)
                .tokensUsedOutput(150)
                .estimatedCost(BigDecimal.valueOf(0.00025))
                .status(AILogStatus.SUCCESS)
                .build();

        updateDTO = UpdateAILogDTO.builder()
                .rawResponse("Updated raw response")
                .processedResponse("Updated processed response")
                .tokensUsedOutput(200)
                .estimatedCost(BigDecimal.valueOf(0.0003))
                .status(AILogStatus.SUCCESS)
                .build();

        aiLog = AILog.builder()
                .id(aiLogId)
                .company(company)
                .aiAgent(aiAgent)
                .user(user)
                .conversation(conversation)
                .message(message)
                .messageTemplate(messageTemplate)
                .requestPrompt(createDTO.getRequestPrompt())
                .rawResponse(createDTO.getRawResponse())
                .processedResponse(createDTO.getProcessedResponse())
                .tokensUsedInput(createDTO.getTokensUsedInput())
                .tokensUsedOutput(createDTO.getTokensUsedOutput())
                .estimatedCost(createDTO.getEstimatedCost())
                .status(createDTO.getStatus())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createAILog_ShouldCreateAndReturnAILog_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageTemplateRepository.findById(messageTemplateId)).thenReturn(Optional.of(messageTemplate));
        when(aiLogRepository.save(any(AILog.class))).thenReturn(aiLog);

        // When
        AILog result = aiLogService.createAILog(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(aiLog.getId(), result.getId());
        assertEquals(createDTO.getRequestPrompt(), result.getRequestPrompt());
        assertEquals(createDTO.getRawResponse(), result.getRawResponse());
        assertEquals(createDTO.getStatus(), result.getStatus());
        assertEquals(company.getId(), result.getCompany().getId());
        assertEquals(aiAgent.getId(), result.getAiAgent().getId());

        verify(companyRepository).findById(companyId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(aiLogRepository).save(any(AILog.class));
    }

    @Test
    void createAILog_ShouldCreateWithoutOptionalEntities_WhenOnlyRequiredDataProvided() {
        // Given
        CreateAILogDTO minimalDTO = CreateAILogDTO.builder()
                .companyId(companyId)
                .aiAgentId(aiAgentId)
                .requestPrompt("Test prompt")
                .status(AILogStatus.SUCCESS)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(aiLogRepository.save(any(AILog.class))).thenReturn(aiLog);

        // When
        AILog result = aiLogService.createAILog(minimalDTO);

        // Then
        assertNotNull(result);
        verify(companyRepository).findById(companyId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(userRepository, never()).findById(any());
        verify(conversationRepository, never()).findById(any());
        verify(messageRepository, never()).findById(any());
        verify(messageTemplateRepository, never()).findById(any());
        verify(aiLogRepository).save(any(AILog.class));
    }

    @Test
    void createAILog_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> aiLogService.createAILog(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(aiLogRepository, never()).save(any(AILog.class));
    }

    @Test
    void createAILog_ShouldThrowException_WhenAIAgentNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> aiLogService.createAILog(createDTO));
        
        assertEquals("AI Agent not found with ID: " + aiAgentId, exception.getMessage());
        verify(aiAgentRepository).findById(aiAgentId);
        verify(aiLogRepository, never()).save(any(AILog.class));
    }

    @Test
    void getAILogById_ShouldReturnAILog_WhenExists() {
        // Given
        when(aiLogRepository.findById(aiLogId)).thenReturn(Optional.of(aiLog));

        // When
        Optional<AILog> result = aiLogService.getAILogById(aiLogId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(aiLog.getId(), result.get().getId());
        assertEquals(aiLog.getRequestPrompt(), result.get().getRequestPrompt());
        
        verify(aiLogRepository).findById(aiLogId);
    }

    @Test
    void getAILogById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(aiLogRepository.findById(aiLogId)).thenReturn(Optional.empty());

        // When
        Optional<AILog> result = aiLogService.getAILogById(aiLogId);

        // Then
        assertTrue(result.isEmpty());
        verify(aiLogRepository).findById(aiLogId);
    }

    @Test
    void getAllAILogs_ShouldReturnPagedResults() {
        // Given
        List<AILog> aiLogs = List.of(aiLog);
        Page<AILog> page = new PageImpl<>(aiLogs);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(aiLogRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<AILog> result = aiLogService.getAllAILogs(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(aiLog.getId(), result.getContent().get(0).getId());
        
        verify(aiLogRepository).findAll(pageable);
    }

    @Test
    void getAILogsByCompanyId_ShouldReturnLogsForCompany() {
        // Given
        List<AILog> aiLogs = List.of(aiLog);
        when(aiLogRepository.findByCompanyId(companyId)).thenReturn(aiLogs);

        // When
        List<AILog> result = aiLogService.getAILogsByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiLog.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(aiLogRepository).findByCompanyId(companyId);
    }

    @Test
    void getAILogsByStatus_ShouldReturnLogsWithSpecificStatus() {
        // Given
        List<AILog> successLogs = List.of(aiLog);
        when(aiLogRepository.findByStatus(AILogStatus.SUCCESS)).thenReturn(successLogs);

        // When
        List<AILog> result = aiLogService.getAILogsByStatus(AILogStatus.SUCCESS);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiLog.getId(), result.get(0).getId());
        assertEquals(AILogStatus.SUCCESS, result.get(0).getStatus());
        
        verify(aiLogRepository).findByStatus(AILogStatus.SUCCESS);
    }

    @Test
    void getAILogsByAIAgentId_ShouldReturnLogsForAgent() {
        // Given
        List<AILog> agentLogs = List.of(aiLog);
        when(aiLogRepository.findByAiAgentId(aiAgentId)).thenReturn(agentLogs);

        // When
        List<AILog> result = aiLogService.getAILogsByAIAgentId(aiAgentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiLog.getId(), result.get(0).getId());
        assertEquals(aiAgentId, result.get(0).getAiAgent().getId());
        
        verify(aiLogRepository).findByAiAgentId(aiAgentId);
    }

    @Test
    void updateAILog_ShouldUpdateAndReturnAILog_WhenValidData() {
        // Given
        when(aiLogRepository.findById(aiLogId)).thenReturn(Optional.of(aiLog));
        when(aiLogRepository.save(any(AILog.class))).thenReturn(aiLog);

        // When
        Optional<AILog> result = aiLogService.updateAILog(aiLogId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        AILog updated = result.get();
        assertEquals(aiLog.getId(), updated.getId());
        
        verify(aiLogRepository).findById(aiLogId);
        verify(aiLogRepository).save(any(AILog.class));
    }

    @Test
    void updateAILog_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(aiLogRepository.findById(aiLogId)).thenReturn(Optional.empty());

        // When
        Optional<AILog> result = aiLogService.updateAILog(aiLogId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(aiLogRepository).findById(aiLogId);
        verify(aiLogRepository, never()).save(any(AILog.class));
    }

    @Test
    void deleteAILog_ShouldReturnTrue_WhenExists() {
        // Given
        when(aiLogRepository.existsById(aiLogId)).thenReturn(true);

        // When
        boolean result = aiLogService.deleteAILog(aiLogId);

        // Then
        assertTrue(result);
        
        verify(aiLogRepository).existsById(aiLogId);
        verify(aiLogRepository).deleteById(aiLogId);
    }

    @Test
    void deleteAILog_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(aiLogRepository.existsById(aiLogId)).thenReturn(false);

        // When
        boolean result = aiLogService.deleteAILog(aiLogId);

        // Then
        assertFalse(result);
        
        verify(aiLogRepository).existsById(aiLogId);
        verify(aiLogRepository, never()).deleteById(aiLogId);
    }

    @Test
    void getTotalCostByCompanyId_ShouldReturnCorrectSum() {
        // Given
        BigDecimal totalCost = BigDecimal.valueOf(1.25);
        when(aiLogRepository.sumEstimatedCostByCompanyId(companyId)).thenReturn(totalCost);

        // When
        BigDecimal result = aiLogService.getTotalCostByCompanyId(companyId);

        // Then
        assertEquals(totalCost, result);
        verify(aiLogRepository).sumEstimatedCostByCompanyId(companyId);
    }

    @Test
    void getTotalTokensUsedByCompanyId_ShouldReturnCorrectSum() {
        // Given
        Long totalTokens = 1500L;
        when(aiLogRepository.sumTokensUsedByCompanyId(companyId)).thenReturn(totalTokens);

        // When
        Long result = aiLogService.getTotalTokensUsedByCompanyId(companyId);

        // Then
        assertEquals(totalTokens, result);
        verify(aiLogRepository).sumTokensUsedByCompanyId(companyId);
    }

    @Test
    void countAILogsByCompanyIdAndStatus_ShouldReturnCorrectCount() {
        // Given
        long count = 5L;
        when(aiLogRepository.countByCompanyIdAndStatus(companyId, AILogStatus.SUCCESS)).thenReturn(count);

        // When
        long result = aiLogService.countAILogsByCompanyIdAndStatus(companyId, AILogStatus.SUCCESS);

        // Then
        assertEquals(count, result);
        verify(aiLogRepository).countByCompanyIdAndStatus(companyId, AILogStatus.SUCCESS);
    }

    @Test
    void getAILogsByDateRange_ShouldReturnLogsInRange() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<AILog> logs = List.of(aiLog);
        
        when(aiLogRepository.findByCreatedAtBetween(startDate, endDate)).thenReturn(logs);

        // When
        List<AILog> result = aiLogService.getAILogsByDateRange(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(aiLog.getId(), result.get(0).getId());
        
        verify(aiLogRepository).findByCreatedAtBetween(startDate, endDate);
    }
}