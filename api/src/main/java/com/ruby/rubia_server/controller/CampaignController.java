package com.ruby.rubia_server.controller;

import com.ruby.rubia_server.core.base.BaseCompanyEntityController;
import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.service.CampaignProcessingService;
import com.ruby.rubia_server.core.service.CampaignService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import com.ruby.rubia_server.dto.campaign.CreateCampaignDTO;
import com.ruby.rubia_server.dto.campaign.UpdateCampaignDTO;
import com.ruby.rubia_server.dto.campaign.CampaignDTO;
import com.ruby.rubia_server.dto.campaign.ProcessCampaignDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@Slf4j
public class CampaignController extends BaseCompanyEntityController<Campaign, CreateCampaignDTO, UpdateCampaignDTO, CampaignDTO> {

    private final CampaignProcessingService campaignProcessingService;

    public CampaignController(CampaignService campaignService, 
                            CampaignProcessingService campaignProcessingService,
                            CompanyContextUtil companyContextUtil) {
        super((BaseCompanyEntityService<Campaign, CreateCampaignDTO, UpdateCampaignDTO>) campaignService, companyContextUtil);
        this.campaignProcessingService = campaignProcessingService;
    }

    @Override
    protected String getEntityName() {
        return "Campaign";
    }

    @Override
    protected CampaignDTO convertToDTO(Campaign campaign) {
        return CampaignDTO.builder()
            .id(campaign.getId())
            .name(campaign.getName())
            .description(campaign.getDescription())
            .status(campaign.getStatus())
            .startDate(campaign.getStartDate())
            .endDate(campaign.getEndDate())
            .totalContacts(campaign.getTotalContacts())
            .contactsReached(campaign.getContactsReached())
            .sourceSystemName(campaign.getSourceSystemName())
            .sourceSystemId(campaign.getSourceSystemId())
            .companyId(campaign.getCompany().getId())
            .createdBy(campaign.getCreatedBy() != null ? campaign.getCreatedBy().getName() : null)
            .initialMessageTemplateId(campaign.getInitialMessageTemplate() != null ? 
                campaign.getInitialMessageTemplate().getId() : null)
            .createdAt(campaign.getCreatedAt())
            .updatedAt(campaign.getUpdatedAt())
            .build();
    }

    @Override
    protected UUID getCompanyIdFromDTO(CreateCampaignDTO createDTO) {
        return createDTO.getCompanyId();
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processExcelAndCreateCampaign(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") @Valid ProcessCampaignDTO processCampaignDTO) {
        
        try {
            log.info("Processando campanha: {} com arquivo: {}", 
                processCampaignDTO.getName(), file.getOriginalFilename());

            // Validar arquivo
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Arquivo não pode estar vazio"));
            }

            // Validar formato do arquivo
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".csv"))) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Apenas arquivos .xlsx e .csv são suportados"));
            }

            // Validar tamanho do arquivo (10MB max)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Arquivo muito grande. Tamanho máximo: 10MB"));
            }

            // Processar campanha
            CampaignProcessingService.CampaignProcessingResult result = 
                campaignProcessingService.processExcelAndCreateCampaign(
                    file,
                    processCampaignDTO.getName(),
                    processCampaignDTO.getDescription(),
                    processCampaignDTO.getCompanyId(),
                    processCampaignDTO.getUserId(),
                    processCampaignDTO.getStartDate(),
                    processCampaignDTO.getEndDate(),
                    processCampaignDTO.getSourceSystem(),
                    processCampaignDTO.getTemplateIds()
                );

            // Construir resposta de sucesso
            Map<String, Object> response = Map.of(
                "success", true,
                "campaign", convertToDTO(result.getCampaign()),
                "statistics", Map.of(
                    "processed", result.getProcessed(),
                    "created", result.getCreated(),
                    "duplicates", result.getDuplicates(),
                    "errors", result.getErrors().size()
                ),
                "errors", result.getErrors()
            );

            log.info("Campanha {} processada com sucesso. Contatos criados: {}", 
                result.getCampaign().getName(), result.getCreated());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao processar campanha: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
                
        } catch (IOException e) {
            log.warn("Erro ao processar arquivo: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Erro ao processar arquivo: " + e.getMessage()));
                
        } catch (Exception e) {
            log.error("Erro interno ao processar campanha: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor ao processar campanha"));
        }
    }

    @GetMapping("/company/{companyId}/active")
    public ResponseEntity<List<CampaignDTO>> getActiveCampaigns(@PathVariable UUID companyId) {
        try {
            log.debug("Buscando campanhas ativas para empresa: {}", companyId);
            
            List<Campaign> activeCampaigns = ((CampaignService) service).findActiveCampaignsByCompany(companyId);
            List<CampaignDTO> response = activeCampaigns.stream()
                .map(this::convertToDTO)
                .toList();
                
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao buscar campanhas ativas: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<?> pauseCampaign(@PathVariable UUID id) {
        try {
            log.info("Pausando campanha: {}", id);
            
            Campaign campaign = ((CampaignService) service).pauseCampaign(id);
            return ResponseEntity.ok(convertToDTO(campaign));
            
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao pausar campanha {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Erro interno ao pausar campanha {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor"));
        }
    }

    @PutMapping("/{id}/resume")
    public ResponseEntity<?> resumeCampaign(@PathVariable UUID id) {
        try {
            log.info("Resumindo campanha: {}", id);
            
            Campaign campaign = ((CampaignService) service).resumeCampaign(id);
            return ResponseEntity.ok(convertToDTO(campaign));
            
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao resumir campanha {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Erro interno ao resumir campanha {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor"));
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeCampaign(@PathVariable UUID id) {
        try {
            log.info("Marcando campanha como completa: {}", id);
            
            Campaign campaign = ((CampaignService) service).completeCampaign(id);
            return ResponseEntity.ok(convertToDTO(campaign));
            
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao completar campanha {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Erro interno ao completar campanha {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor"));
        }
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<?> getCampaignStatistics(@PathVariable UUID id) {
        try {
            log.debug("Buscando estatísticas da campanha: {}", id);
            
            Map<String, Object> statistics = ((CampaignService) service).getCampaignStatistics(id);
            return ResponseEntity.ok(statistics);
            
        } catch (IllegalArgumentException e) {
            log.warn("Erro ao buscar estatísticas da campanha {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
                
        } catch (Exception e) {
            log.error("Erro interno ao buscar estatísticas da campanha {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor"));
        }
    }
}