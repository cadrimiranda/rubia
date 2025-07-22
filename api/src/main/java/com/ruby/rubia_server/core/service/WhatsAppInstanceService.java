package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import com.ruby.rubia_server.core.repository.WhatsAppInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppInstanceService {

    private final WhatsAppInstanceRepository whatsappInstanceRepository;
    
    public Optional<WhatsAppInstance> findByPhoneNumber(String phoneNumber) {
        return whatsappInstanceRepository.findByPhoneNumberAndIsActiveTrue(phoneNumber);
    }
    
    public Optional<WhatsAppInstance> findById(UUID id) {
        return whatsappInstanceRepository.findById(id);
    }

    public List<WhatsAppInstance> findByCompany(Company company) {
        return whatsappInstanceRepository.findByCompanyAndIsActiveTrue(company);
    }

    public Optional<WhatsAppInstance> findPrimaryByCompany(Company company) {
        return whatsappInstanceRepository.findByCompanyAndIsPrimaryTrueAndIsActiveTrue(company);
    }

    public boolean hasConfiguredInstance(Company company) {
        List<WhatsAppInstance> instances = findByCompany(company);
        return instances.stream()
                .anyMatch(instance -> instance.isConfigured() && instance.getIsActive());
    }

    public boolean hasConnectedInstance(Company company) {
        List<WhatsAppInstance> instances = findByCompany(company);
        return instances.stream()
                .anyMatch(instance -> instance.isConnected() && instance.getIsActive());
    }

    public List<WhatsAppInstance> findNeedingConfiguration(Company company) {
        List<WhatsAppInstance> instances = findByCompany(company);
        return instances.stream()
                .filter(instance -> instance.needsConfiguration() && instance.getIsActive())
                .toList();
    }

    @Transactional
    public WhatsAppInstance createInstance(Company company, String phoneNumber, String displayName) {
        log.info("Creating new WhatsApp instance for company {} with phone {}", company.getId(), phoneNumber);
        
        // Validate phone number format
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format. Must be in format +5511999999999 or 5511999999999");
        }
        
        // Check if this is the first instance for the company
        List<WhatsAppInstance> existingInstances = findByCompany(company);
        boolean isFirstInstance = existingInstances.isEmpty();

        WhatsAppInstance instance = WhatsAppInstance.builder()
                .company(company)
                .phoneNumber(phoneNumber)
                .displayName(displayName)
                .isPrimary(isFirstInstance) // First instance becomes primary
                .status(WhatsAppInstanceStatus.NOT_CONFIGURED)
                .isActive(true)
                .build();

        return whatsappInstanceRepository.save(instance);
    }
    
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Remove any spaces and formatting
        String cleanPhone = phoneNumber.replaceAll("\\s+", "");
        
        // Must match Brazilian format: +5511999999999 or 5511999999999 (11-15 digits total)
        return cleanPhone.matches("^\\+?[1-9]\\d{10,14}$");
    }

    @Transactional
    public WhatsAppInstance updateInstanceStatus(UUID instanceId, WhatsAppInstanceStatus status) {
        WhatsAppInstance instance = whatsappInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp instance not found"));

        instance.setStatus(status);
        instance.setLastStatusCheck(LocalDateTime.now());

        if (status == WhatsAppInstanceStatus.CONNECTED) {
            instance.setLastConnectedAt(LocalDateTime.now());
            instance.setErrorMessage(null);
        }

        log.info("Updated WhatsApp instance {} status to {}", instanceId, status);
        return whatsappInstanceRepository.save(instance);
    }

    @Transactional
    public WhatsAppInstance updateInstanceConfiguration(UUID instanceId, String instanceIdValue, String accessToken) {
        WhatsAppInstance instance = whatsappInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp instance not found"));

        instance.setInstanceId(instanceIdValue);
        instance.setAccessToken(accessToken);
        instance.setStatus(WhatsAppInstanceStatus.CONFIGURING);

        log.info("Updated WhatsApp instance {} configuration", instanceId);
        return whatsappInstanceRepository.save(instance);
    }

    @Transactional
    public void markInstanceAsError(UUID instanceId, String errorMessage) {
        WhatsAppInstance instance = whatsappInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp instance not found"));

        instance.setStatus(WhatsAppInstanceStatus.ERROR);
        instance.setErrorMessage(errorMessage);
        instance.setLastStatusCheck(LocalDateTime.now());

        log.error("Marked WhatsApp instance {} as error: {}", instanceId, errorMessage);
        whatsappInstanceRepository.save(instance);
    }

    @Transactional
    public void setPrimaryInstance(UUID instanceId) {
        WhatsAppInstance instance = whatsappInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp instance not found"));

        // Remove primary flag from all other instances of the same company
        List<WhatsAppInstance> companyInstances = findByCompany(instance.getCompany());
        companyInstances.forEach(inst -> {
            if (!inst.getId().equals(instanceId)) {
                inst.setIsPrimary(false);
            }
        });
        whatsappInstanceRepository.saveAll(companyInstances);

        // Set this instance as primary
        instance.setIsPrimary(true);
        whatsappInstanceRepository.save(instance);

        log.info("Set WhatsApp instance {} as primary for company {}", 
                instanceId, instance.getCompany().getId());
    }

    @Transactional
    public void deactivateInstance(UUID instanceId) {
        WhatsAppInstance instance = whatsappInstanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("WhatsApp instance not found"));

        instance.setIsActive(false);
        instance.setStatus(WhatsAppInstanceStatus.SUSPENDED);

        // If this was the primary instance, make another instance primary
        if (instance.getIsPrimary()) {
            List<WhatsAppInstance> activeInstances = findByCompany(instance.getCompany());
            activeInstances.stream()
                    .filter(inst -> inst.getIsActive() && !inst.getId().equals(instanceId))
                    .findFirst()
                    .ifPresent(inst -> {
                        inst.setIsPrimary(true);
                        whatsappInstanceRepository.save(inst);
                    });
        }

        whatsappInstanceRepository.save(instance);
        log.info("Deactivated WhatsApp instance {}", instanceId);
    }

    public Optional<WhatsAppInstance> findByInstanceId(String instanceId) {
        return whatsappInstanceRepository.findByInstanceIdAndIsActiveTrue(instanceId);
    }
}