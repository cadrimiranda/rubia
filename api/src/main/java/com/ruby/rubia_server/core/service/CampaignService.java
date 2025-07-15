package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.dto.campaign.CreateCampaignDTO;
import com.ruby.rubia_server.dto.campaign.UpdateCampaignDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class CampaignService extends BaseCompanyEntityService<Campaign, CreateCampaignDTO, UpdateCampaignDTO> {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final CampaignContactService campaignContactService;

    public CampaignService(CampaignRepository campaignRepository,
                          CompanyRepository companyRepository,
                          UserRepository userRepository,
                          MessageTemplateRepository messageTemplateRepository,
                          CampaignContactService campaignContactService,
                          EntityRelationshipValidator relationshipValidator) {
        super(campaignRepository, companyRepository, relationshipValidator);
        this.campaignRepository = campaignRepository;
        this.userRepository = userRepository;
        this.messageTemplateRepository = messageTemplateRepository;
        this.campaignContactService = campaignContactService;
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

    @Transactional(readOnly = true)
    public List<Campaign> findActiveCampaignsByCompany(UUID companyId) {
        log.debug("Finding active campaigns for company: {}", companyId);
        return campaignRepository.findByCompanyIdAndStatus(companyId, CampaignStatus.ACTIVE);
    }

    @Transactional
    public Campaign pauseCampaign(UUID campaignId) {
        Campaign campaign = findById(campaignId);
        
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new IllegalArgumentException("Apenas campanhas ativas podem ser pausadas");
        }
        
        campaign.setStatus(CampaignStatus.PAUSED);
        Campaign saved = campaignRepository.save(campaign);
        
        log.info("Campanha {} pausada com sucesso", campaignId);
        return saved;
    }

    @Transactional
    public Campaign resumeCampaign(UUID campaignId) {
        Campaign campaign = findById(campaignId);
        
        if (campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new IllegalArgumentException("Apenas campanhas pausadas podem ser retomadas");
        }
        
        campaign.setStatus(CampaignStatus.ACTIVE);
        Campaign saved = campaignRepository.save(campaign);
        
        log.info("Campanha {} retomada com sucesso", campaignId);
        return saved;
    }

    @Transactional
    public Campaign completeCampaign(UUID campaignId) {
        Campaign campaign = findById(campaignId);
        
        if (campaign.getStatus() == CampaignStatus.COMPLETED || campaign.getStatus() == CampaignStatus.CANCELED) {
            throw new IllegalArgumentException("Campanha já está finalizada");
        }
        
        campaign.setStatus(CampaignStatus.COMPLETED);
        Campaign saved = campaignRepository.save(campaign);
        
        log.info("Campanha {} marcada como completa", campaignId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCampaignStatistics(UUID campaignId) {
        Campaign campaign = findById(campaignId);
        
        // Buscar estatísticas dos contatos da campanha
        List<CampaignContact> contacts = campaignContactService.findByCampaignId(campaignId);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("campaignId", campaignId);
        statistics.put("campaignName", campaign.getName());
        statistics.put("status", campaign.getStatus());
        statistics.put("totalContacts", campaign.getTotalContacts());
        statistics.put("contactsReached", campaign.getContactsReached());
        
        // Calcular estatísticas detalhadas dos contatos
        long pendingCount = contacts.stream()
            .filter(c -> c.getStatus() == com.ruby.rubia_server.core.enums.CampaignContactStatus.PENDING)
            .count();
        
        long sentCount = contacts.stream()
            .filter(c -> c.getStatus() == com.ruby.rubia_server.core.enums.CampaignContactStatus.SENT)
            .count();
        
        long respondedCount = contacts.stream()
            .filter(c -> c.getStatus() == com.ruby.rubia_server.core.enums.CampaignContactStatus.RESPONDED)
            .count();
        
        long convertedCount = contacts.stream()
            .filter(c -> c.getStatus() == com.ruby.rubia_server.core.enums.CampaignContactStatus.CONVERTED)
            .count();
        
        long failedCount = contacts.stream()
            .filter(c -> c.getStatus() == com.ruby.rubia_server.core.enums.CampaignContactStatus.FAILED)
            .count();
        
        long optOutCount = contacts.stream()
            .filter(c -> c.getStatus() == com.ruby.rubia_server.core.enums.CampaignContactStatus.OPT_OUT)
            .count();
        
        statistics.put("contactStatistics", Map.of(
            "pending", pendingCount,
            "sent", sentCount,
            "responded", respondedCount,
            "converted", convertedCount,
            "failed", failedCount,
            "optOut", optOutCount
        ));
        
        // Calcular taxas de conversão
        if (sentCount > 0) {
            statistics.put("responseRate", (double) respondedCount / sentCount * 100);
            statistics.put("conversionRate", (double) convertedCount / sentCount * 100);
        } else {
            statistics.put("responseRate", 0.0);
            statistics.put("conversionRate", 0.0);
        }
        
        log.debug("Estatísticas calculadas para campanha {}", campaignId);
        return statistics;
    }
}