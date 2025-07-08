package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.ConversationMediaDTO;
import com.ruby.rubia_server.core.dto.CreateConversationMediaDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationMediaDTO;
import com.ruby.rubia_server.core.entity.ConversationMedia;
import com.ruby.rubia_server.core.enums.MediaType;
import com.ruby.rubia_server.core.service.ConversationMediaService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversation-media")
@Slf4j
public class ConversationMediaController extends BaseCompanyEntityController<ConversationMedia, CreateConversationMediaDTO, UpdateConversationMediaDTO, ConversationMediaDTO> {

    private final ConversationMediaService conversationMediaService;

    public ConversationMediaController(ConversationMediaService conversationMediaService, CompanyContextUtil companyContextUtil) {
        super(conversationMediaService, companyContextUtil);
        this.conversationMediaService = conversationMediaService;
    }

    @Override
    protected String getEntityName() {
        return "ConversationMedia";
    }

    @Override
    protected ConversationMediaDTO convertToDTO(ConversationMedia conversationMedia) {
        return ConversationMediaDTO.builder()
                .id(conversationMedia.getId())
                .companyId(conversationMedia.getCompany().getId())
                .companyName(conversationMedia.getCompany().getName())
                .conversationId(conversationMedia.getConversation().getId())
                .fileUrl(conversationMedia.getFileUrl())
                .mediaType(conversationMedia.getMediaType())
                .mimeType(conversationMedia.getMimeType())
                .originalFileName(conversationMedia.getOriginalFileName())
                .fileSizeBytes(conversationMedia.getFileSizeBytes())
                .checksum(conversationMedia.getChecksum())
                .uploadedByUserId(conversationMedia.getUploadedByUser() != null ? conversationMedia.getUploadedByUser().getId() : null)
                .uploadedByUserName(conversationMedia.getUploadedByUser() != null ? conversationMedia.getUploadedByUser().getName() : null)
                .uploadedByCustomerId(conversationMedia.getUploadedByCustomer() != null ? conversationMedia.getUploadedByCustomer().getId() : null)
                .uploadedByCustomerName(conversationMedia.getUploadedByCustomer() != null ? conversationMedia.getUploadedByCustomer().getName() : null)
                .uploadedAt(conversationMedia.getUploadedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateConversationMediaDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // Endpoints específicos da entidade
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<ConversationMediaDTO>> findByConversationId(@PathVariable UUID conversationId) {
        log.debug("Finding ConversationMedia by conversation id via API: {}", conversationId);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.findByConversationId(conversationId);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/media-type/{mediaType}")
    public ResponseEntity<List<ConversationMediaDTO>> findByMediaType(@PathVariable MediaType mediaType) {
        log.debug("Finding ConversationMedia by media type via API: {}", mediaType);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.findByMediaType(mediaType);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/media-type/{mediaType}")
    public ResponseEntity<List<ConversationMediaDTO>> findByCompanyAndMediaType(
            @PathVariable UUID companyId, 
            @PathVariable MediaType mediaType) {
        log.debug("Finding ConversationMedia by company: {} and media type: {}", companyId, mediaType);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.findByCompanyIdAndMediaType(companyId, mediaType);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/conversation/{conversationId}/media-type/{mediaType}")
    public ResponseEntity<List<ConversationMediaDTO>> findByConversationAndMediaType(
            @PathVariable UUID conversationId, 
            @PathVariable MediaType mediaType) {
        log.debug("Finding ConversationMedia by conversation: {} and media type: {}", conversationId, mediaType);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.findByConversationIdAndMediaType(conversationId, mediaType);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/uploaded-by-user/{userId}")
    public ResponseEntity<List<ConversationMediaDTO>> findByUploadedByUserId(@PathVariable UUID userId) {
        log.debug("Finding ConversationMedia by uploaded by user id via API: {}", userId);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.findByUploadedByUserId(userId);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/uploaded-by-customer/{customerId}")
    public ResponseEntity<List<ConversationMediaDTO>> findByUploadedByCustomerId(@PathVariable UUID customerId) {
        log.debug("Finding ConversationMedia by uploaded by customer id via API: {}", customerId);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.findByUploadedByCustomerId(customerId);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/conversation/{conversationId}/count")
    public ResponseEntity<Long> countByConversationId(@PathVariable UUID conversationId) {
        log.debug("Counting ConversationMedia by conversation id via API: {}", conversationId);
        
        long count = conversationMediaService.countByConversationId(conversationId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/media-type/{mediaType}/count")
    public ResponseEntity<Long> countByMediaType(@PathVariable MediaType mediaType) {
        log.debug("Counting ConversationMedia by media type via API: {}", mediaType);
        
        long count = conversationMediaService.countByMediaType(mediaType);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/company/{companyId}/media-type/{mediaType}/count")
    public ResponseEntity<Long> countByCompanyAndMediaType(
            @PathVariable UUID companyId, 
            @PathVariable MediaType mediaType) {
        log.debug("Counting ConversationMedia by company: {} and media type: {}", companyId, mediaType);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long count = conversationMediaService.countByCompanyIdAndMediaType(companyId, mediaType);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/company/{companyId}/total-file-size")
    public ResponseEntity<Long> getTotalFileSizeByCompanyId(@PathVariable UUID companyId) {
        log.debug("Getting total file size by company id via API: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long totalSize = conversationMediaService.getTotalFileSizeByCompanyId(companyId);
        return ResponseEntity.ok(totalSize);
    }

    @GetMapping("/conversation/{conversationId}/total-file-size")
    public ResponseEntity<Long> getTotalFileSizeByConversationId(@PathVariable UUID conversationId) {
        log.debug("Getting total file size by conversation id via API: {}", conversationId);
        
        long totalSize = conversationMediaService.getTotalFileSizeByConversationId(conversationId);
        return ResponseEntity.ok(totalSize);
    }

    @GetMapping("/media-type/{mediaType}/total-file-size")
    public ResponseEntity<Long> getTotalFileSizeByMediaType(@PathVariable MediaType mediaType) {
        log.debug("Getting total file size by media type via API: {}", mediaType);
        
        long totalSize = conversationMediaService.getTotalFileSizeByMediaType(mediaType);
        return ResponseEntity.ok(totalSize);
    }

    @GetMapping("/conversation/{conversationId}/images")
    public ResponseEntity<List<ConversationMediaDTO>> getImagesByConversationId(@PathVariable UUID conversationId) {
        log.debug("Getting images for conversation via API: {}", conversationId);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.getImagesByConversationId(conversationId);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/conversation/{conversationId}/documents")
    public ResponseEntity<List<ConversationMediaDTO>> getDocumentsByConversationId(@PathVariable UUID conversationId) {
        log.debug("Getting documents for conversation via API: {}", conversationId);
        
        List<ConversationMedia> conversationMedias = conversationMediaService.getDocumentsByConversationId(conversationId);
        List<ConversationMediaDTO> responseDTOs = conversationMedias.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }
}