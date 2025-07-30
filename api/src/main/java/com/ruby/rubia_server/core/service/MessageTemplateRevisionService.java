package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateMessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.dto.UpdateMessageTemplateRevisionDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.RevisionType;
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
public class MessageTemplateRevisionService extends BaseCompanyEntityService<MessageTemplateRevision, CreateMessageTemplateRevisionDTO, UpdateMessageTemplateRevisionDTO> {

    private final MessageTemplateRevisionRepository messageTemplateRevisionRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final UserRepository userRepository;
    private final AIAgentRepository aiAgentRepository;

    public MessageTemplateRevisionService(MessageTemplateRevisionRepository messageTemplateRevisionRepository,
                                         CompanyRepository companyRepository,
                                         MessageTemplateRepository messageTemplateRepository,
                                         UserRepository userRepository,
                                         AIAgentRepository aiAgentRepository,
                                         EntityRelationshipValidator relationshipValidator) {
        super(messageTemplateRevisionRepository, companyRepository, relationshipValidator);
        this.messageTemplateRevisionRepository = messageTemplateRevisionRepository;
        this.messageTemplateRepository = messageTemplateRepository;
        this.userRepository = userRepository;
        this.aiAgentRepository = aiAgentRepository;
    }

    @Override
    protected String getEntityName() {
        return "MessageTemplateRevision";
    }

    @Override
    protected MessageTemplateRevision buildEntityFromDTO(CreateMessageTemplateRevisionDTO createDTO) {
        MessageTemplate template = messageTemplateRepository.findById(createDTO.getTemplateId())
                .orElseThrow(() -> new RuntimeException("MessageTemplate not found with ID: " + createDTO.getTemplateId()));

        MessageTemplateRevision.MessageTemplateRevisionBuilder builder = MessageTemplateRevision.builder()
                .company(template.getCompany()) // Set the company from the template
                .template(template)
                .revisionNumber(createDTO.getRevisionNumber())
                .content(createDTO.getContent());

        // Handle optional editor user
        if (createDTO.getEditedByUserId() != null) {
            User user = userRepository.findById(createDTO.getEditedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getEditedByUserId()));
            builder.editedBy(user);
        }

