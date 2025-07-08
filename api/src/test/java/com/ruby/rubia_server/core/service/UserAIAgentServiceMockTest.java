package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateUserAIAgentDTO;
import com.ruby.rubia_server.core.dto.UpdateUserAIAgentDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAIAgentServiceMockTest {

    @Mock
    private UserAIAgentRepository userAIAgentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AIAgentRepository aiAgentRepository;

    @Mock
    private EntityRelationshipValidator relationshipValidator;

    @InjectMocks
    private UserAIAgentService userAIAgentService;

    private Company company;
    private User user;
    private AIAgent aiAgent;
    private UserAIAgent userAIAgent;
    private CreateUserAIAgentDTO createDTO;
    private UpdateUserAIAgentDTO updateDTO;
    private UUID companyId;
    private UUID userId;
    private UUID aiAgentId;
    private UUID userAIAgentId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        userId = UUID.randomUUID();
        aiAgentId = UUID.randomUUID();
        userAIAgentId = UUID.randomUUID();

        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();

        user = User.builder()
                .id(userId)
                .name("Test User")
                .build();

        aiAgent = AIAgent.builder()
                .id(aiAgentId)
                .name("Test AI Agent")
                .build();

        createDTO = CreateUserAIAgentDTO.builder()
                .companyId(companyId)
                .userId(userId)
                .aiAgentId(aiAgentId)
                .isDefault(false)
                .build();

        updateDTO = UpdateUserAIAgentDTO.builder()
                .isDefault(true)
                .build();

        userAIAgent = UserAIAgent.builder()
                .id(userAIAgentId)
                .company(company)
                .user(user)
                .aiAgent(aiAgent)
                .isDefault(createDTO.getIsDefault())
                .assignedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createUserAIAgent_ShouldCreateAndReturnUserAIAgent_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(userAIAgentRepository.existsByUserIdAndAiAgentId(userId, aiAgentId)).thenReturn(false);
        when(userAIAgentRepository.save(any(UserAIAgent.class))).thenReturn(userAIAgent);

        // When
        UserAIAgent result = userAIAgentService.create(createDTO);

        // Then
        assertNotNull(result);
        assertEquals(userAIAgent.getId(), result.getId());
        assertEquals(companyId, result.getCompany().getId());
        assertEquals(userId, result.getUser().getId());
        assertEquals(aiAgentId, result.getAiAgent().getId());
        assertEquals(createDTO.getIsDefault(), result.getIsDefault());

        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(userAIAgentRepository).existsByUserIdAndAiAgentId(userId, aiAgentId);
        verify(userAIAgentRepository).save(any(UserAIAgent.class));
    }

    @Test
    void createUserAIAgent_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userAIAgentService.create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(userRepository, never()).findById(userId);
        verify(aiAgentRepository, never()).findById(aiAgentId);
        verify(userAIAgentRepository, never()).save(any(UserAIAgent.class));
    }

    @Test
    void createUserAIAgent_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userAIAgentService.create(createDTO));
        
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(aiAgentRepository, never()).findById(aiAgentId);
        verify(userAIAgentRepository, never()).save(any(UserAIAgent.class));
    }

    @Test
    void createUserAIAgent_ShouldThrowException_WhenAIAgentNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userAIAgentService.create(createDTO));
        
        assertEquals("AIAgent not found with ID: " + aiAgentId, exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(userAIAgentRepository, never()).save(any(UserAIAgent.class));
    }

    @Test
    void createUserAIAgent_ShouldThrowException_WhenAssignmentAlreadyExists() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(userAIAgentRepository.existsByUserIdAndAiAgentId(userId, aiAgentId)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> userAIAgentService.create(createDTO));
        
        assertEquals("User is already assigned to this AI Agent", exception.getMessage());
        verify(companyRepository).findById(companyId);
        verify(userRepository).findById(userId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(userAIAgentRepository).existsByUserIdAndAiAgentId(userId, aiAgentId);
        verify(userAIAgentRepository, never()).save(any(UserAIAgent.class));
    }

    @Test
    void getUserAIAgentById_ShouldReturnUserAIAgent_WhenExists() {
        // Given
        when(userAIAgentRepository.findById(userAIAgentId)).thenReturn(Optional.of(userAIAgent));

        // When
        Optional<UserAIAgent> result = userAIAgentService.findById(userAIAgentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userAIAgent.getId(), result.get().getId());
        assertEquals(userAIAgent.getUser().getId(), result.get().getUser().getId());
        assertEquals(userAIAgent.getAiAgent().getId(), result.get().getAiAgent().getId());
        
        verify(userAIAgentRepository).findById(userAIAgentId);
    }

    @Test
    void getUserAIAgentById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(userAIAgentRepository.findById(userAIAgentId)).thenReturn(Optional.empty());

        // When
        Optional<UserAIAgent> result = userAIAgentService.findById(userAIAgentId);

        // Then
        assertTrue(result.isEmpty());
        verify(userAIAgentRepository).findById(userAIAgentId);
    }

    @Test
    void getAllUserAIAgents_ShouldReturnPagedResults() {
        // Given
        List<UserAIAgent> userAIAgents = List.of(userAIAgent);
        Page<UserAIAgent> page = new PageImpl<>(userAIAgents);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(userAIAgentRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<UserAIAgent> result = userAIAgentService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(userAIAgent.getId(), result.getContent().get(0).getId());
        
        verify(userAIAgentRepository).findAll(pageable);
    }

    @Test
    void getUserAIAgentsByCompanyId_ShouldReturnUserAIAgentsForCompany() {
        // Given
        List<UserAIAgent> userAIAgents = List.of(userAIAgent);
        when(userAIAgentRepository.findByCompanyId(companyId)).thenReturn(userAIAgents);

        // When
        List<UserAIAgent> result = userAIAgentService.findByCompanyId(companyId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userAIAgent.getId(), result.get(0).getId());
        assertEquals(companyId, result.get(0).getCompany().getId());
        
        verify(userAIAgentRepository).findByCompanyId(companyId);
    }

    @Test
    void updateUserAIAgent_ShouldUpdateAndReturnUserAIAgent_WhenValidData() {
        // Given
        when(userAIAgentRepository.findById(userAIAgentId)).thenReturn(Optional.of(userAIAgent));
        when(userAIAgentRepository.save(any(UserAIAgent.class))).thenReturn(userAIAgent);

        // When
        Optional<UserAIAgent> result = userAIAgentService.update(userAIAgentId, updateDTO);

        // Then
        assertTrue(result.isPresent());
        UserAIAgent updated = result.get();
        assertEquals(userAIAgent.getId(), updated.getId());
        
        verify(userAIAgentRepository).findById(userAIAgentId);
        verify(userAIAgentRepository).save(any(UserAIAgent.class));
    }

    @Test
    void updateUserAIAgent_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(userAIAgentRepository.findById(userAIAgentId)).thenReturn(Optional.empty());

        // When
        Optional<UserAIAgent> result = userAIAgentService.update(userAIAgentId, updateDTO);

        // Then
        assertTrue(result.isEmpty());
        
        verify(userAIAgentRepository).findById(userAIAgentId);
        verify(userAIAgentRepository, never()).save(any(UserAIAgent.class));
    }

    @Test
    void deleteUserAIAgent_ShouldReturnTrue_WhenExists() {
        // Given
        when(userAIAgentRepository.existsById(userAIAgentId)).thenReturn(true);

        // When
        boolean result = userAIAgentService.deleteById(userAIAgentId);

        // Then
        assertTrue(result);
        
        verify(userAIAgentRepository).existsById(userAIAgentId);
        verify(userAIAgentRepository).deleteById(userAIAgentId);
    }

    @Test
    void deleteUserAIAgent_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(userAIAgentRepository.existsById(userAIAgentId)).thenReturn(false);

        // When
        boolean result = userAIAgentService.deleteById(userAIAgentId);

        // Then
        assertFalse(result);
        
        verify(userAIAgentRepository).existsById(userAIAgentId);
        verify(userAIAgentRepository, never()).deleteById(userAIAgentId);
    }

    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(userAIAgentRepository.countByCompanyId(companyId)).thenReturn(5L);

        // When
        long count = userAIAgentService.countByCompanyId(companyId);

        // Then
        assertEquals(5L, count);
        verify(userAIAgentRepository).countByCompanyId(companyId);
    }

    @Test
    void findByUserId_ShouldReturnUserAIAgentsForUser() {
        // Given
        List<UserAIAgent> userAIAgents = List.of(userAIAgent);
        when(userAIAgentRepository.findByUserId(userId)).thenReturn(userAIAgents);

        // When
        List<UserAIAgent> result = userAIAgentService.findByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userAIAgent.getId(), result.get(0).getId());
        assertEquals(userId, result.get(0).getUser().getId());
        verify(userAIAgentRepository).findByUserId(userId);
    }

    @Test
    void findByAiAgentId_ShouldReturnUserAIAgentsForAIAgent() {
        // Given
        List<UserAIAgent> userAIAgents = List.of(userAIAgent);
        when(userAIAgentRepository.findByAiAgentId(aiAgentId)).thenReturn(userAIAgents);

        // When
        List<UserAIAgent> result = userAIAgentService.findByAiAgentId(aiAgentId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userAIAgent.getId(), result.get(0).getId());
        assertEquals(aiAgentId, result.get(0).getAiAgent().getId());
        verify(userAIAgentRepository).findByAiAgentId(aiAgentId);
    }

    @Test
    void findByUserIdAndAiAgentId_ShouldReturnSpecificAssignment() {
        // Given
        when(userAIAgentRepository.findByUserIdAndAiAgentId(userId, aiAgentId)).thenReturn(Optional.of(userAIAgent));

        // When
        Optional<UserAIAgent> result = userAIAgentService.findByUserIdAndAiAgentId(userId, aiAgentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userAIAgent.getId(), result.get().getId());
        assertEquals(userId, result.get().getUser().getId());
        assertEquals(aiAgentId, result.get().getAiAgent().getId());
        verify(userAIAgentRepository).findByUserIdAndAiAgentId(userId, aiAgentId);
    }

    @Test
    void findByIsDefault_ShouldReturnDefaultAssignments() {
        // Given
        List<UserAIAgent> userAIAgents = List.of(userAIAgent);
        when(userAIAgentRepository.findByIsDefault(true)).thenReturn(userAIAgents);

        // When
        List<UserAIAgent> result = userAIAgentService.findByIsDefault(true);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userAIAgent.getId(), result.get(0).getId());
        verify(userAIAgentRepository).findByIsDefault(true);
    }

    @Test
    void findByUserIdAndIsDefault_ShouldReturnUserDefaultAgent() {
        // Given
        when(userAIAgentRepository.findByUserIdAndIsDefault(userId, true)).thenReturn(Optional.of(userAIAgent));

        // When
        Optional<UserAIAgent> result = userAIAgentService.findByUserIdAndIsDefault(userId, true);

        // Then
        assertTrue(result.isPresent());
        assertEquals(userAIAgent.getId(), result.get().getId());
        assertEquals(userId, result.get().getUser().getId());
        verify(userAIAgentRepository).findByUserIdAndIsDefault(userId, true);
    }

    @Test
    void existsByUserIdAndAiAgentId_ShouldReturnTrue_WhenExists() {
        // Given
        when(userAIAgentRepository.existsByUserIdAndAiAgentId(userId, aiAgentId)).thenReturn(true);

        // When
        boolean result = userAIAgentService.existsByUserIdAndAiAgentId(userId, aiAgentId);

        // Then
        assertTrue(result);
        verify(userAIAgentRepository).existsByUserIdAndAiAgentId(userId, aiAgentId);
    }

    @Test
    void setAsDefault_ShouldUpdateDefaultStatus() {
        // Given
        when(userAIAgentRepository.findById(userAIAgentId)).thenReturn(Optional.of(userAIAgent));
        when(userAIAgentRepository.save(any(UserAIAgent.class))).thenReturn(userAIAgent);

        // When
        Optional<UserAIAgent> result = userAIAgentService.setAsDefault(userAIAgentId, true);

        // Then
        assertTrue(result.isPresent());
        verify(userAIAgentRepository).findById(userAIAgentId);
        verify(userAIAgentRepository).save(any(UserAIAgent.class));
    }

    @Test
    void clearDefaultForUser_ShouldUpdateAllUserAssignments() {
        // Given
        userAIAgent.setIsDefault(true); // Make sure it's default so it gets filtered
        List<UserAIAgent> userAIAgents = List.of(userAIAgent);
        when(userAIAgentRepository.findByUserId(userId)).thenReturn(userAIAgents);
        when(userAIAgentRepository.saveAll(any())).thenReturn(userAIAgents);

        // When
        userAIAgentService.clearDefaultForUser(userId);

        // Then
        verify(userAIAgentRepository).findByUserId(userId);
        verify(userAIAgentRepository).saveAll(any());
    }

    @Test
    void assignUserToAgent_ShouldCreateNewAssignment() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(aiAgentRepository.findById(aiAgentId)).thenReturn(Optional.of(aiAgent));
        when(userAIAgentRepository.existsByUserIdAndAiAgentId(userId, aiAgentId)).thenReturn(false);
        when(userAIAgentRepository.save(any(UserAIAgent.class))).thenReturn(userAIAgent);

        // When
        UserAIAgent result = userAIAgentService.assignUserToAgent(userId, aiAgentId, false);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(userId);
        verify(aiAgentRepository).findById(aiAgentId);
        verify(userAIAgentRepository).existsByUserIdAndAiAgentId(userId, aiAgentId);
        verify(userAIAgentRepository).save(any(UserAIAgent.class));
    }

    @Test
    void removeUserFromAgent_ShouldDeleteAssignment() {
        // Given
        when(userAIAgentRepository.findByUserIdAndAiAgentId(userId, aiAgentId)).thenReturn(Optional.of(userAIAgent));

        // When
        boolean result = userAIAgentService.removeUserFromAgent(userId, aiAgentId);

        // Then
        assertTrue(result);
        verify(userAIAgentRepository).findByUserIdAndAiAgentId(userId, aiAgentId);
        verify(userAIAgentRepository).delete(userAIAgent);
    }
}