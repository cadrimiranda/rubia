package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.dto.CampaignDTO;
import com.ruby.rubia_server.core.dto.CreateCampaignDTO;
import com.ruby.rubia_server.core.dto.UpdateCampaignDTO;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.service.CampaignService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaigns")
@Slf4j
public class CampaignController extends BaseCompanyEntityController<Campaign, CreateCampaignDTO, UpdateCampaignDTO, CampaignDTO> {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService, CompanyContextUtil companyContextUtil) {
        super(campaignService, companyContextUtil);
        this.campaignService = campaignService;
    }

    @Override
    protected String getEntityName() {
        return "Campaign";
    }

    @Override
    protected CampaignDTO convertToDTO(Campaign campaign) {
        return CampaignDTO.builder()
                .id(campaign.getId())
                .companyId(campaign.getCompany().getId())
                .companyName(campaign.getCompany().getName())
                .name(campaign.getName())
                .description(campaign.getDescription())
                .status(campaign.getStatus())
                .createdByUserId(campaign.getCreatedBy() != null ? campaign.getCreatedBy().getId() : null)
                .createdByUserName(campaign.getCreatedBy() != null ? campaign.getCreatedBy().getName() : null)
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .targetAudienceDescription(campaign.getTargetAudienceDescription())
                .initialMessageTemplateId(campaign.getInitialMessageTemplate() != null ? campaign.getInitialMessageTemplate().getId() : null)
                .initialMessageTemplateName(campaign.getInitialMessageTemplate() != null ? campaign.getInitialMessageTemplate().getName() : null)
                .totalContacts(campaign.getTotalContacts())
                .contactsReached(campaign.getContactsReached())
                .sourceSystemName(campaign.getSourceSystemName())
                .sourceSystemId(campaign.getSourceSystemId())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateCampaignDTO createDTO) {
        return createDTO.getCompanyId();
    }

    // Endpoints específicos da entidade
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CampaignDTO>> findByStatus(@PathVariable CampaignStatus status) {
        log.debug("Finding campaigns by status via API: {}", status);
        
        List<Campaign> campaigns = campaignService.findByStatus(status);
        List<CampaignDTO> responseDTOs = campaigns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/status/{status}")
    public ResponseEntity<List<CampaignDTO>> findByCompanyAndStatus(
            @PathVariable UUID companyId, 
            @PathVariable CampaignStatus status) {
        log.debug("Finding campaigns by company: {} and status: {}", companyId, status);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<Campaign> campaigns = campaignService.findByCompanyIdAndStatus(companyId, status);
        List<CampaignDTO> responseDTOs = campaigns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }

    @GetMapping("/company/{companyId}/count/status/{status}")
    public ResponseEntity<Long> countByCompanyAndStatus(
            @PathVariable UUID companyId, 
            @PathVariable CampaignStatus status) {
        log.debug("Counting campaigns by company: {} and status: {}", companyId, status);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long count = campaignService.countByCompanyIdAndStatus(companyId, status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/company/{companyId}/exists")
    public ResponseEntity<Boolean> checkExists(
            @PathVariable UUID companyId, 
            @RequestParam String name) {
        log.debug("Checking if campaign exists with name: {} for company: {}", name, companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        boolean exists = campaignService.existsByNameAndCompanyId(name, companyId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/company/{companyId}/active")
    public ResponseEntity<List<CampaignDTO>> getActiveCampaignsByCompany(@PathVariable UUID companyId) {
        log.debug("Finding active campaigns for company: {}", companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<Campaign> campaigns = campaignService.getActiveCampaignsByCompanyId(companyId);
        List<CampaignDTO> responseDTOs = campaigns.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }
}