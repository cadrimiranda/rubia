package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateUserDTO;
import com.ruby.rubia_server.core.dto.UserDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private UserService userService;

    private Company company;
    private Department department;
    private User user;
    private CreateUserDTO createUserDTO;
    private UUID companyId;
    private UUID departmentId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        userId = UUID.randomUUID();

        company = Company.builder().id(companyId).name("Test Company").build();
        department = Department.builder().id(departmentId).name("Test Department").company(company).build();
        
        createUserDTO = CreateUserDTO.builder()
                .name("Test User")
                .email("test@example.com")
                .password("password123")
                .companyId(companyId)
                .departmentId(departmentId)
                .role(UserRole.AGENT)
                .build();

        user = User.builder()
                .id(userId)
                .name(createUserDTO.getName())
                .email(createUserDTO.getEmail())
                .passwordHash("hashedPassword")
                .company(company)
                .department(department)
                .role(createUserDTO.getRole())
                .build();
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(userRepository.existsByEmailAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        UserDTO result = userService.create(createUserDTO);

        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(createUserDTO.getName(), result.getName());
        assertEquals(createUserDTO.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUser_FailsWhenUserExists() {
        // Arrange
        when(userRepository.existsByEmailAndCompanyId(createUserDTO.getEmail(), companyId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.create(createUserDTO);
        });
        assertEquals("Usuário com email '" + createUserDTO.getEmail() + "' já existe nesta empresa", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void findById_Success() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserDTO result = userService.findById(userId);

        // Assert
        assertNotNull(result);
        assertEquals(userId, result.getId());
        assertEquals(user.getName(), result.getName());
    }

    @Test
    void findById_NotFound() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.findById(userId);
        });
        assertEquals("Usuário não encontrado", exception.getMessage());
    }
}