        // Handle AI metadata
        if (createDTO.getAiAgentId() != null) {
            AIAgent aiAgent = aiAgentRepository.findById(createDTO.getAiAgentId())
                    .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + createDTO.getAiAgentId()));
            builder.aiAgent(aiAgent);
        }
        
        builder.aiEnhancementType(createDTO.getAiEnhancementType())
               .aiTokensUsed(createDTO.getAiTokensUsed())
               .aiCreditsConsumed(createDTO.getAiCreditsConsumed())
               .aiModelUsed(createDTO.getAiModelUsed())
               .aiExplanation(createDTO.getAiExplanation());

        // Set revision type - AI enhancement if AI metadata present, otherwise based on revision number
        RevisionType revisionType;
        if (createDTO.getAiEnhancementType() != null) {
            revisionType = RevisionType.AI_ENHANCEMENT;
        } else {
            revisionType = createDTO.getRevisionNumber() == 1 ? RevisionType.CREATE : RevisionType.EDIT;
        }
        builder.revisionType(revisionType);

        return builder.build();
    }

    @Override
    protected void updateEntityFromDTO(MessageTemplateRevision revision, UpdateMessageTemplateRevisionDTO updateDTO) {
        if (updateDTO.getContent() != null) {
            revision.setContent(updateDTO.getContent());
        }
        
        if (updateDTO.getEditedByUserId() != null) {
            User user = userRepository.findById(updateDTO.getEditedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + updateDTO.getEditedByUserId()));
            revision.setEditedBy(user);
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateMessageTemplateRevisionDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<MessageTemplateRevision> findByTemplateId(UUID templateId) {
        log.debug("Finding MessageTemplateRevisions by template id: {}", templateId);
        return messageTemplateRevisionRepository.findByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public long countByTemplateId(UUID templateId) {
        log.debug("Counting MessageTemplateRevisions by template id: {}", templateId);
        return messageTemplateRevisionRepository.countByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplateRevision> findByEditedByUserId(UUID editedByUserId) {
        log.debug("Finding MessageTemplateRevisions by edited by user id: {}", editedByUserId);
        return messageTemplateRevisionRepository.findByEditedById(editedByUserId);
    }

    @Transactional(readOnly = true)
    public Optional<MessageTemplateRevision> findByTemplateIdAndRevisionNumber(UUID templateId, Integer revisionNumber) {
        log.debug("Finding MessageTemplateRevision by template id: {} and revision number: {}", templateId, revisionNumber);
        return messageTemplateRevisionRepository.findByTemplateIdAndRevisionNumber(templateId, revisionNumber);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplateRevision> findByTemplateIdOrderByRevisionNumberDesc(UUID templateId) {
        log.debug("Finding MessageTemplateRevisions by template id: {} ordered by revision number desc", templateId);
        return messageTemplateRevisionRepository.findByTemplateIdOrderByRevisionNumberDesc(templateId);
    }

    @Transactional(readOnly = true)
    public Optional<MessageTemplateRevision> getLatestRevision(UUID templateId) {
        log.debug("Getting latest revision for template id: {}", templateId);
        return messageTemplateRevisionRepository.findFirstByTemplateIdOrderByRevisionNumberDesc(templateId);
    }

    @Transactional(readOnly = true)
    public Optional<MessageTemplateRevision> getOriginalRevision(UUID templateId) {
        log.debug("Getting original revision for template id: {}", templateId);
        return messageTemplateRevisionRepository.findByTemplateIdAndRevisionNumber(templateId, 1);
    }

    @Transactional(readOnly = true)
    public boolean existsByTemplateIdAndRevisionNumber(UUID templateId, Integer revisionNumber) {
        log.debug("Checking if revision exists for template id: {} and revision number: {}", templateId, revisionNumber);
        return messageTemplateRevisionRepository.existsByTemplateIdAndRevisionNumber(templateId, revisionNumber);
    }

    @Transactional(readOnly = true)
    public List<MessageTemplateRevision> findRevisionsBetweenNumbers(UUID templateId, Integer minRevision, Integer maxRevision) {
        log.debug("Finding revisions between {} and {} for template id: {}", minRevision, maxRevision, templateId);
        return messageTemplateRevisionRepository.findByTemplateIdAndRevisionNumberBetweenOrderByRevisionNumber(templateId, minRevision, maxRevision);
    }

    @Transactional(readOnly = true)
    public Integer getNextRevisionNumber(UUID templateId) {
        log.debug("Getting next revision number for template id: {}", templateId);
        Optional<Integer> maxRevision = messageTemplateRevisionRepository.findMaxRevisionNumberByTemplateId(templateId);
        return maxRevision.map(max -> max + 1).orElse(1);
    }

    @Transactional
    public MessageTemplateRevision createRevisionFromTemplate(UUID templateId, String content, UUID editedByUserId) {
        return createRevisionFromTemplate(templateId, content, editedByUserId, RevisionType.EDIT);
    }

    @Transactional
    public MessageTemplateRevision createRevisionFromTemplate(UUID templateId, String content, UUID editedByUserId, RevisionType revisionType) {
        log.debug("Creating revision from template id: {} with content by user: {} of type: {}", templateId, editedByUserId, revisionType);

        MessageTemplate template = messageTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("MessageTemplate not found with ID: " + templateId));

        User editor = userRepository.findById(editedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + editedByUserId));

        Integer nextRevisionNumber = getNextRevisionNumber(templateId);

        MessageTemplateRevision revision = MessageTemplateRevision.builder()
                .company(template.getCompany()) // Set the company from the template
                .template(template)
                .revisionNumber(nextRevisionNumber)
                .content(content)
                .editedBy(editor)
                .revisionType(revisionType)
                .build();

        MessageTemplateRevision savedRevision = messageTemplateRevisionRepository.save(revision);
        log.debug("Revision created from template with id: {} and type: {}", savedRevision.getId(), revisionType);

        return savedRevision;
    }

    /**
     * Cria uma nova revisão para um template com metadados de IA
     */
    public MessageTemplateRevision createAIEnhancementRevision(
            UUID templateId, 
            String enhancedContent, 
            UUID editedByUserId,
            UUID aiAgentId,
            String enhancementType,
            Integer tokensUsed,
            Integer creditsConsumed,
            String modelUsed,
            String explanation) {
        
        log.debug("Creating AI enhancement revision for template: {}", templateId);
        
        MessageTemplate template = messageTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("MessageTemplate not found with ID: " + templateId));

        User editor = userRepository.findById(editedByUserId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + editedByUserId));

        AIAgent aiAgent = aiAgentRepository.findById(aiAgentId)
                .orElseThrow(() -> new RuntimeException("AIAgent not found with ID: " + aiAgentId));

        Integer nextRevisionNumber = getNextRevisionNumber(templateId);

        MessageTemplateRevision revision = MessageTemplateRevision.builder()
                .company(template.getCompany())
                .template(template)
                .revisionNumber(nextRevisionNumber)
                .content(enhancedContent)
                .editedBy(editor)
                .revisionType(RevisionType.AI_ENHANCEMENT)
                // AI metadata
                .aiAgent(aiAgent)
                .aiEnhancementType(enhancementType)
                .aiTokensUsed(tokensUsed)
                .aiCreditsConsumed(creditsConsumed)
                .aiModelUsed(modelUsed)
                .aiExplanation(explanation)
                .build();

        MessageTemplateRevision savedRevision = messageTemplateRevisionRepository.save(revision);
        log.debug("AI enhancement revision created with id: {} for template: {}", savedRevision.getId(), templateId);

        return savedRevision;
    }
}