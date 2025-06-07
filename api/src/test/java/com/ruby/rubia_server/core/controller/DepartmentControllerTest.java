package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.CreateDepartmentDTO;
import com.ruby.rubia_server.core.dto.DepartmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDepartmentDTO;
import com.ruby.rubia_server.core.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private DepartmentService departmentService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private DepartmentDTO departmentDTO;
    private CreateDepartmentDTO createDTO;
    private UpdateDepartmentDTO updateDTO;
    private UUID departmentId;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        departmentId = UUID.randomUUID();
        companyId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        
        departmentDTO = DepartmentDTO.builder()
                .id(departmentId)
                .name("Comercial")
                .description("Departamento comercial")
                .autoAssign(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateDepartmentDTO.builder()
                .name("Comercial")
                .description("Departamento comercial")
                .companyId(companyId)
                .autoAssign(true)
                .build();
        
        updateDTO = UpdateDepartmentDTO.builder()
                .name("Comercial Atualizado")
                .description("Descrição atualizada")
                .autoAssign(false)
                .build();
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnCreated_WhenValidData() throws Exception {
        when(departmentService.create(any(CreateDepartmentDTO.class))).thenReturn(departmentDTO);
        
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(departmentId.toString()))
                .andExpect(jsonPath("$.name").value("Comercial"))
                .andExpect(jsonPath("$.description").value("Departamento comercial"))
                .andExpect(jsonPath("$.autoAssign").value(true));
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        CreateDepartmentDTO invalidDTO = CreateDepartmentDTO.builder()
                .name("") // Nome vazio
                .companyId(companyId)
                .build();
        
        mockMvc.perform(post("/api/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnDepartment_WhenExists() throws Exception {
        when(departmentService.findById(departmentId)).thenReturn(departmentDTO);
        
        mockMvc.perform(get("/api/departments/{id}", departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(departmentId.toString()))
                .andExpect(jsonPath("$.name").value("Comercial"));
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(departmentService.findById(departmentId))
                .thenThrow(new IllegalArgumentException("Departamento não encontrado"));
        
        mockMvc.perform(get("/api/departments/{id}", departmentId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findAll_ShouldReturnAllDepartments() throws Exception {
        when(departmentService.findAll()).thenReturn(List.of(departmentDTO));
        
        mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(departmentId.toString()))
                .andExpect(jsonPath("$[0].name").value("Comercial"));
    }
    
    @Test
    @WithMockUser
    void findAll_ShouldReturnAutoAssignOnly_WhenParameterIsTrue() throws Exception {
        when(departmentService.findByAutoAssign()).thenReturn(List.of(departmentDTO));
        
        mockMvc.perform(get("/api/departments")
                .param("autoAssignOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].autoAssign").value(true));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnUpdatedDepartment_WhenValidData() throws Exception {
        when(departmentService.update(eq(departmentId), any(UpdateDepartmentDTO.class)))
                .thenReturn(departmentDTO);
        
        mockMvc.perform(put("/api/departments/{id}", departmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(departmentId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(departmentService.update(eq(departmentId), any(UpdateDepartmentDTO.class)))
                .thenThrow(new IllegalArgumentException("Departamento não encontrado"));
        
        mockMvc.perform(put("/api/departments/{id}", departmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNoContent_WhenExists() throws Exception {
        mockMvc.perform(delete("/api/departments/{id}", departmentId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNotFound_WhenNotExists() throws Exception {
        doThrow(new IllegalArgumentException("Departamento não encontrado"))
                .when(departmentService).delete(departmentId);
        
        mockMvc.perform(delete("/api/departments/{id}", departmentId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}