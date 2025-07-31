package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.service.CustomerService;
import com.ruby.rubia_server.core.service.PhoneService;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {
    
    private final CustomerService customerService;
    private final PhoneService phoneService;
    private final CompanyContextUtil companyContextUtil;
    
    @PostMapping
    public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CreateCustomerDTO createDTO) {
        log.info("Creating customer: {}", createDTO.getPhone());
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            CustomerDTO created = customerService.create(createDTO, currentCompanyId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating customer: {}", e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<CustomerDTO> findById(@PathVariable UUID id) {
        log.debug("Finding customer by id: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            CustomerDTO customer = customerService.findById(id, currentCompanyId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            log.warn("Customer not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/phone/{phone}")
    public ResponseEntity<CustomerDTO> findByPhone(@PathVariable String phone) {
        log.debug("Finding customer by phone: {}", phone);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            String normalizedPhone = phoneService.normalize(phone);
            CustomerDTO customer = customerService.findByPhoneAndCompany(normalizedPhone, currentCompanyId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            log.warn("Customer not found: {}", phone);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/whatsapp/{whatsappId}")
    public ResponseEntity<CustomerDTO> findByWhatsappId(@PathVariable String whatsappId) {
        log.debug("Finding customer by WhatsApp ID: {}", whatsappId);
        
        // This endpoint is not implemented for multi-tenant
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean includeBlocked) {
        log.debug("Finding customers, search: {}, includeBlocked: {}", search, includeBlocked);
        
        UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
        List<CustomerDTO> customers;
        
        if (search != null && !search.trim().isEmpty()) {
            customers = customerService.searchByNameOrPhoneAndCompany(search, currentCompanyId);
        } else if (includeBlocked) {
            // Return empty list for blocked customers as this method doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } else {
            customers = customerService.findAllByCompany(currentCompanyId);
        }
        
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/blocked")
    public ResponseEntity<List<CustomerDTO>> findBlocked() {
        log.debug("Finding blocked customers");
        
        // This endpoint is not implemented for multi-tenant
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @PostMapping("/find-or-create")
    public ResponseEntity<CustomerDTO> findOrCreateByPhone(@RequestParam String phone,
                                                          @RequestParam(required = false) String name) {
        log.info("Finding or creating customer by phone: {}", phone);
        
        // This endpoint is not implemented for multi-tenant
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(@PathVariable UUID id, 
                                             @Valid @RequestBody UpdateCustomerDTO updateDTO) {
        log.info("Updating customer: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            CustomerDTO updated = customerService.update(id, updateDTO, currentCompanyId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating customer: {}", e.getMessage());
            if (e.getMessage().contains("não encontrado")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
    
    @PutMapping("/{id}/block")
    public ResponseEntity<CustomerDTO> blockCustomer(@PathVariable UUID id) {
        log.info("Blocking customer: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            CustomerDTO blocked = customerService.blockCustomer(id, currentCompanyId);
            return ResponseEntity.ok(blocked);
        } catch (IllegalArgumentException e) {
            log.warn("Error blocking customer: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{id}/unblock")
    public ResponseEntity<CustomerDTO> unblockCustomer(@PathVariable UUID id) {
        log.info("Unblocking customer: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            CustomerDTO unblocked = customerService.unblockCustomer(id, currentCompanyId);
            return ResponseEntity.ok(unblocked);
        } catch (IllegalArgumentException e) {
            log.warn("Error unblocking customer: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting customer: {}", id);
        
        try {
            UUID currentCompanyId = companyContextUtil.getCurrentCompanyId();
            customerService.delete(id, currentCompanyId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting customer: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}