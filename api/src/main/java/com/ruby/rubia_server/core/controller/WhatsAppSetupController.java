package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.MessagingProvider;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import com.ruby.rubia_server.config.CompanyContextResolver;
import com.ruby.rubia_server.core.service.WhatsAppInstanceService;
import com.ruby.rubia_server.core.service.ZApiActivationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/whatsapp-setup")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppSetupController {

    private final WhatsAppInstanceService whatsappInstanceService;
    private final ZApiActivationService zapiActivationService;
    private final CompanyContextResolver companyContextResolver;

    @GetMapping("/status")
    public ResponseEntity<WhatsAppSetupStatusResponse> getSetupStatus() {
        Company company = getCurrentCompany();
        
        List<WhatsAppInstance> instances = whatsappInstanceService.findByCompany(company);
        boolean hasConfiguredInstance = whatsappInstanceService.hasConfiguredInstance(company);
        boolean hasConnectedInstance = whatsappInstanceService.hasConnectedInstance(company);
        
        return ResponseEntity.ok(WhatsAppSetupStatusResponse.builder()
            .requiresSetup(!hasConfiguredInstance)
            .hasConfiguredInstance(hasConfiguredInstance)
            .hasConnectedInstance(hasConnectedInstance)
            .totalInstances(instances.size())
            .maxAllowedInstances(company.getMaxWhatsappNumbers())
            .instances(instances.stream().map(this::mapToInstanceInfo).toList())
            .build());
    }

    @PostMapping("/create-instance")
    public ResponseEntity<WhatsAppInstance> createInstance(@RequestBody CreateInstanceRequest request) {
        try {
            Company company = getCurrentCompany();
            
            // Validate company limits
            List<WhatsAppInstance> existingInstances = whatsappInstanceService.findByCompany(company);
            if (existingInstances.size() >= company.getMaxWhatsappNumbers()) {
                return ResponseEntity.badRequest().build();
            }

            // Check if phone number already exists
            if (whatsappInstanceService.findByPhoneNumber(request.phoneNumber()).isPresent()) {
                return ResponseEntity.badRequest().build();
            }

            WhatsAppInstance instance = whatsappInstanceService.createInstance(
                company, 
                request.phoneNumber(), 
                request.displayName()
            );

            log.info("Created WhatsApp instance {} for company {}", instance.getId(), company.getSlug());
            return ResponseEntity.ok(instance);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("Invalid request to create instance: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating WhatsApp instance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{instanceId}/configure")
    public ResponseEntity<WhatsAppInstance> configureInstance(
            @PathVariable UUID instanceId, 
            @RequestBody ConfigureInstanceRequest request) {
        try {
            WhatsAppInstance instance = whatsappInstanceService.updateInstanceConfiguration(
                instanceId, 
                request.instanceId(), 
                request.accessToken()
            );

            log.info("Configured WhatsApp instance {} with external ID {}", instanceId, request.instanceId());
            return ResponseEntity.ok(instance);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request to configure instance {}: {}", instanceId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error configuring WhatsApp instance {}: {}", instanceId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{instanceId}/activate")
    public ResponseEntity<Map<String, Object>> activateInstance(@PathVariable UUID instanceId) {
        WhatsAppInstance instance = whatsappInstanceService.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("WhatsApp instance not found"));

        try {
            // Update instance status to activation process
            whatsappInstanceService.updateInstanceStatus(instanceId, WhatsAppInstanceStatus.AWAITING_QR_SCAN);
            
            // Get activation status from Z-API
            var status = zapiActivationService.getInstanceStatus();
            
            if (status.isConnected()) {
                whatsappInstanceService.updateInstanceStatus(instanceId, WhatsAppInstanceStatus.CONNECTED);
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "status", status,
                "instanceId", instanceId,
                "message", "Instance activation initiated"
            ));

        } catch (Exception e) {
            log.error("Error activating WhatsApp instance {}: {}", instanceId, e.getMessage(), e);
            whatsappInstanceService.markInstanceAsError(instanceId, e.getMessage());
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "error", e.getMessage(),
                "instanceId", instanceId
            ));
        }
    }

    @PostMapping("/{instanceId}/set-primary")
    public ResponseEntity<Map<String, Object>> setPrimaryInstance(@PathVariable UUID instanceId) {
        whatsappInstanceService.setPrimaryInstance(instanceId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Primary instance updated"
        ));
    }

    @DeleteMapping("/{instanceId}")
    public ResponseEntity<Map<String, Object>> deactivateInstance(@PathVariable UUID instanceId) {
        whatsappInstanceService.deactivateInstance(instanceId);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Instance deactivated"
        ));
    }

    @GetMapping("/providers")
    public ResponseEntity<List<MessagingProviderInfo>> getAvailableProviders() {
        List<MessagingProviderInfo> providers = List.of(
            new MessagingProviderInfo(MessagingProvider.Z_API, "Z-API", "WhatsApp via Z-API", true),
            new MessagingProviderInfo(MessagingProvider.TWILIO, "Twilio", "WhatsApp Business via Twilio", false),
            new MessagingProviderInfo(MessagingProvider.WHATSAPP_BUSINESS_API, "WhatsApp Business API", "Direct WhatsApp Business API", false)
        );
        
        return ResponseEntity.ok(providers);
    }

    private Company getCurrentCompany() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return companyContextResolver.resolveCompany(request)
            .orElseThrow(() -> new IllegalStateException("Company not found in request context"));
    }

    private WhatsAppInstanceInfo mapToInstanceInfo(WhatsAppInstance instance) {
        return WhatsAppInstanceInfo.builder()
            .id(instance.getId())
            .phoneNumber(instance.getPhoneNumber())
            .displayName(instance.getDisplayName())
            .provider(instance.getProvider())
            .status(instance.getStatus())
            .isPrimary(instance.getIsPrimary())
            .isActive(instance.getIsActive())
            .lastConnectedAt(instance.getLastConnectedAt())
            .createdAt(instance.getCreatedAt())
            .errorMessage(instance.getErrorMessage())
            .build();
    }

    // Request/Response DTOs
    public record CreateInstanceRequest(String phoneNumber, String displayName) {}
    public record ConfigureInstanceRequest(String instanceId, String accessToken) {}
    public record MessagingProviderInfo(MessagingProvider provider, String name, String description, boolean available) {}

    @lombok.Builder
    @lombok.Data
    public static class WhatsAppSetupStatusResponse {
        private boolean requiresSetup;
        private boolean hasConfiguredInstance;
        private boolean hasConnectedInstance;
        private int totalInstances;
        private int maxAllowedInstances;
        private List<WhatsAppInstanceInfo> instances;
    }

    @lombok.Builder
    @lombok.Data
    public static class WhatsAppInstanceInfo {
        private UUID id;
        private String phoneNumber;
        private String displayName;
        private MessagingProvider provider;
        private WhatsAppInstanceStatus status;
        private Boolean isPrimary;
        private Boolean isActive;
        private java.time.LocalDateTime lastConnectedAt;
        private java.time.LocalDateTime createdAt;
        private String errorMessage;
    }
}