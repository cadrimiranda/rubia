package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.service.CustomerService;
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
    
    @PostMapping
    public ResponseEntity<CustomerDTO> create(@Valid @RequestBody CreateCustomerDTO createDTO) {
        log.info("Creating customer: {}", createDTO.getPhone());
        
        try {
            CustomerDTO created = customerService.create(createDTO);
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
            CustomerDTO customer = customerService.findById(id);
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
            CustomerDTO customer = customerService.findByPhone(phone);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            log.warn("Customer not found: {}", phone);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/whatsapp/{whatsappId}")
    public ResponseEntity<CustomerDTO> findByWhatsappId(@PathVariable String whatsappId) {
        log.debug("Finding customer by WhatsApp ID: {}", whatsappId);
        
        try {
            CustomerDTO customer = customerService.findByWhatsappId(whatsappId);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            log.warn("Customer not found: {}", whatsappId);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<CustomerDTO>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "false") boolean includeBlocked) {
        log.debug("Finding customers, search: {}, includeBlocked: {}", search, includeBlocked);
        
        List<CustomerDTO> customers;
        
        if (search != null && !search.trim().isEmpty()) {
            customers = customerService.searchByNameOrPhone(search);
        } else if (includeBlocked) {
            customers = customerService.findBlocked();
        } else {
            customers = customerService.findAll();
        }
        
        return ResponseEntity.ok(customers);
    }
    
    @GetMapping("/blocked")
    public ResponseEntity<List<CustomerDTO>> findBlocked() {
        log.debug("Finding blocked customers");
        
        List<CustomerDTO> customers = customerService.findBlocked();
        return ResponseEntity.ok(customers);
    }
    
    @PostMapping("/find-or-create")
    public ResponseEntity<CustomerDTO> findOrCreateByPhone(@RequestParam String phone,
                                                          @RequestParam(required = false) String name) {
        log.info("Finding or creating customer by phone: {}", phone);
        
        try {
            CustomerDTO customer = customerService.findOrCreateByPhone(phone, name);
            return ResponseEntity.ok(customer);
        } catch (IllegalArgumentException e) {
            log.warn("Error finding or creating customer: {}", e.getMessage());
            throw e;
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CustomerDTO> update(@PathVariable UUID id, 
                                             @Valid @RequestBody UpdateCustomerDTO updateDTO) {
        log.info("Updating customer: {}", id);
        
        try {
            CustomerDTO updated = customerService.update(id, updateDTO);
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
            CustomerDTO blocked = customerService.blockCustomer(id);
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
            CustomerDTO unblocked = customerService.unblockCustomer(id);
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
            customerService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting customer: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}