package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.CreateCampaignDTO;
import com.ruby.rubia_server.core.dto.UpdateCampaignDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class CampaignService extends BaseCompanyEntityService<Campaign, CreateCampaignDTO, UpdateCampaignDTO> {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final MessageTemplateRepository messageTemplateRepository;

    public CampaignService(CampaignRepository campaignRepository,
                          CompanyRepository companyRepository,
                          UserRepository userRepository,
                          MessageTemplateRepository messageTemplateRepository,
                          EntityRelationshipValidator relationshipValidator) {
        super(campaignRepository, companyRepository, relationshipValidator);
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.messageTemplateRepository = messageTemplateRepository;
    }

    @Override
    protected String getEntityName() {
        return "Campaign";
    }

    @Override
    protected Campaign buildEntityFromDTO(CreateCampaignDTO createDTO) {
        Campaign.CampaignBuilder builder = Campaign.builder()
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .status(createDTO.getStatus())
                .startDate(createDTO.getStartDate())
                .endDate(createDTO.getEndDate())
                .targetAudienceDescription(createDTO.getTargetAudienceDescription())
                .totalContacts(createDTO.getTotalContacts())
                .sourceSystemName(createDTO.getSourceSystemName())
                .sourceSystemId(createDTO.getSourceSystemId());

        // Handle optional relationships
        if (createDTO.getCreatedByUserId() != null) {
            User user = userRepository.findById(createDTO.getCreatedByUserId())
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + createDTO.getCreatedByUserId()));
            builder.createdBy(user);
        }

        if (createDTO.getInitialMessageTemplateId() != null) {
            MessageTemplate template = messageTemplateRepository.findById(createDTO.getInitialMessageTemplateId())
                    .orElseThrow(() -> new RuntimeException("Message template not found with ID: " + createDTO.getInitialMessageTemplateId()));
            builder.initialMessageTemplate(template);
        }

        return builder.build();
    }

    @Override
    protected void updateEntityFromDTO(Campaign campaign, UpdateCampaignDTO updateDTO) {
        if (updateDTO.getName() != null) {
            campaign.setName(updateDTO.getName());
        }
        if (updateDTO.getDescription() != null) {
            campaign.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getStatus() != null) {
            campaign.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getStartDate() != null) {
            campaign.setStartDate(updateDTO.getStartDate());
        }
        if (updateDTO.getEndDate() != null) {
            campaign.setEndDate(updateDTO.getEndDate());
        }
        if (updateDTO.getTargetAudienceDescription() != null) {
            campaign.setTargetAudienceDescription(updateDTO.getTargetAudienceDescription());
        }
        if (updateDTO.getTotalContacts() != null) {
            campaign.setTotalContacts(updateDTO.getTotalContacts());
        }
        if (updateDTO.getContactsReached() != null) {
            campaign.setContactsReached(updateDTO.getContactsReached());
        }
        if (updateDTO.getSourceSystemName() != null) {
            campaign.setSourceSystemName(updateDTO.getSourceSystemName());
        }
        if (updateDTO.getSourceSystemId() != null) {
            campaign.setSourceSystemId(updateDTO.getSourceSystemId());
        }
    }

    @Override
    protected Company getCompanyFromDTO(CreateCampaignDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    // Métodos específicos da entidade
    @Transactional(readOnly = true)
    public List<Campaign> findByStatus(CampaignStatus status) {
        log.debug("Finding campaigns by status: {}", status);
        return campaignRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Campaign> findByCompanyIdAndStatus(UUID companyId, CampaignStatus status) {
        log.debug("Finding campaigns by company: {} and status: {}", companyId, status);
        return campaignRepository.findByCompanyIdAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public long countByCompanyIdAndStatus(UUID companyId, CampaignStatus status) {
        log.debug("Counting campaigns by company: {} and status: {}", companyId, status);
        return campaignRepository.countByCompanyIdAndStatus(companyId, status);
    }

    @Transactional(readOnly = true)
    public boolean existsByNameAndCompanyId(String name, UUID companyId) {
        log.debug("Checking if campaign exists with name: {} for company: {}", name, companyId);
        return campaignRepository.existsByNameAndCompanyId(name, companyId);
    }

    @Transactional(readOnly = true)
    public List<Campaign> getActiveCampaignsByCompanyId(UUID companyId) {
        log.debug("Finding active campaigns for company: {}", companyId);
        return campaignRepository.findByCompanyIdAndStatus(companyId, CampaignStatus.ACTIVE);
    }
}