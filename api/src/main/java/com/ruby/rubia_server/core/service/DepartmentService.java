package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateDepartmentDTO;
import com.ruby.rubia_server.core.dto.DepartmentDTO;
import com.ruby.rubia_server.core.dto.UpdateDepartmentDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepartmentService {
    
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    
    public DepartmentDTO create(CreateDepartmentDTO createDTO, UUID companyId) {
        log.info("Creating department with name: {} for company: {}", createDTO.getName(), companyId);
        
        if (departmentRepository.existsByNameAndCompanyId(createDTO.getName(), companyId)) {
            throw new IllegalArgumentException("Departamento com nome '" + createDTO.getName() + "' já existe");
        }
        
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        
        Department department = Department.builder()
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .company(company)
                .autoAssign(createDTO.getAutoAssign())
                .build();
        
        Department saved = departmentRepository.save(department);
        log.info("Department created successfully with id: {}", saved.getId());
        
        return toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public DepartmentDTO findById(UUID id, UUID companyId) {
        log.debug("Finding department by id: {} for company: {}", id, companyId);
        
        Department department = departmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
        
        return toDTO(department);
    }
    
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findAll(UUID companyId) {
        log.debug("Finding all departments for company: {}", companyId);
        
        return departmentRepository.findAllByCompanyIdOrderedByName(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<DepartmentDTO> findByAutoAssign(UUID companyId) {
        log.debug("Finding departments with auto assign enabled for company: {}", companyId);
        
        return departmentRepository.findByAutoAssignTrueAndCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    public DepartmentDTO update(UUID id, UpdateDepartmentDTO updateDTO, UUID companyId) {
        log.info("Updating department with id: {} for company: {}", id, companyId);
        
        Department department = departmentRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
        
        if (updateDTO.getName() != null) {
            if (!updateDTO.getName().equals(department.getName()) && 
                departmentRepository.existsByNameAndCompanyId(updateDTO.getName(), companyId)) {
                throw new IllegalArgumentException("Departamento com nome '" + updateDTO.getName() + "' já existe");
            }
            department.setName(updateDTO.getName());
        }
        
        if (updateDTO.getDescription() != null) {
            department.setDescription(updateDTO.getDescription());
        }
        
        if (updateDTO.getAutoAssign() != null) {
            department.setAutoAssign(updateDTO.getAutoAssign());
        }
        
        Department updated = departmentRepository.save(department);
        log.info("Department updated successfully");
        
        return toDTO(updated);
    }
    
    public void delete(UUID id, UUID companyId) {
        log.info("Deleting department with id: {} for company: {}", id, companyId);
        
        if (!departmentRepository.existsByIdAndCompanyId(id, companyId)) {
            throw new IllegalArgumentException("Departamento não encontrado");
        }
        
        departmentRepository.deleteByIdAndCompanyId(id, companyId);
        log.info("Department deleted successfully");
    }
    
    private DepartmentDTO toDTO(Department department) {
        return DepartmentDTO.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .autoAssign(department.getAutoAssign())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }
}