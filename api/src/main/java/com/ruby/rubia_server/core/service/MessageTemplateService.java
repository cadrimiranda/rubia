package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public MessageTemplateService(MessageTemplateRepository messageTemplateRepository,
                                 CompanyRepository companyRepository,
                                 UserRepository userRepository,
                                 AIAgentRepository aiAgentRepository,
                                 EntityRelationshipValidator relationshipValidator) {
        super(messageTemplateRepository, companyRepository, relationshipValidator);
        this.messageTemplateRepository = messageTemplateRepository;
        this.userRepository = userRepository;
        this.aiAgentRepository = aiAgentRepository;
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

        // Handle optional relationships
        if (createDTO.getCreatedByUserId() != null) {
            User user = userRepository.findById(createDTO.getCreatedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getCreatedByUserId()));
            builder.createdBy(user);
        }

        if (createDTO.getAiAgentId() != null) {
            AIAgent aiAgent = aiAgentRepository.findById(createDTO.getAiAgentId())
                    .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + createDTO.getAiAgentId()));
            builder.aiAgent(aiAgent);
        }

        return builder.build();
    }

    @Override
    protected void updateEntityFromDTO(MessageTemplate messageTemplate, UpdateMessageTemplateDTO updateDTO) {
        if (updateDTO.getName() != null) {
            messageTemplate.setName(updateDTO.getName());
        }
        if (updateDTO.getContent() != null) {
            messageTemplate.setContent(updateDTO.getContent());
        }
        if (updateDTO.getTone() != null) {
            messageTemplate.setTone(updateDTO.getTone());
        }
        
        // Handle last edited by user
        if (updateDTO.getLastEditedByUserId() != null) {
            User user = userRepository.findById(updateDTO.getLastEditedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + updateDTO.getLastEditedByUserId()));
            messageTemplate.setLastEditedBy(user);
            
            // Increment edit count
            messageTemplate.setEditCount(messageTemplate.getEditCount() + 1);
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateMessageTemplateDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<MessageTemplate> findByIsAiGenerated(Boolean isAiGenerated) {
        log.debug("Finding MessageTemplates by AI generated flag: {}", isAiGenerated);
        return messageTemplateRepository.findByIsAiGenerated(isAiGenerated);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated) {
        log.debug("Finding MessageTemplates by company: {} and AI generated flag: {}", companyId, isAiGenerated);
        return messageTemplateRepository.findByCompanyIdAndIsAiGenerated(companyId, isAiGenerated);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCreatedByUserId(UUID createdByUserId) {
        log.debug("Finding MessageTemplates by created by user id: {}", createdByUserId);
        return messageTemplateRepository.findByCreatedById(createdByUserId);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByAiAgentId(UUID aiAgentId) {
        log.debug("Finding MessageTemplates by AI agent id: {}", aiAgentId);
        return messageTemplateRepository.findByAiAgentId(aiAgentId);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByTone(String tone) {
        log.debug("Finding MessageTemplates by tone: {}", tone);
        return messageTemplateRepository.findByTone(tone);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByNameContaining(String name) {
        log.debug("Finding MessageTemplates by name containing: {}", name);
        return messageTemplateRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByContentContaining(String content) {
        log.debug("Finding MessageTemplates by content containing: {}", content);
        return messageTemplateRepository.findByContentContainingIgnoreCase(content);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCompanyIdAndTone(UUID companyId, String tone) {
        log.debug("Finding MessageTemplates by company: {} and tone: {}", companyId, tone);
        return messageTemplateRepository.findByCompanyIdAndTone(companyId, tone);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        log.debug("Checking if MessageTemplate exists by name: {} and company: {}", name, companyId);
        return messageTemplateRepository.existsByNameAndCompanyId(name, companyId);
    }

    @Transactional(readOnly = true)
    public long countByIsAiGenerated(Boolean isAiGenerated) {
        log.debug("Counting MessageTemplates by AI generated flag: {}", isAiGenerated);
        return messageTemplateRepository.countByIsAiGenerated(isAiGenerated);
    }

    @Transactional(readOnly = true)
    public long countByCompanyIdAndIsAiGenerated(UUID companyId, Boolean isAiGenerated) {
        log.debug("Counting MessageTemplates by company: {} and AI generated flag: {}", companyId, isAiGenerated);
        return messageTemplateRepository.countByCompanyIdAndIsAiGenerated(companyId, isAiGenerated);
    }

    @Transactional(readOnly = true)
    public long countByCreatedByUserId(UUID createdByUserId) {
        log.debug("Counting MessageTemplates by created by user id: {}", createdByUserId);
        return messageTemplateRepository.countByCreatedById(createdByUserId);
    }

    @Transactional(readOnly = true)
    public long countByAiAgentId(UUID aiAgentId) {
        log.debug("Counting MessageTemplates by AI agent id: {}", aiAgentId);
        return messageTemplateRepository.countByAiAgentId(aiAgentId);
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
        return messageTemplateRepository.findByCompanyIdAndIsAiGenerated(companyId, false);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> getAiGeneratedTemplates(UUID companyId) {
        log.debug("Getting AI-generated templates for company: {}", companyId);
        return messageTemplateRepository.findByCompanyIdAndIsAiGenerated(companyId, true);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplate> searchTemplates(String searchTerm) {
        log.debug("Searching templates for term: {}", searchTerm);
        List<MessageTemplate> nameMatches = messageTemplateRepository.findByNameContainingIgnoreCase(searchTerm);
        List<MessageTemplate> contentMatches = messageTemplateRepository.findByContentContainingIgnoreCase(searchTerm);
        
        // Combine and deduplicate results
        nameMatches.addAll(contentMatches);
        return nameMatches.stream().distinct().toList();
    }
}