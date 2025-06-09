package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageDTO;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.service.MessageService;
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

@WebMvcTest(MessageController.class)
class MessageControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private MessageService messageService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MessageDTO messageDTO;
    private CreateMessageDTO createDTO;
    private UpdateMessageDTO updateDTO;
    private UUID messageId;
    private UUID conversationId;
    private UUID senderId;
    
    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        
        messageDTO = MessageDTO.builder()
                .id(messageId)
                .conversationId(conversationId)
                .senderId(senderId)
                .senderName("João Silva")
                .senderType(SenderType.CUSTOMER)
                .content("Olá, preciso de ajuda")
                .messageType(MessageType.TEXT)
                .status(MessageStatus.SENT)
                .externalMessageId("ext_123")
                .mediaUrl(null)
                .createdAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateMessageDTO.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .senderType(SenderType.CUSTOMER)
                .content("Olá, preciso de ajuda")
                .messageType(MessageType.TEXT)
                .externalMessageId("ext_123")
                .build();
        
        updateDTO = UpdateMessageDTO.builder()
                .content("Mensagem editada")
                .status(MessageStatus.DELIVERED)
                .build();
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnCreated_WhenValidData() throws Exception {
        when(messageService.create(any(CreateMessageDTO.class))).thenReturn(messageDTO);
        
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.conversationId").value(conversationId.toString()))
                .andExpect(jsonPath("$.content").value("Olá, preciso de ajuda"))
                .andExpect(jsonPath("$.senderType").value("CUSTOMER"));
    }
    
    @Test
    @WithMockUser
    void create_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        when(messageService.create(any(CreateMessageDTO.class)))
                .thenThrow(new IllegalArgumentException("Conversa não encontrada"));
        
        mockMvc.perform(post("/api/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDTO))
                .with(csrf()))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnMessage_WhenExists() throws Exception {
        when(messageService.findById(messageId)).thenReturn(messageDTO);
        
        mockMvc.perform(get("/api/messages/{id}", messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.content").value("Olá, preciso de ajuda"));
    }
    
    @Test
    @WithMockUser
    void findById_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(messageService.findById(messageId))
                .thenThrow(new IllegalArgumentException("Mensagem não encontrada"));
        
        mockMvc.perform(get("/api/messages/{id}", messageId))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void findByExternalMessageId_ShouldReturnMessage_WhenExists() throws Exception {
        when(messageService.findByExternalMessageId("ext_123")).thenReturn(messageDTO);
        
        mockMvc.perform(get("/api/messages/external/{externalMessageId}", "ext_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalMessageId").value("ext_123"));
    }
    
    @Test
    @WithMockUser
    void findByConversation_ShouldReturnMessages() throws Exception {
        when(messageService.findByConversation(conversationId)).thenReturn(List.of(messageDTO));
        
        mockMvc.perform(get("/api/messages/conversation/{conversationId}", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].conversationId").value(conversationId.toString()));
    }
    
    @Test
    @WithMockUser
    void findByConversationPaginated_ShouldReturnPagedMessages() throws Exception {
        Pageable pageable = PageRequest.of(0, 50);
        Page<MessageDTO> page = new PageImpl<>(List.of(messageDTO), pageable, 1);
        
        when(messageService.findByConversationWithPagination(eq(conversationId), any(Pageable.class)))
                .thenReturn(page);
        
        mockMvc.perform(get("/api/messages/conversation/{conversationId}/paginated", conversationId)
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
    
    @Test
    @WithMockUser
    void searchInContent_ShouldReturnMatchingMessages() throws Exception {
        when(messageService.searchInContent("ajuda")).thenReturn(List.of(messageDTO));
        
        mockMvc.perform(get("/api/messages/search")
                .param("searchTerm", "ajuda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].content").value("Olá, preciso de ajuda"));
    }
    
    @Test
    @WithMockUser
    void searchInConversation_ShouldReturnMatchingMessages() throws Exception {
        when(messageService.searchInConversation(conversationId, "ajuda")).thenReturn(List.of(messageDTO));
        
        mockMvc.perform(get("/api/messages/conversation/{conversationId}/search", conversationId)
                .param("searchTerm", "ajuda"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].conversationId").value(conversationId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnUpdatedMessage_WhenValidData() throws Exception {
        when(messageService.update(eq(messageId), any(UpdateMessageDTO.class))).thenReturn(messageDTO);
        
        mockMvc.perform(put("/api/messages/{id}", messageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()));
    }
    
    @Test
    @WithMockUser
    void update_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(messageService.update(eq(messageId), any(UpdateMessageDTO.class)))
                .thenThrow(new IllegalArgumentException("Mensagem não encontrada"));
        
        mockMvc.perform(put("/api/messages/{id}", messageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void markAsDelivered_ShouldReturnUpdatedMessage() throws Exception {
        when(messageService.markAsDelivered(messageId)).thenReturn(messageDTO);
        
        mockMvc.perform(put("/api/messages/{id}/delivered", messageId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()));
    }
    
    @Test
    @WithMockUser
    void markAsDelivered_ShouldReturnNotFound_WhenNotExists() throws Exception {
        when(messageService.markAsDelivered(messageId))
                .thenThrow(new IllegalArgumentException("Mensagem não encontrada"));
        
        mockMvc.perform(put("/api/messages/{id}/delivered", messageId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser
    void markAsRead_ShouldReturnUpdatedMessage() throws Exception {
        when(messageService.markAsRead(messageId)).thenReturn(messageDTO);
        
        mockMvc.perform(put("/api/messages/{id}/read", messageId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()));
    }
    
    @Test
    @WithMockUser
    void markConversationAsRead_ShouldReturnOk() throws Exception {
        mockMvc.perform(put("/api/messages/conversation/{conversationId}/read-all", conversationId)
                .with(csrf()))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser
    void countUnreadByConversation_ShouldReturnCount() throws Exception {
        when(messageService.countUnreadByConversation(conversationId)).thenReturn(3L);
        
        mockMvc.perform(get("/api/messages/conversation/{conversationId}/stats/unread", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(3));
    }
    
    @Test
    @WithMockUser
    void countByConversation_ShouldReturnCount() throws Exception {
        when(messageService.countByConversation(conversationId)).thenReturn(10L);
        
        mockMvc.perform(get("/api/messages/conversation/{conversationId}/stats/total", conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(10));
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNoContent_WhenExists() throws Exception {
        mockMvc.perform(delete("/api/messages/{id}", messageId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @WithMockUser
    void delete_ShouldReturnNotFound_WhenNotExists() throws Exception {
        doThrow(new IllegalArgumentException("Mensagem não encontrada"))
                .when(messageService).delete(messageId);
        
        mockMvc.perform(delete("/api/messages/{id}", messageId)
                .with(csrf()))
                .andExpect(status().isNotFound());
    }
}