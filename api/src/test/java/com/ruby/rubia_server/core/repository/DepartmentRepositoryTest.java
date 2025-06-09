package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DepartmentRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    private Department department1;
    private Department department2;
    private Company company;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .slug("test-company")
                .isActive(true)
                .build();
        
        entityManager.persistAndFlush(company);
        
        department1 = Department.builder()
                .name("Comercial")
                .description("Departamento comercial")
                .company(company)
                .autoAssign(true)
                .build();
        
        department2 = Department.builder()
                .name("Suporte")
                .description("Departamento de suporte")
                .company(company)
                .autoAssign(false)
                .build();
        
        entityManager.persistAndFlush(department1);
        entityManager.persistAndFlush(department2);
    }
    
    @Test
    void findByName_ShouldReturnDepartment_WhenExists() {
        Optional<Department> result = departmentRepository.findByNameAndCompanyId("Comercial", companyId);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Comercial");
        assertThat(result.get().getDescription()).isEqualTo("Departamento comercial");
    }
    
    @Test
    void findByName_ShouldReturnEmpty_WhenNotExists() {
        Optional<Department> result = departmentRepository.findByNameAndCompanyId("Inexistente", companyId);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByAutoAssignTrue_ShouldReturnFilteredDepartments() {
        List<Department> result = departmentRepository.findByAutoAssignTrueAndCompanyId(companyId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Comercial");
        assertThat(result.get(0).getAutoAssign()).isTrue();
    }
    
    @Test
    void findAllOrderedByName_ShouldReturnOrderedList() {
        List<Department> result = departmentRepository.findAllByCompanyIdOrderedByName(companyId);
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Comercial");
        assertThat(result.get(1).getName()).isEqualTo("Suporte");
    }
    
    @Test
    void existsByName_ShouldReturnTrue_WhenExists() {
        boolean result = departmentRepository.existsByNameAndCompanyId("Comercial", companyId);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByName_ShouldReturnFalse_WhenNotExists() {
        boolean result = departmentRepository.existsByNameAndCompanyId("Inexistente", companyId);
        
        assertThat(result).isFalse();
    }
}