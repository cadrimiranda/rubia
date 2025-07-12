package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationControllerUnitTest {

    @Mock
    private ConversationService conversationService;

    @Mock
    private CompanyContextUtil companyContextUtil;

    @InjectMocks
    private ConversationController conversationController;

    private UUID companyId;
    private UUID customerId;
    private ConversationDTO mockConversationDTO;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        
        mockConversationDTO = ConversationDTO.builder()
                .id(UUID.randomUUID())
                .companyId(companyId)
                .customerId(customerId)
                .status(ConversationStatus.ENTRADA)
                .channel(Channel.WHATSAPP)
                .priority(1)
                .build();
    }

    @Test
    void shouldCreateConversationSuccessfully() {
        // Given
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(customerId);
        createDTO.setChannel(Channel.WHATSAPP);
        createDTO.setPriority(1);

        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.create(any(CreateConversationDTO.class), eq(companyId)))
                .thenReturn(mockConversationDTO);

        // When
        ResponseEntity<ConversationDTO> response = conversationController.create(createDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(mockConversationDTO.getId());
        assertThat(response.getBody().getStatus()).isEqualTo(ConversationStatus.ENTRADA);
        assertThat(response.getBody().getChannel()).isEqualTo(Channel.WHATSAPP);
    }

    @Test
    void shouldReturnNotFoundWhenConversationDoesNotExist() {
        // Given
        UUID conversationId = UUID.randomUUID();
        
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findById(conversationId, companyId))
                .thenThrow(new IllegalArgumentException("Conversa não encontrada"));

        // When
        ResponseEntity<ConversationDTO> response = conversationController.findById(conversationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldFindConversationByIdSuccessfully() {
        // Given
        UUID conversationId = mockConversationDTO.getId();
        
        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.findById(conversationId, companyId))
                .thenReturn(mockConversationDTO);

        // When
        ResponseEntity<ConversationDTO> response = conversationController.findById(conversationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(conversationId);
        assertThat(response.getBody().getStatus()).isEqualTo(ConversationStatus.ENTRADA);
    }

    @Test
    void shouldHandleCreateConversationError() {
        // Given
        CreateConversationDTO createDTO = new CreateConversationDTO();
        createDTO.setCustomerId(customerId);
        createDTO.setChannel(Channel.WHATSAPP);

        when(companyContextUtil.getCurrentCompanyId()).thenReturn(companyId);
        when(conversationService.create(any(CreateConversationDTO.class), eq(companyId)))
                .thenThrow(new IllegalArgumentException("Cliente não encontrado"));

        // When & Then
        try {
            conversationController.create(createDTO);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Cliente não encontrado");
        }
    }
}