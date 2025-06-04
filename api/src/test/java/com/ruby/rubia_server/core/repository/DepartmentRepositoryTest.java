package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DepartmentRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    private Department department1;
    private Department department2;
    
    @BeforeEach
    void setUp() {
        department1 = Department.builder()
                .name("Comercial")
                .description("Departamento comercial")
                .autoAssign(true)
                .build();
        
        department2 = Department.builder()
                .name("Suporte")
                .description("Departamento de suporte")
                .autoAssign(false)
                .build();
        
        entityManager.persistAndFlush(department1);
        entityManager.persistAndFlush(department2);
    }
    
    @Test
    void findByName_ShouldReturnDepartment_WhenExists() {
        Optional<Department> result = departmentRepository.findByName("Comercial");
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Comercial");
        assertThat(result.get().getDescription()).isEqualTo("Departamento comercial");
    }
    
    @Test
    void findByName_ShouldReturnEmpty_WhenNotExists() {
        Optional<Department> result = departmentRepository.findByName("Inexistente");
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByAutoAssignTrue_ShouldReturnFilteredDepartments() {
        List<Department> result = departmentRepository.findByAutoAssignTrue();
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Comercial");
        assertThat(result.get(0).getAutoAssign()).isTrue();
    }
    
    @Test
    void findAllOrderedByName_ShouldReturnOrderedList() {
        List<Department> result = departmentRepository.findAllOrderedByName();
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Comercial");
        assertThat(result.get(1).getName()).isEqualTo("Suporte");
    }
    
    @Test
    void existsByName_ShouldReturnTrue_WhenExists() {
        boolean result = departmentRepository.existsByName("Comercial");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByName_ShouldReturnFalse_WhenNotExists() {
        boolean result = departmentRepository.existsByName("Inexistente");
        
        assertThat(result).isFalse();
    }
}