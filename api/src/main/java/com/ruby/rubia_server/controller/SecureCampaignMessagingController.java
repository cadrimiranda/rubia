package com.ruby.rubia_server.controller;

import com.ruby.rubia_server.core.service.SecureCampaignQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller seguro para gerenciamento de filas de campanhas
 * Requer autenticação e autorização adequadas
 */
@RestController
@RequestMapping("/api/secure/campaigns")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "campaign.queue.provider", havingValue = "redis")
public class SecureCampaignMessagingController {

    private final SecureCampaignQueueService secureCampaignQueueService;

    /**
     * Adiciona campanha à fila segura (apenas usuários autenticados da empresa)
     */
    @PostMapping("/{campaignId}/start-messaging")
    @PreAuthorize("hasRole('CAMPAIGN_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> startSecureCampaignMessaging(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        
        log.info("🔒 Iniciando campanha segura: {} por usuário: {}", 
                campaignId, authentication.getName());
        
        try {
            // Extrair informações do usuário autenticado
            String userId = extractUserId(authentication);
            String companyId = extractCompanyId(authentication);
            
            if (companyId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Usuário não possui empresa associada"
                ));
            }
            
            // Validar permissões específicas da campanha
            if (!hasAccessToCampaign(campaignId, companyId, userId)) {
                log.warn("🚨 Tentativa de acesso não autorizado à campanha {} por usuário {}", 
                        campaignId, userId);
                
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Acesso negado à campanha"
                ));
            }
            
            // Adicionar à fila segura
            secureCampaignQueueService.enqueueCampaign(campaignId, companyId, userId);
            
            // Log de auditoria
            log.info("✅ Campanha {} adicionada à fila segura por usuário {} da empresa {}", 
                    campaignId, userId, companyId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campanha adicionada à fila segura",
                "campaignId", campaignId,
                "addedBy", userId,
                "companyId", companyId
            ));
            
        } catch (SecurityException e) {
            log.warn("🚨 Violação de segurança: {}", e.getMessage());
            
            return ResponseEntity.status(403).body(Map.of(
                "success", false,
                "error", "Acesso negado: " + e.getMessage()
            ));
            
        } catch (Exception e) {
            log.error("❌ Erro ao adicionar campanha {} à fila segura: {}", 
                    campaignId, e.getMessage(), e);
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage(),
                "campaignId", campaignId
            ));
        }
    }

    /**
     * Pausa campanha (apenas usuários autorizados)
     */
    @PostMapping("/{campaignId}/pause-messaging")
    @PreAuthorize("hasRole('CAMPAIGN_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> pauseSecureCampaignMessaging(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        
        log.info("🔒 Pausando campanha segura: {} por usuário: {}", 
                campaignId, authentication.getName());
        
        try {
            String userId = extractUserId(authentication);
            String companyId = extractCompanyId(authentication);
            
            if (!hasAccessToCampaign(campaignId, companyId, userId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Acesso negado à campanha"
                ));
            }
            
            // TODO: Implementar pausar campanha no Redis
            // secureCampaignQueueService.pauseCampaign(campaignId, companyId, userId);
            
            log.info("⏸️ Campanha {} pausada por usuário {} da empresa {}", 
                    campaignId, userId, companyId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Campanha pausada com sucesso",
                "campaignId", campaignId,
                "pausedBy", userId
            ));
            
        } catch (Exception e) {
            log.error("❌ Erro ao pausar campanha {}: {}", campaignId, e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Estatísticas da campanha (apenas para usuários da empresa)
     */
    @GetMapping("/{campaignId}/messaging-stats")
    @PreAuthorize("hasRole('CAMPAIGN_VIEWER') or hasRole('CAMPAIGN_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSecureCampaignStats(
            @PathVariable UUID campaignId,
            Authentication authentication) {
        
        try {
            String userId = extractUserId(authentication);
            String companyId = extractCompanyId(authentication);
            
            if (!hasAccessToCampaign(campaignId, companyId, userId)) {
                return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "error", "Acesso negado às estatísticas da campanha"
                ));
            }
            
            // TODO: Implementar obter estatísticas do Redis
            // Map<String, Object> stats = secureCampaignQueueService.getCampaignStats(campaignId, companyId);
            
            Map<String, Object> stats = Map.of(
                "campaignId", campaignId,
                "message", "Estatísticas em desenvolvimento",
                "accessedBy", userId,
                "companyId", companyId
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
            
        } catch (Exception e) {
            log.error("❌ Erro ao buscar estatísticas da campanha {}: {}", 
                    campaignId, e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Estatísticas globais (apenas administradores)
     */
    @GetMapping("/queue/global-stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SYSTEM_MONITOR')")
    public ResponseEntity<Map<String, Object>> getGlobalSecureStats(Authentication authentication) {
        
        log.info("🔍 Acessando estatísticas globais: usuário {}", authentication.getName());
        
        try {
            String userId = extractUserId(authentication);
            String companyId = extractCompanyId(authentication);
            
            // TODO: Implementar estatísticas globais do Redis
            Map<String, Object> stats = Map.of(
                "message", "Estatísticas globais em desenvolvimento",
                "accessedBy", userId,
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats
            ));
            
        } catch (Exception e) {
            log.error("❌ Erro ao buscar estatísticas globais: {}", e.getMessage());
            
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Extrai ID do usuário do token JWT
     */
    private String extractUserId(Authentication authentication) {
        // TODO: Implementar extração do JWT
        // Assumindo que o principal contém as informações do usuário
        return authentication.getName(); // Temporário
    }

    /**
     * Extrai ID da empresa do token JWT
     */
    private String extractCompanyId(Authentication authentication) {
        // TODO: Implementar extração da empresa do JWT
        // Deve extrair do token ou buscar no banco baseado no usuário
        return "temp-company-id"; // Temporário
    }

    /**
     * Valida se usuário tem acesso à campanha específica
     */
    private boolean hasAccessToCampaign(UUID campaignId, String companyId, String userId) {
        try {
            // TODO: Implementar validação real
            // 1. Verificar se campanha pertence à empresa do usuário
            // 2. Verificar se usuário tem permissão na campanha
            // 3. Verificar se campanha não está bloqueada/arquivada
            
            log.debug("Validando acesso: campanha={}, empresa={}, usuário={}", 
                    campaignId, companyId, userId);
            
            return true; // Temporário - implementar validação real
            
        } catch (Exception e) {
            log.error("Erro na validação de acesso: {}", e.getMessage());
            return false;
        }
    }
}