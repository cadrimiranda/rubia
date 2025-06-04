package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateUserDTO;
import com.ruby.rubia_server.core.dto.UpdateUserDTO;
import com.ruby.rubia_server.core.dto.UserDTO;
import com.ruby.rubia_server.core.dto.UserLoginDTO;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @InjectMocks
    private UserService userService;
    
    private User user;
    private Department department;
    private CreateUserDTO createDTO;
    private UpdateUserDTO updateDTO;
    private UserLoginDTO loginDTO;
    private UUID userId;
    private UUID departmentId;
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        
        department = Department.builder()
                .id(departmentId)
                .name("Comercial")
                .description("Departamento comercial")
                .autoAssign(true)
                .build();
        
        user = User.builder()
                .id(userId)
                .name("João Silva")
                .email("joao@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .department(department)
                .role(UserRole.AGENT)
                .avatarUrl("avatar.jpg")
                .isOnline(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateUserDTO.builder()
                .name("João Silva")
                .email("joao@test.com")
                .password("password123")
                .departmentId(departmentId)
                .role(UserRole.AGENT)
                .avatarUrl("avatar.jpg")
                .build();
        
        updateDTO = UpdateUserDTO.builder()
                .name("João Silva Atualizado")
                .email("joao.updated@test.com")
                .build();
        
        loginDTO = UserLoginDTO.builder()
                .email("joao@test.com")
                .password("password123")
                .build();
    }
    
    @Test
    void create_ShouldCreateUser_WhenValidData() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        UserDTO result = userService.create(createDTO);
        
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("João Silva");
        assertThat(result.getEmail()).isEqualTo("joao@test.com");
        assertThat(result.getRole()).isEqualTo(UserRole.AGENT);
        assertThat(result.getDepartmentId()).isEqualTo(departmentId);
        
        verify(userRepository).existsByEmail("joao@test.com");
        verify(departmentRepository).findById(departmentId);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        assertThatThrownBy(() -> userService.create(createDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
        
        verify(userRepository).existsByEmail("joao@test.com");
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenDepartmentNotFound() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.create(createDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Departamento não encontrado");
        
        verify(departmentRepository).findById(departmentId);
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void findById_ShouldReturnUser_WhenExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        
        UserDTO result = userService.findById(userId);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getName()).isEqualTo("João Silva");
        
        verify(userRepository).findById(userId);
    }
    
    @Test
    void findById_ShouldThrowException_WhenNotExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(userRepository).findById(userId);
    }
    
    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        when(userRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(user));
        
        UserDTO result = userService.findByEmail("joao@test.com");
        
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("joao@test.com");
        
        verify(userRepository).findByEmail("joao@test.com");
    }
    
    @Test
    void findAll_ShouldReturnAllUsers() {
        when(userRepository.findAllOrderedByName()).thenReturn(List.of(user));
        
        List<UserDTO> result = userService.findAll();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("João Silva");
        
        verify(userRepository).findAllOrderedByName();
    }
    
    @Test
    void findAvailableAgents_ShouldReturnOnlineAgents() {
        when(userRepository.findAvailableAgents()).thenReturn(List.of(user));
        
        List<UserDTO> result = userService.findAvailableAgents();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.AGENT);
        
        verify(userRepository).findAvailableAgents();
    }
    
    @Test
    void update_ShouldUpdateUser_WhenValidData() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        UserDTO result = userService.update(userId, updateDTO);
        
        assertThat(result).isNotNull();
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void updateOnlineStatus_ShouldUpdateStatus() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        UserDTO result = userService.updateOnlineStatus(userId, true);
        
        assertThat(result).isNotNull();
        
        verify(userRepository).findById(userId);
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void validateLogin_ShouldReturnTrue_WhenCredentialsAreValid() {
        when(userRepository.findByEmail("joao@test.com")).thenReturn(Optional.of(user));
        
        boolean result = userService.validateLogin(loginDTO);
        
        assertThat(result).isTrue();
        
        verify(userRepository).findByEmail("joao@test.com");
    }
    
    @Test
    void validateLogin_ShouldReturnFalse_WhenUserNotFound() {
        when(userRepository.findByEmail("joao@test.com")).thenReturn(Optional.empty());
        
        boolean result = userService.validateLogin(loginDTO);
        
        assertThat(result).isFalse();
        
        verify(userRepository).findByEmail("joao@test.com");
    }
    
    @Test
    void delete_ShouldDeleteUser_WhenExists() {
        when(userRepository.existsById(userId)).thenReturn(true);
        
        userService.delete(userId);
        
        verify(userRepository).existsById(userId);
        verify(userRepository).deleteById(userId);
    }
    
    @Test
    void delete_ShouldThrowException_WhenNotExists() {
        when(userRepository.existsById(userId)).thenReturn(false);
        
        assertThatThrownBy(() -> userService.delete(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(userRepository).existsById(userId);
        verify(userRepository, never()).deleteById(any());
    }
}