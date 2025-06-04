package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CreateDepartmentDTO;
import com.ruby.rubia_server.core.dto.DepartmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDepartmentDTO;
import com.ruby.rubia_server.core.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {
    
    private final DepartmentService departmentService;
    
    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody CreateDepartmentDTO createDTO) {
        log.info("Creating department: {}", createDTO.getName());
        
        try {
            DepartmentDTO created = departmentService.create(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating department: {}", e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> findById(@PathVariable UUID id) {
        log.debug("Finding department by id: {}", id);
        
        try {
            DepartmentDTO department = departmentService.findById(id);
            return ResponseEntity.ok(department);
        } catch (IllegalArgumentException e) {
            log.warn("Department not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> findAll(
            @RequestParam(required = false, defaultValue = "false") boolean autoAssignOnly) {
        log.debug("Finding all departments, autoAssignOnly: {}", autoAssignOnly);
        
        List<DepartmentDTO> departments = autoAssignOnly 
            ? departmentService.findByAutoAssign()
            : departmentService.findAll();
            
        return ResponseEntity.ok(departments);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDTO> update(@PathVariable UUID id, 
                                               @Valid @RequestBody UpdateDepartmentDTO updateDTO) {
        log.info("Updating department: {}", id);
        
        try {
            DepartmentDTO updated = departmentService.update(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating department: {}", e.getMessage());
            if (e.getMessage().contains("não encontrado")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting department: {}", id);
        
        try {
            departmentService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting department: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}