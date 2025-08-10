package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateCampaignContactDTO;
import com.ruby.rubia_server.core.dto.UpdateCampaignContactDTO;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.repository.CampaignContactRepository;
import com.ruby.rubia_server.core.repository.CampaignRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CampaignContactService {

    private final CampaignContactRepository campaignContactRepository;
    private final CampaignRepository campaignRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public CampaignContact create(CreateCampaignContactDTO createDTO) {
        log.debug("Creating CampaignContact with data: {}", createDTO);

        // Validate and get Campaign
        Campaign campaign = campaignRepository.findById(createDTO.getCampaignId())
                .orElseThrow(() -> new RuntimeException("Campaign not found with ID: " + createDTO.getCampaignId()));

        // Validate and get Customer
        Customer customer = customerRepository.findById(createDTO.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + createDTO.getCustomerId()));

        // Build CampaignContact
        CampaignContact campaignContact = CampaignContact.builder()
                .campaign(campaign)
                .customer(customer)
                .status(createDTO.getStatus())
                .messageSentAt(createDTO.getMessageSentAt())
                .responseReceivedAt(createDTO.getResponseReceivedAt())
                .notes(createDTO.getNotes())
                .build();

        CampaignContact savedContact = campaignContactRepository.save(campaignContact);
        log.debug("CampaignContact created successfully with id: {}", savedContact.getId());

        return savedContact;
    }

    @Transactional(readOnly = true)
    public Optional<CampaignContact> findById(UUID id) {
        log.debug("Finding CampaignContact by id: {}", id);
        return campaignContactRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CampaignContact> findByIdWithRelations(UUID id) {
        log.debug("Finding CampaignContact with Customer, Campaign, and MessageTemplate by id: {}", id);
        return campaignContactRepository.findByIdWithRelations(id);
    }

    @Transactional(readOnly = true)
    public Page<CampaignContact> findAll(Pageable pageable) {
        log.debug("Finding all CampaignContacts with pageable: {}", pageable);
        return campaignContactRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<CampaignContact> findByCampaignId(UUID campaignId) {
        log.debug("Finding CampaignContacts by campaign id: {}", campaignId);
        return campaignContactRepository.findByCampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<CampaignContact> findByCustomerId(UUID customerId) {
        log.debug("Finding CampaignContacts by customer id: {}", customerId);
        return campaignContactRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<CampaignContact> findByStatus(CampaignContactStatus status) {
        log.debug("Finding CampaignContacts by status: {}", status);
        return campaignContactRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<CampaignContact> findByCampaignIdAndStatus(UUID campaignId, CampaignContactStatus status) {
        log.debug("Finding CampaignContacts by campaign id: {} and status: {}", campaignId, status);
        return campaignContactRepository.findByCampaignIdAndStatus(campaignId, status);
    }

    @Transactional(readOnly = true)
    public List<CampaignContact> findByCustomerIdAndStatus(UUID customerId, CampaignContactStatus status) {
        log.debug("Finding CampaignContacts by customer id: {} and status: {}", customerId, status);
        return campaignContactRepository.findByCustomerIdAndStatus(customerId, status);
    }

    @Transactional(readOnly = true)
    public List<CampaignContact> findPendingByCustomerPhone(String customerPhone) {
        log.debug("Finding pending CampaignContacts by customer phone: {}", customerPhone);
        return campaignContactRepository.findByCustomer_PhoneAndStatus(customerPhone, CampaignContactStatus.PENDING);
    }

    @Transactional
    public void markAsSentByCustomerPhone(String customerPhone, String reason) {
        log.debug("Marking CampaignContacts as SENT for phone: {} (reason: {})", customerPhone, reason);
        List<CampaignContact> pendingContacts = findPendingByCustomerPhone(customerPhone);
        
        for (CampaignContact contact : pendingContacts) {
            contact.setStatus(CampaignContactStatus.SENT);
            contact.setMessageSentAt(LocalDateTime.now());
            campaignContactRepository.save(contact);
            log.info("Marked CampaignContact {} as SENT due to manual sending", contact.getId());
        }
    }

    @Transactional
    public Optional<CampaignContact> update(UUID id, UpdateCampaignContactDTO updateDTO) {
        log.debug("Updating CampaignContact with id: {}", id);

        Optional<CampaignContact> optionalContact = campaignContactRepository.findById(id);
        if (optionalContact.isEmpty()) {
            log.warn("CampaignContact not found with id: {}", id);
            return Optional.empty();
        }

        CampaignContact campaignContact = optionalContact.get();

        // Update fields
        if (updateDTO.getStatus() != null) {
            campaignContact.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getMessageSentAt() != null) {
            campaignContact.setMessageSentAt(updateDTO.getMessageSentAt());
        }
        if (updateDTO.getResponseReceivedAt() != null) {
            campaignContact.setResponseReceivedAt(updateDTO.getResponseReceivedAt());
        }
        if (updateDTO.getNotes() != null) {
            campaignContact.setNotes(updateDTO.getNotes());
        }

        CampaignContact updatedContact = campaignContactRepository.save(campaignContact);
        log.debug("CampaignContact updated successfully with id: {}", updatedContact.getId());

        return Optional.of(updatedContact);
    }

    @Transactional
    public boolean deleteById(UUID id) {
        log.debug("Deleting CampaignContact with id: {}", id);

        if (!campaignContactRepository.existsById(id)) {
            log.warn("CampaignContact not found with id: {}", id);
            return false;
        }

        campaignContactRepository.deleteById(id);
        log.debug("CampaignContact deleted successfully");
        return true;
    }

    @Transactional(readOnly = true)
    public long countByCampaignId(UUID campaignId) {
        log.debug("Counting CampaignContacts by campaign id: {}", campaignId);
        return campaignContactRepository.countByCampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public long countByCustomerId(UUID customerId) {
        log.debug("Counting CampaignContacts by customer id: {}", customerId);
        return campaignContactRepository.countByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public long countByCampaignIdAndStatus(UUID campaignId, CampaignContactStatus status) {
        log.debug("Counting CampaignContacts by campaign id: {} and status: {}", campaignId, status);
        return campaignContactRepository.countByCampaignIdAndStatus(campaignId, status);
    }

    @Transactional(readOnly = true)
    public boolean existsByCampaignIdAndCustomerId(UUID campaignId, UUID customerId) {
        log.debug("Checking if CampaignContact exists by campaign id: {} and customer id: {}", campaignId, customerId);
        return campaignContactRepository.existsByCampaignIdAndCustomerId(campaignId, customerId);
    }

    @Transactional
    public Optional<CampaignContact> markAsCompleted(UUID id) {
        log.debug("Marking CampaignContact as completed with id: {}", id);

        Optional<CampaignContact> optionalContact = campaignContactRepository.findById(id);
        if (optionalContact.isEmpty()) {
            log.warn("CampaignContact not found with id: {}", id);
            return Optional.empty();
        }

        CampaignContact campaignContact = optionalContact.get();
        campaignContact.setStatus(CampaignContactStatus.SENT);
        campaignContact.setMessageSentAt(LocalDateTime.now());

        CampaignContact updatedContact = campaignContactRepository.save(campaignContact);
        log.debug("CampaignContact marked as completed successfully with id: {}", updatedContact.getId());

        return Optional.of(updatedContact);
    }

    @Transactional
    public Optional<CampaignContact> markAsResponded(UUID id) {
        log.debug("Marking CampaignContact as responded with id: {}", id);

        Optional<CampaignContact> optionalContact = campaignContactRepository.findById(id);
        if (optionalContact.isEmpty()) {
            log.warn("CampaignContact not found with id: {}", id);
            return Optional.empty();
        }

        CampaignContact campaignContact = optionalContact.get();
        campaignContact.setStatus(CampaignContactStatus.RESPONDED);
        campaignContact.setResponseReceivedAt(LocalDateTime.now());

        CampaignContact updatedContact = campaignContactRepository.save(campaignContact);
        log.debug("CampaignContact marked as responded successfully with id: {}", updatedContact.getId());

        return Optional.of(updatedContact);
    }
}