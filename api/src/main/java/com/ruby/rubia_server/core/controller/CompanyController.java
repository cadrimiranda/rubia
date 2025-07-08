package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CompanyDTO;
import com.ruby.rubia_server.core.dto.CreateCompanyDTO;
import com.ruby.rubia_server.core.dto.UpdateCompanyDTO;
import com.ruby.rubia_server.core.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    public ResponseEntity<CompanyDTO> create(@Valid @RequestBody CreateCompanyDTO createDTO) {
        log.info("Creating company: {}", createDTO.getName());
        try {
            CompanyDTO created = companyService.create(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating company: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<CompanyDTO>> findAll() {
        log.debug("Finding all companies");
        List<CompanyDTO> companies = companyService.findAll();
        return ResponseEntity.ok(companies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyDTO> findById(@PathVariable UUID id) {
        log.debug("Finding company by id: {}", id);
        try {
            CompanyDTO company = companyService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Company not found with id: " + id));
            return ResponseEntity.ok(company);
        } catch (IllegalArgumentException e) {
            log.warn("Company not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateCompanyDTO updateDTO) {
        log.info("Updating company: {}", id);
        try {
            CompanyDTO updated = companyService.update(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating company: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting company: {}", id);
        try {
            companyService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting company: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
