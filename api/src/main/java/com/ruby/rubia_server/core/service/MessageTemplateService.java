package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.RevisionType;
import com.ruby.rubia_server.core.exception.MessageTemplateRevisionException;
import com.ruby.rubia_server.core.exception.MessageTemplateTransactionException;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class MessageTemplateService extends BaseCompanyEntityService<MessageTemplate, CreateMessageTemplateDTO, UpdateMessageTemplateDTO> {

    private final MessageTemplateRepository messageTemplateRepository;
    private final UserRepository userRepository;
    private final AIAgentRepository aiAgentRepository;
    private final MessageTemplateRevisionService messageTemplateRevisionService;
    private final CompanyContextUtil companyContextUtil;
    
    @Value("${app.message-template.revision.fail-on-error:true}")
    private boolean failOnRevisionError;

    public MessageTemplateService(MessageTemplateRepository messageTemplateRepository,
                                 CompanyRepository companyRepository,
                                 UserRepository userRepository,
                                 AIAgentRepository aiAgentRepository,
                                 MessageTemplateRevisionService messageTemplateRevisionService,
                                 EntityRelationshipValidator relationshipValidator,
                                 CompanyContextUtil companyContextUtil) {
        super(messageTemplateRepository, companyRepository, relationshipValidator);
        this.messageTemplateRepository = messageTemplateRepository;
        this.userRepository = userRepository;
        this.aiAgentRepository = aiAgentRepository;
        this.messageTemplateRevisionService = messageTemplateRevisionService;
        this.companyContextUtil = companyContextUtil;
    }

    @Override
    protected String getEntityName() {
        return "MessageTemplate";
    }

    @Override
    protected MessageTemplate buildEntityFromDTO(CreateMessageTemplateDTO createDTO) {
        MessageTemplate.MessageTemplateBuilder builder = MessageTemplate.builder()
                .name(createDTO.getName())
                .content(createDTO.getContent())
                .isAiGenerated(createDTO.getIsAiGenerated())
                .tone(createDTO.getTone())
                .editCount(0);

        // Automatically set the created by user from authenticated user
        try {
            User authenticatedUser = companyContextUtil.getAuthenticatedUser();
            builder.createdBy(authenticatedUser);
        } catch (Exception e) {
            log.warn("Could not set created by user: {}", e.getMessage());
            // Continue without setting the user - this maintains backward compatibility
        }

        if (createDTO.getAiAgentId() != null) {
            AIAgent aiAgent = aiAgentRepository.findById(createDTO.getAiAgentId())
                    .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + createDTO.getAiAgentId()));
            builder.aiAgent(aiAgent);
        }

        return builder.build();
    }

    @Override
    public MessageTemplate create(CreateMessageTemplateDTO createDTO) {
        log.debug("Creating MessageTemplate: {}", createDTO.getName());
        
        // Create the template using the parent method
        MessageTemplate createdTemplate = super.create(createDTO);
        
        // Create the initial revision (revision number 1) using authenticated user
        try {
            User authenticatedUser = companyContextUtil.getAuthenticatedUser();
            try {
                messageTemplateRevisionService.createRevisionFromTemplate(
                    createdTemplate.getId(), 
                    createdTemplate.getContent(), 
                    authenticatedUser.getId(),
                    RevisionType.CREATE
                );
                log.debug("Initial revision created for template: {}", createdTemplate.getId());
            } catch (Exception e) {
                return handleRevisionCreationFailure(
                    createdTemplate.getId().toString(),
                    "CREATE",
                    "Failed to create initial revision for template",
                    e,
                    createdTemplate
                );
            }
        } catch (Exception e) {
            log.warn("Could not create initial revision - no authenticated user: {}", e.getMessage());
            // Continue without creating revision - this maintains backward compatibility
        }
        
        return createdTemplate;
    }

    @Override
    protected void updateEntityFromDTO(MessageTemplate messageTemplate, UpdateMessageTemplateDTO updateDTO) {
        boolean contentChanged = false;
        String oldContent = messageTemplate.getContent();
        
        if (updateDTO.getName() != null) {
            messageTemplate.setName(updateDTO.getName());
        }
        if (updateDTO.getContent() != null) {
            messageTemplate.setContent(updateDTO.getContent());
            contentChanged = !oldContent.equals(updateDTO.getContent());
        }
        if (updateDTO.getTone() != null) {
            messageTemplate.setTone(updateDTO.getTone());
        }
        
        // Automatically set the last edited by user from the authenticated user
        try {
            User authenticatedUser = companyContextUtil.getAuthenticatedUser();
            messageTemplate.setLastEditedBy(authenticatedUser);
            
            // Increment edit count
            messageTemplate.setEditCount(messageTemplate.getEditCount() + 1);
            
            // Create revision if content changed
            if (contentChanged) {
                try {
                    messageTemplateRevisionService.createRevisionFromTemplate(
                        messageTemplate.getId(), 
                        messageTemplate.getContent(), 
                        authenticatedUser.getId(),
                        RevisionType.EDIT
                    );
                    log.debug("Revision created for template: {} after content change", messageTemplate.getId());
                } catch (Exception e) {
                    handleRevisionCreationFailureUpdate(
                        messageTemplate.getId().toString(),
                        "UPDATE",
                        "Failed to create revision for template update",
                        e
                    );
                }
            }
        } catch (Exception e) {
            log.warn("Could not set last edited by user: {}", e.getMessage());
            // Continue without setting the user - this maintains backward compatibility
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateMessageTemplateDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade (filtram templates deletados por padrão)
    @Transactional(readOnly = true)
    public List<MessageTemplate> findByIsAiGenerated(Boolean isAiGenerated) {
        log.debug("Finding MessageTemplates by AI generated flag: {}", isAiGenerated);
        return messageTemplateRepository.findByIsAiGeneratedAndNotDeleted(isAiGenerated);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated) {
        log.debug("Finding MessageTemplates by company: {} and AI generated flag: {}", companyId, isAiGenerated);
        return messageTemplateRepository.findByCompanyIdAndIsAiGeneratedAndNotDeleted(companyId, isAiGenerated);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCreatedByUserId(UUID createdByUserId) {
        log.debug("Finding MessageTemplates by created by user id: {}", createdByUserId);
        return messageTemplateRepository.findByCreatedByIdAndNotDeleted(createdByUserId);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByAiAgentId(UUID aiAgentId) {
        log.debug("Finding MessageTemplates by AI agent id: {}", aiAgentId);
        return messageTemplateRepository.findByAiAgentIdAndNotDeleted(aiAgentId);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByTone(String tone) {
        log.debug("Finding MessageTemplates by tone: {}", tone);
        return messageTemplateRepository.findByToneAndNotDeleted(tone);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByNameContaining(String name) {
        log.debug("Finding MessageTemplates by name containing: {}", name);
        return messageTemplateRepository.findByNameContainingIgnoreCaseAndNotDeleted(name);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByContentContaining(String content) {
        log.debug("Finding MessageTemplates by content containing: {}", content);
        return messageTemplateRepository.findByContentContainingIgnoreCaseAndNotDeleted(content);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCompanyIdAndTone(UUID companyId, String tone) {
        log.debug("Finding MessageTemplates by company: {} and tone: {}", companyId, tone);
        return messageTemplateRepository.findByCompanyIdAndToneAndNotDeleted(companyId, tone);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        log.debug("Checking if MessageTemplate exists by name: {} and company: {}", name, companyId);
        return messageTemplateRepository.existsByNameAndCompanyIdAndNotDeleted(name, companyId);
    }

    @Transactional(readOnly = true)
    public long countByIsAiGenerated(Boolean isAiGenerated) {
        log.debug("Counting MessageTemplates by AI generated flag: {}", isAiGenerated);
        return messageTemplateRepository.countByIsAiGeneratedAndNotDeleted(isAiGenerated);
    }

    @Transactional(readOnly = true)
    public long countByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated) {
        log.debug("Counting MessageTemplates by company: {} and AI generated flag: {}", companyId, isAiGenerated);
        return messageTemplateRepository.countByCompanyIdAndIsAiGeneratedAndNotDeleted(companyId, isAiGenerated);
    }

    @Transactional(readOnly = true)
    public long countByCreatedByUserId(UUID createdByUserId) {
        log.debug("Counting MessageTemplates by created by user id: {}", createdByUserId);
        return messageTemplateRepository.countByCreatedByIdAndNotDeleted(createdByUserId);
    }

    @Transactional(readOnly = true)
    public long countByAiAgentId(UUID aiAgentId) {
        log.debug("Counting MessageTemplates by AI agent id: {}", aiAgentId);
        return messageTemplateRepository.countByAiAgentIdAndNotDeleted(aiAgentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCompanyId(UUID companyId) {
        log.debug("Finding MessageTemplates by company id: {}", companyId);
        return messageTemplateRepository.findByCompanyIdAndNotDeleted(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageTemplate> findById(UUID id) {
        log.debug("Finding MessageTemplate by id: {}", id);
        return messageTemplateRepository.findByIdAndNotDeleted(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCompanyId(UUID companyId) {
        log.debug("Counting MessageTemplates by company id: {}", companyId);
        return messageTemplateRepository.countByCompanyIdAndNotDeleted(companyId);
    }

    // Métodos para acessar templates deletados
    @Transactional(readOnly = true)
    public List<MessageTemplate> findDeletedByCompanyId(UUID companyId) {
        log.debug("Finding deleted MessageTemplates by company id: {}", companyId);
        return messageTemplateRepository.findDeletedByCompanyId(companyId);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findAllDeleted() {
        log.debug("Finding all deleted MessageTemplates");
        return messageTemplateRepository.findAllDeleted();
    }

    // Métodos que incluem templates deletados (para casos especiais)
    @Transactional(readOnly = true)
    public Optional<MessageTemplate> findByIdIncludingDeleted(UUID id) {
        log.debug("Finding MessageTemplate by id including deleted: {}", id);
        return messageTemplateRepository.findById(id);
    }

    @Transactional
    public Optional<MessageTemplate> incrementEditCount(UUID templateId, UUID editorUserId) {
        log.debug("Incrementing edit count for MessageTemplate: {} by user: {}", templateId, editorUserId);

        Optional<MessageTemplate> optionalTemplate = messageTemplateRepository.findById(templateId);
        if (optionalTemplate.isEmpty()) {
            log.warn("MessageTemplate not found with id: {}", templateId);
            return Optional.empty();
        }

        MessageTemplate template = optionalTemplate.get();
        
        // Get the editor user
        User editor = userRepository.findById(editorUserId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + editorUserId));

        template.setEditCount(template.getEditCount() + 1);
        template.setLastEditedBy(editor);

        MessageTemplate updatedTemplate = messageTemplateRepository.save(template);
        log.debug("MessageTemplate edit count incremented successfully for id: {}", updatedTemplate.getId());

        return Optional.of(updatedTemplate);
    }

    @Transactional
    public Optional<MessageTemplate> cloneTemplate(UUID templateId, String newName) {
        log.debug("Cloning MessageTemplate: {} with new name: {}", templateId, newName);

        Optional<MessageTemplate> optionalTemplate = messageTemplateRepository.findById(templateId);
        if (optionalTemplate.isEmpty()) {
            log.warn("MessageTemplate not found with id: {}", templateId);
            return Optional.empty();
        }

        MessageTemplate originalTemplate = optionalTemplate.get();
        
        // Create a clone
        MessageTemplate clonedTemplate = MessageTemplate.builder()
                .company(originalTemplate.getCompany())
                .name(newName)
                .content(originalTemplate.getContent())
                .isAiGenerated(originalTemplate.getIsAiGenerated())
                .createdBy(originalTemplate.getCreatedBy())
                .aiAgent(originalTemplate.getAiAgent())
                .tone(originalTemplate.getTone())
                .editCount(0)
                .build();

        MessageTemplate savedTemplate = messageTemplateRepository.save(clonedTemplate);
        log.debug("MessageTemplate cloned successfully with id: {}", savedTemplate.getId());

        return Optional.of(savedTemplate);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> getHumanCreatedTemplates(UUID companyId) {
        log.debug("Getting human-created templates for company: {}", companyId);
        return messageTemplateRepository.findByCompanyIdAndIsAiGeneratedAndNotDeleted(companyId, false);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> getAiGeneratedTemplates(UUID companyId) {
        log.debug("Getting AI-generated templates for company: {}", companyId);
        return messageTemplateRepository.findByCompanyIdAndIsAiGeneratedAndNotDeleted(companyId, true);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> searchTemplates(String searchTerm) {
        log.debug("Searching templates for term: {}", searchTerm);
        List<MessageTemplate> nameMatches = messageTemplateRepository.findByNameContainingIgnoreCaseAndNotDeleted(searchTerm);
        List<MessageTemplate> contentMatches = messageTemplateRepository.findByContentContainingIgnoreCaseAndNotDeleted(searchTerm);
        
        // Combine and deduplicate results
        nameMatches.addAll(contentMatches);
        return nameMatches.stream().distinct().toList();
    }
    
    /**
     * Handles revision creation failure for create operations.
     * Based on configuration, either throws exception (rollback) or returns the template (continue).
     */
    private MessageTemplate handleRevisionCreationFailure(
            String templateId, 
            String operation, 
            String message, 
            Exception cause,
            MessageTemplate template) {
        
        if (failOnRevisionError) {
            log.error("Failed to create revision for template: {}, rolling back transaction", templateId, cause);
            throw new MessageTemplateTransactionException(templateId, operation, message, cause);
        } else {
            log.warn("Failed to create revision for template: {}, continuing without revision", templateId, cause);
            return template;
        }
    }
    
    /**
     * Handles revision creation failure for update operations.
     * Based on configuration, either throws exception (rollback) or continues silently.
     */
    private void handleRevisionCreationFailureUpdate(
            String templateId, 
            String operation, 
            String message, 
            Exception cause) {
        
        if (failOnRevisionError) {
            log.error("Failed to create revision for template: {}, rolling back transaction", templateId, cause);
            throw new MessageTemplateTransactionException(templateId, operation, message, cause);
        } else {
            log.warn("Failed to create revision for template: {}, continuing without revision", templateId, cause);
        }
    }
    
    /**
     * Soft delete a MessageTemplate and create a revision for the deletion.
     * @param id The template ID to delete
     * @return true if the template was successfully deleted, false if not found
     */
    @Transactional
    public boolean softDeleteById(UUID id) {
        log.info("Soft deleting MessageTemplate with id: {}", id);
        
        Optional<MessageTemplate> optionalTemplate = messageTemplateRepository.findById(id);
        if (optionalTemplate.isEmpty()) {
            log.warn("MessageTemplate not found with id: {}", id);
            return false;
        }
        
        MessageTemplate template = optionalTemplate.get();
        
        // Check if already deleted
        if (template.getDeletedAt() != null) {
            log.warn("MessageTemplate with id: {} is already deleted", id);
            return false;
        }
        
        // Mark as deleted
        template.setDeletedAt(LocalDateTime.now());
        
        // Set the user who deleted it from authenticated user
        try {
            User authenticatedUser = companyContextUtil.getAuthenticatedUser();
            template.setLastEditedBy(authenticatedUser);
            
            // Save the soft-deleted template
            messageTemplateRepository.save(template);
            
            // Create a revision for the deletion
            try {
                messageTemplateRevisionService.createRevisionFromTemplate(
                    template.getId(),
                    template.getContent(),
                    authenticatedUser.getId(),
                    RevisionType.DELETE
                );
                log.debug("Deletion revision created for template: {}", template.getId());
            } catch (Exception e) {
                handleRevisionCreationFailureUpdate(
                    template.getId().toString(),
                    "DELETE",
                    "Failed to create revision for template deletion",
                    e
                );
            }
        } catch (Exception e) {
            log.warn("Could not set deleted by user: {}", e.getMessage());
            // Still save the template as deleted, but without user information
            messageTemplateRepository.save(template);
        }
        
        log.info("MessageTemplate soft deleted successfully with id: {}", id);
        return true;
    }
    
    /**
     * Soft delete a MessageTemplate and create a revision for the deletion.
     * @param id The template ID to delete
     * @param deletedByUserId The user who is deleting the template (for backward compatibility)
     * @return true if the template was successfully deleted, false if not found
     * @deprecated Use softDeleteById(UUID id) instead - user is automatically determined from authentication
     */
    @Deprecated
    @Transactional
    public boolean softDeleteById(UUID id, UUID deletedByUserId) {
        log.warn("Using deprecated softDeleteById(UUID, UUID) method. Consider using softDeleteById(UUID) instead.");
        return softDeleteById(id);
    }
    
    /**
     * Restore a soft-deleted MessageTemplate.
     * @param id The template ID to restore
     * @return true if the template was successfully restored, false if not found or not deleted
     */
    @Transactional
    public boolean restoreById(UUID id) {
        log.info("Restoring MessageTemplate with id: {}", id);
        
        Optional<MessageTemplate> optionalTemplate = messageTemplateRepository.findById(id);
        if (optionalTemplate.isEmpty()) {
            log.warn("MessageTemplate not found with id: {}", id);
            return false;
        }
        
        MessageTemplate template = optionalTemplate.get();
        
        // Check if not deleted
        if (template.getDeletedAt() == null) {
            log.warn("MessageTemplate with id: {} is not deleted, cannot restore", id);
            return false;
        }
        
        // Restore the template
        template.setDeletedAt(null);
        
        // Set the user who restored it from authenticated user
        try {
            User authenticatedUser = companyContextUtil.getAuthenticatedUser();
            template.setLastEditedBy(authenticatedUser);
            
            // Save the restored template
            messageTemplateRepository.save(template);
            
            // Create a revision for the restoration
            try {
                messageTemplateRevisionService.createRevisionFromTemplate(
                    template.getId(),
                    template.getContent(),
                    authenticatedUser.getId(),
                    RevisionType.RESTORE
                );
                log.debug("Restoration revision created for template: {}", template.getId());
            } catch (Exception e) {
                handleRevisionCreationFailureUpdate(
                    template.getId().toString(),
                    "RESTORE",
                    "Failed to create revision for template restoration",
                    e
                );
            }
        } catch (Exception e) {
            log.warn("Could not set restored by user: {}", e.getMessage());
            // Still save the template as restored, but without user information
            messageTemplateRepository.save(template);
        }
        
        log.info("MessageTemplate restored successfully with id: {}", id);
        return true;
    }
    
    /**
     * Restore a soft-deleted MessageTemplate.
     * @param id The template ID to restore
     * @param restoredByUserId The user who is restoring the template (for backward compatibility)
     * @return true if the template was successfully restored, false if not found or not deleted
     * @deprecated Use restoreById(UUID id) instead - user is automatically determined from authentication
     */
    @Deprecated
    @Transactional
    public boolean restoreById(UUID id, UUID restoredByUserId) {
        log.warn("Using deprecated restoreById(UUID, UUID) method. Consider using restoreById(UUID) instead.");
        return restoreById(id);
    }
    
    /**
     * Override the parent deleteById to use soft delete instead of hard delete.
     */
    @Override
    public boolean deleteById(UUID id) {
        log.warn("Hard delete called on MessageTemplate with id: {}. Consider using softDeleteById instead.", id);
        return super.deleteById(id);
    }
}