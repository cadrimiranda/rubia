package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    
    public CustomerDTO create(CreateCustomerDTO createDTO, UUID companyId) {
        log.info("Creating customer with phone: {} for company: {}", createDTO.getPhone(), companyId);
        
        // Buscar a empresa
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        
        // Normalizar telefone antes de verificar duplicação
        String normalizedPhone = normalizePhoneNumber(createDTO.getPhone());
        
        if (customerRepository.existsByPhoneAndCompanyId(normalizedPhone, companyId)) {
            throw new IllegalArgumentException("Cliente com telefone '" + normalizedPhone + "' já existe nesta empresa");
        }
        
        if (createDTO.getWhatsappId() != null && customerRepository.existsByWhatsappIdAndCompanyId(createDTO.getWhatsappId(), companyId)) {
            throw new IllegalArgumentException("Cliente com WhatsApp ID '" + createDTO.getWhatsappId() + "' já existe nesta empresa");
        }
        
        Customer customer = Customer.builder()
                .phone(normalizedPhone) // Usar o telefone normalizado
                .name(createDTO.getName())
                .whatsappId(createDTO.getWhatsappId())
                .profileUrl(createDTO.getProfileUrl())
                .isBlocked(createDTO.getIsBlocked() != null ? createDTO.getIsBlocked() : false)
                .sourceSystemName(createDTO.getSourceSystemName())
                .sourceSystemId(createDTO.getSourceSystemId())
                .importedAt(createDTO.getImportedAt())
                .birthDate(createDTO.getBirthDate())
                .lastDonationDate(createDTO.getLastDonationDate())
                .bloodType(createDTO.getBloodType())
                .height(createDTO.getHeight())
                .weight(createDTO.getWeight())
                .addressStreet(createDTO.getAddressStreet())
                .addressNumber(createDTO.getAddressNumber())
                .addressComplement(createDTO.getAddressComplement())
                .addressPostalCode(createDTO.getAddressPostalCode())
                .addressCity(createDTO.getAddressCity())
                .addressState(createDTO.getAddressState())
                .company(company)
                .build();
        
        customer.setNextEligibleDonationDate(createDTO.getNextEligibleDonationDate());
        
        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully with id: {}", saved.getId());
        
        return toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO findById(UUID id, UUID companyId) {
        log.debug("Finding customer by id: {} for company: {}", id, companyId);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
        return toDTO(customer);
    }
    
    public CustomerDTO update(UUID id, UpdateCustomerDTO updateDTO, UUID companyId) {
        log.info("Updating customer with id: {} for company: {}", id, companyId);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
        if (updateDTO.getPhone() != null) {
            // Normalizar telefone antes de verificar duplicação
            String normalizedPhone = normalizePhoneNumber(updateDTO.getPhone());
            if (!normalizedPhone.equals(customer.getPhone())) {
                if (customerRepository.existsByPhoneAndCompanyId(normalizedPhone, companyId)) {
                    throw new IllegalArgumentException("Cliente com telefone '" + normalizedPhone + "' já existe nesta empresa");
                }
            }
            customer.setPhone(normalizedPhone);
        }
        
        if (updateDTO.getName() != null) {
            customer.setName(updateDTO.getName());
        }
        
        if (updateDTO.getWhatsappId() != null) {
            if (!updateDTO.getWhatsappId().equals(customer.getWhatsappId())) {
                if (customerRepository.existsByWhatsappIdAndCompanyId(updateDTO.getWhatsappId(), companyId)) {
                    throw new IllegalArgumentException("Cliente com WhatsApp ID '" + updateDTO.getWhatsappId() + "' já existe nesta empresa");
                }
            }
            customer.setWhatsappId(updateDTO.getWhatsappId());
        }
        
        if (updateDTO.getProfileUrl() != null) {
            customer.setProfileUrl(updateDTO.getProfileUrl());
        }
        
        if (updateDTO.getIsBlocked() != null) {
            customer.setIsBlocked(updateDTO.getIsBlocked());
        }

        Optional.ofNullable(updateDTO.getSourceSystemName()).ifPresent(customer::setSourceSystemName);
        Optional.ofNullable(updateDTO.getSourceSystemId()).ifPresent(customer::setSourceSystemId);
        Optional.ofNullable(updateDTO.getImportedAt()).ifPresent(customer::setImportedAt);
        Optional.ofNullable(updateDTO.getBirthDate()).ifPresent(customer::setBirthDate);
        Optional.ofNullable(updateDTO.getLastDonationDate()).ifPresent(customer::setLastDonationDate);
        Optional.ofNullable(updateDTO.getNextEligibleDonationDate()).ifPresent(customer::setNextEligibleDonationDate);
        Optional.ofNullable(updateDTO.getBloodType()).ifPresent(customer::setBloodType);
        Optional.ofNullable(updateDTO.getHeight()).ifPresent(customer::setHeight);
        Optional.ofNullable(updateDTO.getWeight()).ifPresent(customer::setWeight);
        Optional.ofNullable(updateDTO.getAddressStreet()).ifPresent(customer::setAddressStreet);
        Optional.ofNullable(updateDTO.getAddressNumber()).ifPresent(customer::setAddressNumber);
        Optional.ofNullable(updateDTO.getAddressComplement()).ifPresent(customer::setAddressComplement);
        Optional.ofNullable(updateDTO.getAddressPostalCode()).ifPresent(customer::setAddressPostalCode);
        Optional.ofNullable(updateDTO.getAddressCity()).ifPresent(customer::setAddressCity);
        Optional.ofNullable(updateDTO.getAddressState()).ifPresent(customer::setAddressState);
        
        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully");
        
        return toDTO(updated);
    }
    
    public CustomerDTO blockCustomer(UUID id, UUID companyId) {
        log.info("Blocking customer with id: {} for company: {}", id, companyId);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
        customer.setIsBlocked(true);
        Customer updated = customerRepository.save(customer);
        
        log.info("Customer blocked successfully");
        return toDTO(updated);
    }
    
    public CustomerDTO unblockCustomer(UUID id, UUID companyId) {
        log.info("Unblocking customer with id: {} for company: {}", id, companyId);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
        customer.setIsBlocked(false);
        Customer updated = customerRepository.save(customer);
        
        log.info("Customer unblocked successfully");
        return toDTO(updated);
    }
    
    public void delete(UUID id, UUID companyId) {
        log.info("Deleting customer with id: {} for company: {}", id, companyId);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        // Validate customer belongs to company
        if (!customer.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("Cliente não pertence a esta empresa");
        }
        
        customerRepository.deleteById(id);
        log.info("Customer deleted successfully");
    }
    
    @Transactional(readOnly = true)
    public String normalizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }
        
        // Remove all non-digit characters
        String digitsOnly = phone.replaceAll("\\D", "");
        
        // Add +55 prefix if not present
        if (digitsOnly.length() == 10 || digitsOnly.length() == 11) {
            // Brazilian phone without country code
            return "+55" + digitsOnly;
        } else if (digitsOnly.length() == 12 && digitsOnly.startsWith("55")) {
            // Brazilian phone with country code but without +
            return "+" + digitsOnly;
        } else if (digitsOnly.length() == 13 && digitsOnly.startsWith("55")) {
            // Brazilian phone with country code and +
            return "+" + digitsOnly;
        }
        
        return phone; // Return as is if format is unexpected
    }
    
    // Company-scoped methods
    @Transactional(readOnly = true)
    public List<CustomerDTO> findAllByCompany(UUID companyId) {
        log.debug("Finding all customers for company: {}", companyId);
        
        return customerRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO findByPhoneAndCompany(String phone, UUID companyId) {
        log.debug("Finding customer by phone: {} for company: {}", phone, companyId);
        
        Optional<Customer> customer = customerRepository.findByPhoneAndCompanyId(phone, companyId);
        
        return customer.map(this::toDTO).orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<CustomerDTO> findActiveByCompany(UUID companyId) {
        log.debug("Finding active customers for company: {}", companyId);
        
        return customerRepository.findByIsBlockedFalseAndCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<CustomerDTO> searchByNameOrPhoneAndCompany(String searchTerm, UUID companyId) {
        log.debug("Searching customers by term: {} for company: {}", searchTerm, companyId);
        
        return customerRepository.searchByNameOrPhoneAndCompany(searchTerm, companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public long countActiveByCompany(UUID companyId) {
        return customerRepository.countActiveCustomersByCompany(companyId);
    }
    
    public void deleteAllByCompany(UUID companyId) {
        log.info("Deleting all customers for company: {}", companyId);
        
        List<Customer> customers = customerRepository.findByCompanyId(companyId);
        customerRepository.deleteAll(customers);
        
        log.info("Deleted {} customers for company: {}", customers.size(), companyId);
    }

    private CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .companyId(customer.getCompany() != null ? customer.getCompany().getId() : null)
                .phone(customer.getPhone())
                .name(customer.getName())
                .whatsappId(customer.getWhatsappId())
                .profileUrl(customer.getProfileUrl())
                .isBlocked(customer.getIsBlocked())
                .sourceSystemName(customer.getSourceSystemName())
                .sourceSystemId(customer.getSourceSystemId())
                .importedAt(customer.getImportedAt())
                .birthDate(customer.getBirthDate())
                .lastDonationDate(customer.getLastDonationDate())
                .nextEligibleDonationDate(customer.getNextEligibleDonationDate())
                .bloodType(customer.getBloodType())
                .height(customer.getHeight())
                .weight(customer.getWeight())
                .addressStreet(customer.getAddressStreet())
                .addressNumber(customer.getAddressNumber())
                .addressComplement(customer.getAddressComplement())
                .addressPostalCode(customer.getAddressPostalCode())
                .addressCity(customer.getAddressCity())
                .addressState(customer.getAddressState())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}