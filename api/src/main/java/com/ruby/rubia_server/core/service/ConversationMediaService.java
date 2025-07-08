package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateConversationMediaDTO;
import com.ruby.rubia_server.core.dto.UpdateConversationMediaDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.MediaType;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class ConversationMediaService extends BaseCompanyEntityService<ConversationMedia, CreateConversationMediaDTO, UpdateConversationMediaDTO> {

    private final ConversationMediaRepository conversationMediaRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public ConversationMediaService(ConversationMediaRepository conversationMediaRepository,
                                   CompanyRepository companyRepository,
                                   ConversationRepository conversationRepository,
                                   UserRepository userRepository,
                                   CustomerRepository customerRepository,
                                   EntityRelationshipValidator relationshipValidator) {
        super(conversationMediaRepository, companyRepository, relationshipValidator);
        this.conversationMediaRepository = conversationMediaRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    @Override
    protected String getEntityName() {
        return "ConversationMedia";
    }

    @Override
    protected ConversationMedia buildEntityFromDTO(CreateConversationMediaDTO createDTO) {
        ConversationMedia.ConversationMediaBuilder builder = ConversationMedia.builder()
                .fileUrl(createDTO.getFileUrl())
                .mediaType(createDTO.getMediaType())
                .mimeType(createDTO.getMimeType())
                .originalFileName(createDTO.getOriginalFileName())
                .fileSizeBytes(createDTO.getFileSizeBytes())
                .checksum(createDTO.getChecksum());

        // Validate and set required conversation
        Conversation conversation = conversationRepository.findById(createDTO.getConversationId())
                .orElseThrow(() -> new RuntimeException("Conversation not found with ID: " + createDTO.getConversationId()));
        builder.conversation(conversation);

        // Handle optional relationships
        if (createDTO.getUploadedByUserId() != null) {
            User user = userRepository.findById(createDTO.getUploadedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getUploadedByUserId()));
            builder.uploadedByUser(user);
        }

        if (createDTO.getUploadedByCustomerId() != null) {
            Customer customer = customerRepository.findById(createDTO.getUploadedByCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + createDTO.getUploadedByCustomerId()));
            builder.uploadedByCustomer(customer);
        }

        return builder.build();
    }

    @Override
    protected void updateEntityFromDTO(ConversationMedia conversationMedia, UpdateConversationMediaDTO updateDTO) {
        if (updateDTO.getFileUrl() != null) {
            conversationMedia.setFileUrl(updateDTO.getFileUrl());
        }
        if (updateDTO.getMimeType() != null) {
            conversationMedia.setMimeType(updateDTO.getMimeType());
        }
        if (updateDTO.getOriginalFileName() != null) {
            conversationMedia.setOriginalFileName(updateDTO.getOriginalFileName());
        }
        if (updateDTO.getFileSizeBytes() != null) {
            conversationMedia.setFileSizeBytes(updateDTO.getFileSizeBytes());
        }
        if (updateDTO.getChecksum() != null) {
            conversationMedia.setChecksum(updateDTO.getChecksum());
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateConversationMediaDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<ConversationMedia> findByConversationId(UUID conversationId) {
        log.debug("Finding ConversationMedia by conversation id: {}", conversationId);
        return conversationMediaRepository.findByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> findByMediaType(MediaType mediaType) {
        log.debug("Finding ConversationMedia by media type: {}", mediaType);
        return conversationMediaRepository.findByMediaType(mediaType);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> findByCompanyIdAndMediaType(UUID companyId, MediaType mediaType) {
        log.debug("Finding ConversationMedia by company: {} and media type: {}", companyId, mediaType);
        return conversationMediaRepository.findByCompanyIdAndMediaType(companyId, mediaType);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> findByConversationIdAndMediaType(UUID conversationId, MediaType mediaType) {
        log.debug("Finding ConversationMedia by conversation: {} and media type: {}", conversationId, mediaType);
        return conversationMediaRepository.findByConversationIdAndMediaType(conversationId, mediaType);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> findByUploadedByUserId(UUID uploadedByUserId) {
        log.debug("Finding ConversationMedia by uploaded by user id: {}", uploadedByUserId);
        return conversationMediaRepository.findByUploadedByUserId(uploadedByUserId);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> findByUploadedByCustomerId(UUID uploadedByCustomerId) {
        log.debug("Finding ConversationMedia by uploaded by customer id: {}", uploadedByCustomerId);
        return conversationMediaRepository.findByUploadedByCustomerId(uploadedByCustomerId);
    }

    @Transactional(readOnly = true)
    public long countByConversationId(UUID conversationId) {
        log.debug("Counting ConversationMedia by conversation id: {}", conversationId);
        return conversationMediaRepository.countByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public long countByMediaType(MediaType mediaType) {
        log.debug("Counting ConversationMedia by media type: {}", mediaType);
        return conversationMediaRepository.countByMediaType(mediaType);
    }

    @Transactional(readOnly = true)
    public long countByCompanyIdAndMediaType(UUID companyId, MediaType mediaType) {
        log.debug("Counting ConversationMedia by company: {} and media type: {}", companyId, mediaType);
        return conversationMediaRepository.countByCompanyIdAndMediaType(companyId, mediaType);
    }

    @Transactional(readOnly = true)
    public long getTotalFileSizeByCompanyId(UUID companyId) {
        log.debug("Getting total file size by company id: {}", companyId);
        Long totalSize = conversationMediaRepository.getTotalFileSizeByCompanyId(companyId);
        return totalSize != null ? totalSize : 0L;
    }

    @Transactional(readOnly = true)
    public long getTotalFileSizeByConversationId(UUID conversationId) {
        log.debug("Getting total file size by conversation id: {}", conversationId);
        Long totalSize = conversationMediaRepository.getTotalFileSizeByConversationId(conversationId);
        return totalSize != null ? totalSize : 0L;
    }

    @Transactional(readOnly = true)
    public long getTotalFileSizeByMediaType(MediaType mediaType) {
        log.debug("Getting total file size by media type: {}", mediaType);
        Long totalSize = conversationMediaRepository.getTotalFileSizeByMediaType(mediaType);
        return totalSize != null ? totalSize : 0L;
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> getMediaForConversation(UUID conversationId) {
        log.debug("Getting all media for conversation: {}", conversationId);
        return conversationMediaRepository.findByConversationId(conversationId);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> getImagesByConversationId(UUID conversationId) {
        log.debug("Getting images for conversation: {}", conversationId);
        return conversationMediaRepository.findByConversationIdAndMediaType(conversationId, MediaType.IMAGE);
    }

    @Transactional(readOnly = true)
    public List<ConversationMedia> getDocumentsByConversationId(UUID conversationId) {
        log.debug("Getting documents for conversation: {}", conversationId);
        return conversationMediaRepository.findByConversationIdAndMediaType(conversationId, MediaType.DOCUMENT);
    }
}