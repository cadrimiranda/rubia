package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateDepartmentDTO;
import com.ruby.rubia_server.core.dto.DepartmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDepartmentDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {
    
    @Mock
    private DepartmentRepository departmentRepository;
    
    @Mock
    private CompanyRepository companyRepository;
    
    @InjectMocks
    private DepartmentService departmentService;
    
    private Company company;
    private Department department;
    private CreateDepartmentDTO createDTO;
    private UpdateDepartmentDTO updateDTO;
    private UUID companyId;
    private UUID departmentId;
    
    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        departmentId = UUID.randomUUID();
        
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .slug("test-company")
                .isActive(true)
                .build();
        
        department = Department.builder()
                .id(departmentId)
                .name("Comercial")
                .description("Departamento comercial")
                .company(company)
                .autoAssign(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateDepartmentDTO.builder()
                .name("Comercial")
                .description("Departamento comercial")
                .companyId(companyId)
                .autoAssign(true)
                .build();
        
        updateDTO = UpdateDepartmentDTO.builder()
                .name("Comercial Atualizado")
                .description("Descrição atualizada")
                .autoAssign(false)
                .build();
    }
    
    @Test
    void create_ShouldCreateDepartment_WhenValidData() {
        when(departmentRepository.existsByNameAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        
        DepartmentDTO result = departmentService.create(createDTO, companyId);
        
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Comercial");
        assertThat(result.getDescription()).isEqualTo("Departamento comercial");
        assertThat(result.getAutoAssign()).isTrue();
        
        verify(departmentRepository).existsByNameAndCompanyId("Comercial", companyId);
        verify(companyRepository).findById(companyId);
        verify(departmentRepository).save(any(Department.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenNameAlreadyExists() {
        when(departmentRepository.existsByNameAndCompanyId(anyString(), any(UUID.class))).thenReturn(true);
        
        assertThatThrownBy(() -> departmentService.create(createDTO, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
        
        verify(departmentRepository).existsByNameAndCompanyId("Comercial", companyId);
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    void findById_ShouldReturnDepartment_WhenExists() {
        when(departmentRepository.findByIdAndCompanyId(departmentId, companyId)).thenReturn(Optional.of(department));
        
        DepartmentDTO result = departmentService.findById(departmentId, companyId);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(departmentId);
        assertThat(result.getName()).isEqualTo("Comercial");
        
        verify(departmentRepository).findByIdAndCompanyId(departmentId, companyId);
    }
    
    @Test
    void findById_ShouldThrowException_WhenNotExists() {
        when(departmentRepository.findByIdAndCompanyId(departmentId, companyId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> departmentService.findById(departmentId, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(departmentRepository).findByIdAndCompanyId(departmentId, companyId);
    }
    
    @Test
    void findAll_ShouldReturnAllDepartments() {
        when(departmentRepository.findAllByCompanyIdOrderedByName(companyId)).thenReturn(List.of(department));
        
        List<DepartmentDTO> result = departmentService.findAll(companyId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Comercial");
        
        verify(departmentRepository).findAllByCompanyIdOrderedByName(companyId);
    }
    
    @Test
    void findByAutoAssign_ShouldReturnFilteredDepartments() {
        when(departmentRepository.findByAutoAssignTrueAndCompanyId(companyId)).thenReturn(List.of(department));
        
        List<DepartmentDTO> result = departmentService.findByAutoAssign(companyId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAutoAssign()).isTrue();
        
        verify(departmentRepository).findByAutoAssignTrueAndCompanyId(companyId);
    }
    
    @Test
    void update_ShouldUpdateDepartment_WhenValidData() {
        when(departmentRepository.findByIdAndCompanyId(departmentId, companyId)).thenReturn(Optional.of(department));
        when(departmentRepository.existsByNameAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenReturn(department);
        
        DepartmentDTO result = departmentService.update(departmentId, updateDTO, companyId);
        
        assertThat(result).isNotNull();
        
        verify(departmentRepository).findByIdAndCompanyId(departmentId, companyId);
        verify(departmentRepository).save(any(Department.class));
    }
    
    @Test
    void update_ShouldThrowException_WhenNotExists() {
        when(departmentRepository.findByIdAndCompanyId(departmentId, companyId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> departmentService.update(departmentId, updateDTO, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(departmentRepository).findByIdAndCompanyId(departmentId, companyId);
        verify(departmentRepository, never()).save(any(Department.class));
    }
    
    @Test
    void delete_ShouldDeleteDepartment_WhenExists() {
        when(departmentRepository.existsByIdAndCompanyId(departmentId, companyId)).thenReturn(true);
        
        departmentService.delete(departmentId, companyId);
        
        verify(departmentRepository).existsByIdAndCompanyId(departmentId, companyId);
        verify(departmentRepository).deleteByIdAndCompanyId(departmentId, companyId);
    }
    
    @Test
    void delete_ShouldThrowException_WhenNotExists() {
        when(departmentRepository.existsByIdAndCompanyId(departmentId, companyId)).thenReturn(false);
        
        assertThatThrownBy(() -> departmentService.delete(departmentId, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(departmentRepository).existsByIdAndCompanyId(departmentId, companyId);
        verify(departmentRepository, never()).deleteByIdAndCompanyId(any(), any());
    }
}