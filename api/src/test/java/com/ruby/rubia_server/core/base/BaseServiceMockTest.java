package com.ruby.rubia_server.core.base;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public abstract class BaseServiceMockTest<T extends BaseEntity, CreateDTO, UpdateDTO> {
    
    protected BaseCompanyEntityRepository<T> repository;
    protected CompanyRepository companyRepository;
    protected EntityRelationshipValidator relationshipValidator;
    
    protected Company company;
    protected T entity;
    protected CreateDTO createDTO;
    protected UpdateDTO updateDTO;
    protected UUID companyId;
    protected UUID entityId;
    
    @BeforeEach
    void setUp() {
        // Criar mocks manualmente para evitar problemas de injeção genérica
        repository = mock(BaseCompanyEntityRepository.class);
        companyRepository = mock(CompanyRepository.class);
        relationshipValidator = mock(EntityRelationshipValidator.class);
        
        companyId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .build();
        
        entity = createEntity();
        createDTO = createValidDTO();
        updateDTO = createUpdateDTO();
        
        // Configurar comportamento padrão dos mocks
        configureMocks();
    }
    
    protected void configureMocks() {
        // Configuração padrão que pode ser sobrescrita
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenReturn(entity);
        when(repository.findById(entityId)).thenReturn(Optional.of(entity));
        when(repository.existsById(entityId)).thenReturn(true);
        when(repository.findByCompanyId(companyId)).thenReturn(List.of(entity));
        when(repository.countByCompanyId(companyId)).thenReturn(1L);
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(entity)));
    }
    
    // Template tests (15+ testes padronizados)
    @Test
    void create_ShouldCreateAndReturn_WhenValidData() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenReturn(entity);
        
        // When
        T result = getService().create(createDTO);
        
        // Then
        assertNotNull(result);
        assertEquals(entity.getId(), result.getId());
        verify(companyRepository).findById(companyId);
        verify(repository).save(any());
    }
    
    @Test
    void create_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> getService().create(createDTO));
        
        assertEquals("Company not found with ID: " + companyId, exception.getMessage());
        verify(repository, never()).save(any());
    }
    
    @Test
    void findById_ShouldReturn_WhenExists() {
        // Given
        when(repository.findById(entityId)).thenReturn(Optional.of(entity));
        
        // When
        Optional<T> result = getService().findById(entityId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        verify(repository).findById(entityId);
    }
    
    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(repository.findById(entityId)).thenReturn(Optional.empty());
        
        // When
        Optional<T> result = getService().findById(entityId);
        
        // Then
        assertTrue(result.isEmpty());
        verify(repository).findById(entityId);
    }
    
    @Test
    void findAll_ShouldReturnPagedResults() {
        // Given
        List<T> entities = List.of(entity);
        Page<T> page = new PageImpl<>(entities);
        Pageable pageable = PageRequest.of(0, 10);
        
        when(repository.findAll(pageable)).thenReturn(page);
        
        // When
        Page<T> result = getService().findAll(pageable);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals(entity.getId(), result.getContent().get(0).getId());
        verify(repository).findAll(pageable);
    }
    
    @Test
    void findByCompanyId_ShouldReturnEntitiesForCompany() {
        // Given
        List<T> entities = List.of(entity);
        when(repository.findByCompanyId(companyId)).thenReturn(entities);
        
        // When
        List<T> result = getService().findByCompanyId(companyId);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(entity.getId(), result.get(0).getId());
        verify(repository).findByCompanyId(companyId);
    }
    
    @Test
    void update_ShouldUpdateAndReturn_WhenExists() {
        // Given
        when(repository.findById(entityId)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);
        
        // When
        Optional<T> result = getService().update(entityId, updateDTO);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().getId());
        verify(repository).findById(entityId);
        verify(repository).save(any());
    }
    
    @Test
    void update_ShouldReturnEmpty_WhenNotExists() {
        // Given
        when(repository.findById(entityId)).thenReturn(Optional.empty());
        
        // When
        Optional<T> result = getService().update(entityId, updateDTO);
        
        // Then
        assertTrue(result.isEmpty());
        verify(repository).findById(entityId);
        verify(repository, never()).save(any());
    }
    
    @Test
    void deleteById_ShouldReturnTrue_WhenExists() {
        // Given
        when(repository.existsById(entityId)).thenReturn(true);
        
        // When
        boolean result = getService().deleteById(entityId);
        
        // Then
        assertTrue(result);
        verify(repository).existsById(entityId);
        verify(repository).deleteById(entityId);
    }
    
    @Test
    void deleteById_ShouldReturnFalse_WhenNotExists() {
        // Given
        when(repository.existsById(entityId)).thenReturn(false);
        
        // When
        boolean result = getService().deleteById(entityId);
        
        // Then
        assertFalse(result);
        verify(repository).existsById(entityId);
        verify(repository, never()).deleteById(entityId);
    }
    
    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        // Given
        when(repository.countByCompanyId(companyId)).thenReturn(5L);
        
        // When
        long count = getService().countByCompanyId(companyId);
        
        // Then
        assertEquals(5L, count);
        verify(repository).countByCompanyId(companyId);
    }
    
    // Abstract methods para customização
    protected abstract T createEntity();
    protected abstract CreateDTO createValidDTO();
    protected abstract UpdateDTO createUpdateDTO();
    protected abstract BaseCompanyEntityService<T, CreateDTO, UpdateDTO> getService();
}