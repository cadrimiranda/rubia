package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.CreateUserDTO;
import com.ruby.rubia_server.core.dto.UpdateUserDTO;
import com.ruby.rubia_server.core.dto.UserDTO;
import com.ruby.rubia_server.core.dto.UserLoginDTO;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.service.UserService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @MockBean
    private CompanyContextUtil companyContextUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private UserDTO userDTO;
    private CreateUserDTO createDTO;
    private UpdateUserDTO updateDTO;
    private UserLoginDTO loginDTO;
    private UUID userId;
    private UUID departmentId;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        userDTO = UserDTO.builder()
                .id(userId)
                .name("João Silva")
                .email("joao@company.com")
                .role(UserRole.AGENT)
                .departmentId(departmentId)
                .departmentName("Suporte")
                .companyId(companyId)
                .isOnline(true)
                .avatarUrl("avatar.jpg")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateUserDTO.builder()
                .name("João Silva")
                .email("joao@company.com")
                .password("password123")
                .role(UserRole.AGENT)
                .departmentId(departmentId)
                .companyId(companyId)
                .build();
        
        updateDTO = UpdateUserDTO.builder()
                .name("João Silva Atualizado")
                .role(UserRole.SUPERVISOR)
                .departmentId(departmentId)
                .avatarUrl("new-avatar.jpg")
                .build();
        
        loginDTO = UserLoginDTO.builder()
                .email("joao@company.com")
                .password("password123")
                .build();
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnCreated_WhenValidData() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.create(any(CreateUserDTO.class))).thenReturn(userDTO);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@company.com"))
                .andExpect(jsonPath("$.role").value("AGENT"));
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnBadRequest_WhenEmailAlreadyExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.create(any(CreateUserDTO.class)))
                .thenThrow(new IllegalArgumentException("Email já está em uso"));
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnUser_WhenExists() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("João Silva"));
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(userService.findById(userId))
                .thenThrow(new IllegalArgumentException("Usuário não encontrado"));
        
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnNotFound_WhenSecurityException() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        doThrow(new SecurityException("Access denied"))
                .when(companyContextUtil).ensureCompanyAccess(companyId);
        
        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findByEmail_ShouldReturnUser_WhenExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.findByEmailAndCompany("joao@company.com", companyId)).thenReturn(userDTO);
        
        mockMvc.perform(get("/api/users/email/{email}", "joao@company.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@company.com"));
    }
    
    @Test
    @WithMockUser
    void findByEmail_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.findByEmailAndCompany("inexistente@test.com", companyId))
                .thenThrow(new IllegalArgumentException("Usuário não encontrado"));
        
        mockMvc.perform(get("/api/users/email/{email}", "inexistente@test.com"))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findAll_ShouldReturnAllUsers_WhenNoDepartmentFilter() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.findAllByCompany(companyId)).thenReturn(List.of(userDTO));
        
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(userId.toString()));
    }
    
    @Test
    @WithMockUser
    void findAll_ShouldReturnFilteredUsers_WhenDepartmentFilter() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.findByDepartmentAndCompany(departmentId, companyId)).thenReturn(List.of(userDTO));
        
        mockMvc.perform(get("/api/users")
                .param("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].departmentId").value(departmentId.toString()));
    }
    
    @Test
    @WithMockUser
    void findAvailableAgents_ShouldReturnAgents_WhenNoDepartmentFilter() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.findAvailableAgentsByCompany(companyId)).thenReturn(List.of(userDTO));
        
        mockMvc.perform(get("/api/users/available-agents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].role").value("AGENT"));
    }
    
    @Test
    @WithMockUser
    void findAvailableAgents_ShouldReturnFilteredAgents_WhenDepartmentFilter() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.findAvailableAgentsByDepartmentAndCompany(departmentId, companyId))
                .thenReturn(List.of(userDTO));
        
        mockMvc.perform(get("/api/users/available-agents")
                .param("departmentId", departmentId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].departmentId").value(departmentId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnUpdatedUser_WhenValidData() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        when(userService.update(eq(userId), any(UpdateUserDTO.class))).thenReturn(userDTO);
        
        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(userService.findById(userId))
                .thenThrow(new IllegalArgumentException("Usuário não encontrado"));
        
        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnNotFound_WhenSecurityException() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        doThrow(new SecurityException("Access denied"))
                .when(companyContextUtil).ensureCompanyAccess(companyId);
        
        mockMvc.perform(put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void updateOnlineStatus_ShouldReturnUpdatedUser() throws Exception {
        when(userService.updateOnlineStatus(userId, false)).thenReturn(userDTO);
        
        mockMvc.perform(put("/api/users/{id}/online-status", userId)
                .param("isOnline", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }
    
    @Test
    @WithMockUser
    void updateOnlineStatus_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        when(userService.updateOnlineStatus(userId, false))
                .thenThrow(new IllegalArgumentException("Usuário não encontrado"));
        
        mockMvc.perform(put("/api/users/{id}/online-status", userId)
                .param("isOnline", "false")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void assignToDepartment_ShouldReturnUpdatedUser() throws Exception {
        UUID newDepartmentId = UUID.randomUUID();
        when(userService.assignToDepartment(userId, newDepartmentId)).thenReturn(userDTO);
        
        mockMvc.perform(put("/api/users/{userId}/assign-department/{departmentId}", userId, newDepartmentId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }
    
    @Test
    @WithMockUser
    void assignToDepartment_ShouldReturnNotFound_WhenUserNotExists() throws Exception {
        UUID newDepartmentId = UUID.randomUUID();
        when(userService.assignToDepartment(userId, newDepartmentId))
                .thenThrow(new IllegalArgumentException("Usuário não encontrado"));
        
        mockMvc.perform(put("/api/users/{userId}/assign-department/{departmentId}", userId, newDepartmentId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void login_ShouldReturnTrue_WhenValidCredentials() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.validateLoginByCompany(any(UserLoginDTO.class), eq(companyId))).thenReturn(true);
        
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
    
    @Test
    @WithMockUser
    void login_ShouldReturnUnauthorized_WhenInvalidCredentials() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(userService.validateLoginByCompany(any(UserLoginDTO.class), eq(companyId))).thenReturn(false);
        
        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDTO))
                .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value(false));
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNoContent_WhenExists() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        
        mockMvc.perform(delete("/api/users/{id}", userId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(userService.findById(userId))
                .thenThrow(new IllegalArgumentException("Usuário não encontrado"));
        
        mockMvc.perform(delete("/api/users/{id}", userId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNotFound_WhenSecurityException() throws Exception {
        when(userService.findById(userId)).thenReturn(userDTO);
        doThrow(new SecurityException("Access denied"))
                .when(companyContextUtil).ensureCompanyAccess(companyId);
        
        mockMvc.perform(delete("/api/users/{id}", userId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}