package com.ruby.rubia_server.core.base;

import com.ruby.rubia_server.core.util.CompanyContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public abstract class BaseCompanyEntityController<T extends BaseEntity, CreateDTO, UpdateDTO, ResponseDTO> {
    
    protected final BaseCompanyEntityService<T, CreateDTO, UpdateDTO> service;
    protected final CompanyContextUtil companyContextUtil;
    
    @PostMapping
    public ResponseEntity<ResponseDTO> create(@Valid @RequestBody CreateDTO createDTO) {
        log.info("Creating {} via API", getEntityName());
        
        // Validar company context
        UUID companyId = getCompanyIdFromDTO(createDTO);
        companyContextUtil.ensureCompanyAccess(companyId);
        
        T entity = service.create(createDTO);
        ResponseDTO responseDTO = convertToDTO(entity);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDTO> findById(@PathVariable UUID id) {
        log.debug("Finding {} by id via API: {}", getEntityName(), id);
        
        return service.findById(id)
                .map(entity -> {
                    companyContextUtil.ensureCompanyAccess(entity.getCompany().getId());
                    return ResponseEntity.ok(convertToDTO(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<Page<ResponseDTO>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.debug("Finding all {} via API - page: {}, size: {}", getEntityName(), page, size);
        
        Pageable pageable = createPageable(page, size, sortBy, sortDir);
        Page<T> entities = service.findAll(pageable);
        Page<ResponseDTO> responseDTOs = entities.map(this::convertToDTO);
        
        return ResponseEntity.ok(responseDTOs);
    }
    
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ResponseDTO>> findByCompany(@PathVariable UUID companyId) {
        log.debug("Finding {} by company via API: {}", getEntityName(), companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        List<T> entities = service.findByCompanyId(companyId);
        List<ResponseDTO> responseDTOs = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responseDTOs);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody UpdateDTO updateDTO) {
        log.info("Updating {} via API with id: {}", getEntityName(), id);
        
        return service.update(id, updateDTO)
                .map(entity -> {
                    companyContextUtil.ensureCompanyAccess(entity.getCompany().getId());
                    return ResponseEntity.ok(convertToDTO(entity));
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        log.info("Deleting {} via API with id: {}", getEntityName(), id);
        
        // Verificar se existe e tem acesso antes de deletar
        Optional<T> entity = service.findById(id);
        if (entity.isPresent()) {
            companyContextUtil.ensureCompanyAccess(entity.get().getCompany().getId());
            boolean deleted = service.deleteById(id);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/company/{companyId}/count")
    public ResponseEntity<Long> countByCompany(@PathVariable UUID companyId) {
        log.debug("Counting {} by company via API: {}", getEntityName(), companyId);
        
        companyContextUtil.ensureCompanyAccess(companyId);
        
        long count = service.countByCompanyId(companyId);
        return ResponseEntity.ok(count);
    }
    
    // Métodos utilitários
    protected Pageable createPageable(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
        return PageRequest.of(page, size, sort);
    }
    
    // Abstract methods
    protected abstract String getEntityName();
    protected abstract ResponseDTO convertToDTO(T entity);
    protected abstract UUID getCompanyIdFromDTO(CreateDTO createDTO);
}