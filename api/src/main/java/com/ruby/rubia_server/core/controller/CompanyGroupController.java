package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CompanyGroupDTO;
import com.ruby.rubia_server.core.dto.CreateCompanyGroupDTO;
import com.ruby.rubia_server.core.dto.UpdateCompanyGroupDTO;
import com.ruby.rubia_server.core.service.CompanyGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/company-groups")
@RequiredArgsConstructor
@Slf4j
public class CompanyGroupController {

    private final CompanyGroupService companyGroupService;

    @PostMapping
    public ResponseEntity<CompanyGroupDTO> create(@Valid @RequestBody CreateCompanyGroupDTO createDTO) {
        log.info("Creating company group: {}", createDTO.getName());
        try {
            CompanyGroupDTO created = companyGroupService.create(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating company group: {}", e.getMessage());
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<CompanyGroupDTO>> findAll() {
        log.debug("Finding all company groups");
        List<CompanyGroupDTO> companyGroups = companyGroupService.findAll();
        return ResponseEntity.ok(companyGroups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyGroupDTO> findById(@PathVariable UUID id) {
        log.debug("Finding company group by id: {}", id);
        try {
            CompanyGroupDTO companyGroup = companyGroupService.findById(id);
            return ResponseEntity.ok(companyGroup);
        } catch (IllegalArgumentException e) {
            log.warn("Company group not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompanyGroupDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateCompanyGroupDTO updateDTO) {
        log.info("Updating company group: {}", id);
        try {
            CompanyGroupDTO updated = companyGroupService.update(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating company group: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting company group: {}", id);
        try {
            companyGroupService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting company group: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
