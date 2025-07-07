package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateDepartmentDTO;
import com.ruby.rubia_server.core.dto.DepartmentDTO;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
    private CreateDepartmentDTO createDepartmentDTO;
    private UUID companyId;
    private UUID departmentId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        departmentId = UUID.randomUUID();

        company = Company.builder().id(companyId).name("Test Company").build();
        
        createDepartmentDTO = CreateDepartmentDTO.builder()
                .name("Test Department")
                .description("A test department")
                .companyId(companyId)
                .autoAssign(true)
                .build();

        department = Department.builder()
                .id(departmentId)
                .name(createDepartmentDTO.getName())
                .description(createDepartmentDTO.getDescription())
                .company(company)
                .autoAssign(createDepartmentDTO.getAutoAssign())
                .build();
    }

    @Test
    void createDepartment_Success() {
        // Arrange
        when(departmentRepository.existsByNameAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(departmentRepository.save(any(Department.class))).thenReturn(department);

        // Act
        DepartmentDTO result = departmentService.create(createDepartmentDTO, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(department.getId(), result.getId());
        assertEquals(createDepartmentDTO.getName(), result.getName());
        verify(departmentRepository, times(1)).save(any(Department.class));
    }

    @Test
    void createDepartment_FailsWhenNameExists() {
        // Arrange
        when(departmentRepository.existsByNameAndCompanyId(createDepartmentDTO.getName(), companyId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            departmentService.create(createDepartmentDTO, companyId);
        });
        assertEquals("Departamento com nome '" + createDepartmentDTO.getName() + "' já existe", exception.getMessage());
        verify(departmentRepository, never()).save(any(Department.class));
    }
}
