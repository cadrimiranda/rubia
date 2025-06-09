package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationDTO;
import com.ruby.rubia_server.core.enums.ConversationChannel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@WebMvcTest(ConversationController.class)
class ConversationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ConversationService conversationService;
    
    @MockBean
    private CompanyContextUtil companyContextUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ConversationDTO conversationDTO;
    private CreateConversationDTO createDTO;
    private UpdateConversationDTO updateDTO;
    private UUID conversationId;
    private UUID customerId;
    private UUID userId;
    private UUID departmentId;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        conversationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        conversationDTO = ConversationDTO.builder()
                .id(conversationId)
                .customerId(customerId)
                .customerName("João Silva")
                .customerPhone("+5511999999001")
                .assignedUserId(userId)
                .assignedUserName("Agent Silva")
                .departmentId(departmentId)
                .departmentName("Suporte")
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .isPinned(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateConversationDTO.builder()
                .customerId(customerId)
                .departmentId(departmentId)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .build();
        
        updateDTO = UpdateConversationDTO.builder()
                .status(ConversationStatus.ESPERANDO)
                .priority(2)
                .isPinned(true)
                .build();
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnCreated_WhenValidData() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.create(any(CreateConversationDTO.class), eq(companyId))).thenReturn(conversationDTO);
        
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.customerName").value("João Silva"))
                .andExpect(jsonPath("$.status").value("ENTRADA"));
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.create(any(CreateConversationDTO.class), eq(companyId)))
                .thenThrow(new IllegalArgumentException("Customer não encontrado"));
        
        mockMvc.perform(post("/api/conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnConversation_WhenExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findById(conversationId, companyId)).thenReturn(conversationDTO);
        
        mockMvc.perform(get("/api/conversations/{id}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.customerName").value("João Silva"));
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findById(conversationId, companyId))
                .thenThrow(new IllegalArgumentException("Conversa não encontrada"));
        
        mockMvc.perform(get("/api/conversations/{id}", conversationId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findByStatus_ShouldReturnConversations() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findByStatusAndCompany(ConversationStatus.ENTRADA, companyId))
                .thenReturn(List.of(conversationDTO));
        
        mockMvc.perform(get("/api/conversations")
                .param("status", "ENTRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(conversationId.toString()));
    }
    
    @Test
    @WithMockUser
    void findByStatusPaginated_ShouldReturnPagedConversations() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ConversationDTO> page = new PageImpl<>(List.of(conversationDTO), pageable, 1);
        
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findByStatusAndCompanyWithPagination(eq(ConversationStatus.ENTRADA), eq(companyId), any(Pageable.class)))
                .thenReturn(page);
        
        mockMvc.perform(get("/api/conversations/paginated")
                .param("status", "ENTRADA")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(conversationId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    @WithMockUser
    void findByCustomer_ShouldReturnCustomerConversations() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findByCustomerAndCompany(customerId, companyId))
                .thenReturn(List.of(conversationDTO));
        
        mockMvc.perform(get("/api/conversations/customer/{customerId}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].customerId").value(customerId.toString()));
    }
    
    @Test
    @WithMockUser
    void findByAssignedUser_ShouldReturnUserConversations() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findByAssignedUserAndCompany(userId, companyId))
                .thenReturn(List.of(conversationDTO));
        
        mockMvc.perform(get("/api/conversations/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].assignedUserId").value(userId.toString()));
    }
    
    @Test
    @WithMockUser
    void findUnassigned_ShouldReturnUnassignedConversations() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findUnassignedByCompany(companyId))
                .thenReturn(List.of(conversationDTO));
        
        mockMvc.perform(get("/api/conversations/unassigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnUpdatedConversation_WhenValidData() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.update(eq(conversationId), any(UpdateConversationDTO.class), eq(companyId)))
                .thenReturn(conversationDTO);
        
        mockMvc.perform(put("/api/conversations/{id}", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.update(eq(conversationId), any(UpdateConversationDTO.class), eq(companyId)))
                .thenThrow(new IllegalArgumentException("Conversa não encontrada"));
        
        mockMvc.perform(put("/api/conversations/{id}", conversationId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void assignToUser_ShouldReturnAssignedConversation() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.assignToUser(conversationId, userId, companyId))
                .thenReturn(conversationDTO);
        
        mockMvc.perform(put("/api/conversations/{conversationId}/assign/{userId}", conversationId, userId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedUserId").value(userId.toString()));
    }
    
    @Test
    @WithMockUser
    void assignToUser_ShouldReturnNotFound_WhenConversationNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.assignToUser(conversationId, userId, companyId))
                .thenThrow(new IllegalArgumentException("Conversa não encontrada"));
        
        mockMvc.perform(put("/api/conversations/{conversationId}/assign/{userId}", conversationId, userId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void changeStatus_ShouldReturnUpdatedConversation() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.changeStatus(conversationId, ConversationStatus.ESPERANDO, companyId))
                .thenReturn(conversationDTO);
        
        mockMvc.perform(put("/api/conversations/{conversationId}/status", conversationId)
                .param("status", "ESPERANDO")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));
    }
    
    @Test
    @WithMockUser
    void pinConversation_ShouldReturnPinnedConversation() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.pinConversation(conversationId, companyId))
                .thenReturn(conversationDTO);
        
        mockMvc.perform(put("/api/conversations/{conversationId}/pin", conversationId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()));
    }
    
    @Test
    @WithMockUser
    void countByStatus_ShouldReturnCount() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.countByStatusAndCompany(ConversationStatus.ENTRADA, companyId))
                .thenReturn(5L);
        
        mockMvc.perform(get("/api/conversations/stats/count")
                .param("status", "ENTRADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(5));
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNoContent_WhenExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        
        mockMvc.perform(delete("/api/conversations/{id}", conversationId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        doThrow(new IllegalArgumentException("Conversa não encontrada"))
                .when(conversationService).delete(conversationId, companyId);
        
        mockMvc.perform(delete("/api/conversations/{id}", conversationId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}