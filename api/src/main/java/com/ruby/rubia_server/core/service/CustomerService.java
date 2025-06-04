package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {
    
    private final CustomerRepository customerRepository;
    
    public CustomerDTO create(CreateCustomerDTO createDTO) {
        log.info("Creating customer with phone: {}", createDTO.getPhone());
        
        if (customerRepository.existsByPhone(createDTO.getPhone())) {
            throw new IllegalArgumentException("Cliente com telefone '" + createDTO.getPhone() + "' já existe");
        }
        
        if (createDTO.getWhatsappId() != null && customerRepository.existsByWhatsappId(createDTO.getWhatsappId())) {
            throw new IllegalArgumentException("Cliente com WhatsApp ID '" + createDTO.getWhatsappId() + "' já existe");
        }
        
        Customer customer = Customer.builder()
                .phone(createDTO.getPhone())
                .name(createDTO.getName())
                .whatsappId(createDTO.getWhatsappId())
                .profileUrl(createDTO.getProfileUrl())
                .isBlocked(createDTO.getIsBlocked())
                .build();
        
        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully with id: {}", saved.getId());
        
        return toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO findById(UUID id) {
        log.debug("Finding customer by id: {}", id);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        return toDTO(customer);
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO findByPhone(String phone) {
        log.debug("Finding customer by phone: {}", phone);
        
        Customer customer = customerRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        return toDTO(customer);
    }
    
    @Transactional(readOnly = true)
    public CustomerDTO findByWhatsappId(String whatsappId) {
        log.debug("Finding customer by WhatsApp ID: {}", whatsappId);
        
        Customer customer = customerRepository.findByWhatsappId(whatsappId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        return toDTO(customer);
    }
    
    @Transactional(readOnly = true)
    public List<CustomerDTO> findAll() {
        log.debug("Finding all customers");
        
        return customerRepository.findActiveCustomersOrderedByName()
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<CustomerDTO> findBlocked() {
        log.debug("Finding blocked customers");
        
        return customerRepository.findByIsBlockedTrue()
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<CustomerDTO> searchByNameOrPhone(String searchTerm) {
        log.debug("Searching customers by name or phone: {}", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll();
        }
        
        return customerRepository.searchByNameOrPhone(searchTerm.trim())
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    public CustomerDTO findOrCreateByPhone(String phone, String name) {
        log.info("Finding or creating customer by phone: {}", phone);
        
        try {
            return findByPhone(phone);
        } catch (IllegalArgumentException e) {
            log.info("Customer not found, creating new one");
            
            CreateCustomerDTO createDTO = CreateCustomerDTO.builder()
                    .phone(phone)
                    .name(name)
                    .build();
            
            return create(createDTO);
        }
    }
    
    public CustomerDTO update(UUID id, UpdateCustomerDTO updateDTO) {
        log.info("Updating customer with id: {}", id);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        if (updateDTO.getPhone() != null) {
            if (!updateDTO.getPhone().equals(customer.getPhone()) && 
                customerRepository.existsByPhone(updateDTO.getPhone())) {
                throw new IllegalArgumentException("Cliente com telefone '" + updateDTO.getPhone() + "' já existe");
            }
            customer.setPhone(updateDTO.getPhone());
        }
        
        if (updateDTO.getName() != null) {
            customer.setName(updateDTO.getName());
        }
        
        if (updateDTO.getWhatsappId() != null) {
            if (!updateDTO.getWhatsappId().equals(customer.getWhatsappId()) && 
                customerRepository.existsByWhatsappId(updateDTO.getWhatsappId())) {
                throw new IllegalArgumentException("Cliente com WhatsApp ID '" + updateDTO.getWhatsappId() + "' já existe");
            }
            customer.setWhatsappId(updateDTO.getWhatsappId());
        }
        
        if (updateDTO.getProfileUrl() != null) {
            customer.setProfileUrl(updateDTO.getProfileUrl());
        }
        
        if (updateDTO.getIsBlocked() != null) {
            customer.setIsBlocked(updateDTO.getIsBlocked());
        }
        
        Customer updated = customerRepository.save(customer);
        log.info("Customer updated successfully");
        
        return toDTO(updated);
    }
    
    public CustomerDTO blockCustomer(UUID id) {
        log.info("Blocking customer with id: {}", id);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        customer.setIsBlocked(true);
        Customer updated = customerRepository.save(customer);
        
        log.info("Customer blocked successfully");
        return toDTO(updated);
    }
    
    public CustomerDTO unblockCustomer(UUID id) {
        log.info("Unblocking customer with id: {}", id);
        
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));
        
        customer.setIsBlocked(false);
        Customer updated = customerRepository.save(customer);
        
        log.info("Customer unblocked successfully");
        return toDTO(updated);
    }
    
    public void delete(UUID id) {
        log.info("Deleting customer with id: {}", id);
        
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Cliente não encontrado");
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
    
    private CustomerDTO toDTO(Customer customer) {
        return CustomerDTO.builder()
                .id(customer.getId())
                .phone(customer.getPhone())
                .name(customer.getName())
                .whatsappId(customer.getWhatsappId())
                .profileUrl(customer.getProfileUrl())
                .isBlocked(customer.getIsBlocked())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}