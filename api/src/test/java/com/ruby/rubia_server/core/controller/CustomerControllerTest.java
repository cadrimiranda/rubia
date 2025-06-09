package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.service.CustomerService;
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

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private CustomerService customerService;
    
    @MockBean
    private CompanyContextUtil companyContextUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private CustomerDTO customerDTO;
    private CreateCustomerDTO createDTO;
    private UpdateCustomerDTO updateDTO;
    private UUID customerId;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        customerDTO = CustomerDTO.builder()
                .id(customerId)
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .profileUrl("profile.jpg")
                .isBlocked(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateCustomerDTO.builder()
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .profileUrl("profile.jpg")
                .isBlocked(false)
                .build();
        
        updateDTO = UpdateCustomerDTO.builder()
                .name("João Silva Atualizado")
                .whatsappId("wa_001_updated")
                .build();
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnCreated_WhenValidData() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.create(any(CreateCustomerDTO.class), eq(companyId))).thenReturn(customerDTO);
        
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.phone").value("+5511999999001"))
                .andExpect(jsonPath("$.name").value("João Silva"))
                .andExpect(jsonPath("$.whatsappId").value("wa_001"))
                .andExpect(jsonPath("$.isBlocked").value(false));
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        CreateCustomerDTO invalidDTO = CreateCustomerDTO.builder()
                .phone("") // Phone vazio
                .name("João Silva")
                .build();
        
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidDTO))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnBadRequest_WhenPhoneAlreadyExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.create(any(CreateCustomerDTO.class), eq(companyId)))
                .thenThrow(new IllegalArgumentException("Cliente com telefone já existe"));
        
        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnCustomer_WhenExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.findById(customerId, companyId)).thenReturn(customerDTO);
        
        mockMvc.perform(get("/api/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.phone").value("+5511999999001"))
                .andExpect(jsonPath("$.name").value("João Silva"));
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.findById(customerId, companyId))
                .thenThrow(new IllegalArgumentException("Cliente não encontrado"));
        
        mockMvc.perform(get("/api/customers/{id}", customerId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findAll_ShouldReturnAllCustomers() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.findAllByCompany(companyId)).thenReturn(List.of(customerDTO));
        
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(customerId.toString()))
                .andExpect(jsonPath("$[0].name").value("João Silva"));
    }
    
    @Test
    @WithMockUser
    void findByPhone_ShouldReturnCustomer_WhenExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.findByPhoneAndCompany("+5511999999001", companyId)).thenReturn(customerDTO);
        
        mockMvc.perform(get("/api/customers/phone/{phone}", "+5511999999001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+5511999999001"))
                .andExpect(jsonPath("$.name").value("João Silva"));
    }
    
    @Test
    @WithMockUser
    void search_ShouldReturnMatchingCustomers() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.searchByNameOrPhoneAndCompany("João", companyId)).thenReturn(List.of(customerDTO));
        
        mockMvc.perform(get("/api/customers/search")
                .param("q", "João"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("João Silva"));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnUpdatedCustomer_WhenValidData() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.update(eq(customerId), any(UpdateCustomerDTO.class), eq(companyId)))
                .thenReturn(customerDTO);
        
        mockMvc.perform(put("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.update(eq(customerId), any(UpdateCustomerDTO.class), eq(companyId)))
                .thenThrow(new IllegalArgumentException("Cliente não encontrado"));
        
        mockMvc.perform(put("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void blockCustomer_ShouldReturnUpdatedCustomer() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.blockCustomer(customerId, companyId)).thenReturn(customerDTO);
        
        mockMvc.perform(patch("/api/customers/{id}/block", customerId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()));
    }
    
    @Test
    @WithMockUser
    void unblockCustomer_ShouldReturnUpdatedCustomer() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(customerService.unblockCustomer(customerId, companyId)).thenReturn(customerDTO);
        
        mockMvc.perform(patch("/api/customers/{id}/unblock", customerId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()));
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNoContent_WhenExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        doThrow(new IllegalArgumentException("Cliente não encontrado"))
                .when(customerService).delete(customerId, companyId);
        
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}