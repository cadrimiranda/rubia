package com.ruby.rubia_server.core.base;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Transactional
@RequiredArgsConstructor
public abstract class BaseCompanyEntityService<T extends BaseEntity, CreateDTO, UpdateDTO> {
    
    protected final BaseCompanyEntityRepository<T> repository;
    protected final CompanyRepository companyRepository;
    protected final EntityRelationshipValidator relationshipValidator;
    
    // Template methods (implementação padrão)
    public T create(CreateDTO createDTO) {
        log.info("Creating {} with data: {}", getEntityName(), createDTO);
        
        // Validar relacionamentos
        relationshipValidator.validateRelationships(createDTO);
        
        // Buscar company obrigatória
        Company company = getCompanyFromDTO(createDTO);
        
        // Criar entidade
        T entity = buildEntityFromDTO(createDTO);
        entity.setCompany(company);
        
        // Salvar
        T saved = repository.save(entity);
        log.info("{} created successfully with id: {}", getEntityName(), saved.getId());
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public Optional<T> findById(UUID id) {
        log.debug("Finding {} by id: {}", getEntityName(), id);
        return repository.findById(id);
    }
    
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        log.debug("Finding all {} with pagination: {}", getEntityName(), pageable);
        return repository.findAll(pageable);
    }
    
    public Optional<T> update(UUID id, UpdateDTO updateDTO) {
        log.info("Updating {} with id: {}", getEntityName(), id);
        
        Optional<T> entityOpt = repository.findById(id);
        if (entityOpt.isEmpty()) {
            log.warn("{} not found with id: {}", getEntityName(), id);
            return Optional.empty();
        }
        
        T entity = entityOpt.get();
        updateEntityFromDTO(entity, updateDTO);
        
        T saved = repository.save(entity);
        log.info("{} updated successfully with id: {}", getEntityName(), saved.getId());
        
        return Optional.of(saved);
    }
    
    public boolean deleteById(UUID id) {
        log.info("Deleting {} with id: {}", getEntityName(), id);
        
        if (!repository.existsById(id)) {
            log.warn("{} not found with id: {}", getEntityName(), id);
            return false;
        }
        
        repository.deleteById(id);
        log.info("{} deleted successfully", getEntityName());
        return true;
    }
    
    // Métodos comuns implementados
    @Transactional(readOnly = true)
    public List<T> findByCompanyId(UUID companyId) {
        log.debug("Finding {} by company id: {}", getEntityName(), companyId);
        return repository.findByCompanyId(companyId);
    }
    
    @Transactional(readOnly = true)
    public long countByCompanyId(UUID companyId) {
        log.debug("Counting {} by company id: {}", getEntityName(), companyId);
        return repository.countByCompanyId(companyId);
    }
    
    // Método helper para validar e buscar company
    protected Company validateAndGetCompany(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + companyId));
    }
    
    // Abstract methods para implementação específica
    protected abstract String getEntityName();
    protected abstract T buildEntityFromDTO(CreateDTO createDTO);
    protected abstract void updateEntityFromDTO(T entity, UpdateDTO updateDTO);
    protected abstract Company getCompanyFromDTO(CreateDTO createDTO);
}