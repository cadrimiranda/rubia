package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CompanyRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    private Company company1;
    private Company company2;
    private Company company3;
    private UUID company1Id;
    private UUID company2Id;
    private UUID company3Id;
    
    @BeforeEach
    void setUp() {
        company1Id = UUID.randomUUID();
        company2Id = UUID.randomUUID();
        company3Id = UUID.randomUUID();
        
        company1 = Company.builder()
                .id(company1Id)
                .name("Tech Solutions Inc")
                .slug("tech-solutions")
                .isActive(true)
                .planType("PREMIUM")
                .maxUsers(50)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();
        
        company2 = Company.builder()
                .id(company2Id)
                .name("Digital Marketing Corp")
                .slug("digital-marketing")
                .isActive(true)
                .planType("BASIC")
                .maxUsers(10)
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();
        
        company3 = Company.builder()
                .id(company3Id)
                .name("Inactive Company")
                .slug("inactive-company")
                .isActive(false)
                .planType("BASIC")
                .maxUsers(5)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();
        
        entityManager.persistAndFlush(company1);
        entityManager.persistAndFlush(company2);
        entityManager.persistAndFlush(company3);
    }
    
    @Test
    void findBySlug_ShouldReturnCompany_WhenExists() {
        Optional<Company> result = companyRepository.findBySlug("tech-solutions");
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tech Solutions Inc");
        assertThat(result.get().getSlug()).isEqualTo("tech-solutions");
    }
    
    @Test
    void findBySlug_ShouldReturnEmpty_WhenNotExists() {
        Optional<Company> result = companyRepository.findBySlug("non-existent-slug");
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findBySlug_ShouldReturnInactiveCompany() {
        Optional<Company> result = companyRepository.findBySlug("inactive-company");
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Inactive Company");
        assertThat(result.get().getIsActive()).isFalse();
    }
    
    @Test
    void findByIsActiveTrue_ShouldReturnOnlyActiveCompanies() {
        List<Company> result = companyRepository.findByIsActiveTrue();
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Company::getName)
                .containsExactlyInAnyOrder("Tech Solutions Inc", "Digital Marketing Corp");
        assertThat(result).allMatch(Company::getIsActive);
    }
    
    @Test
    void findByIsActiveTrue_ShouldNotReturnInactiveCompanies() {
        List<Company> result = companyRepository.findByIsActiveTrue();
        
        assertThat(result).extracting(Company::getName)
                .doesNotContain("Inactive Company");
    }
    
    @Test
    void findByPlanType_ShouldReturnCompaniesWithSpecificPlan() {
        List<Company> result = companyRepository.findByPlanType("BASIC");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Digital Marketing Corp");
        assertThat(result.get(0).getPlanType()).isEqualTo("BASIC");
        assertThat(result.get(0).getIsActive()).isTrue();
    }
    
    @Test
    void findByPlanType_ShouldNotReturnInactiveCompanies() {
        List<Company> result = companyRepository.findByPlanType("BASIC");
        
        // Should only return active companies, even if inactive companies have the same plan
        assertThat(result).hasSize(1);
        assertThat(result).extracting(Company::getName)
                .doesNotContain("Inactive Company");
    }
    
    @Test
    void findByPlanType_ShouldReturnPremiumCompanies() {
        List<Company> result = companyRepository.findByPlanType("PREMIUM");
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Tech Solutions Inc");
        assertThat(result.get(0).getPlanType()).isEqualTo("PREMIUM");
    }
    
    @Test
    void findByPlanType_ShouldReturnEmpty_WhenPlanNotExists() {
        List<Company> result = companyRepository.findByPlanType("ENTERPRISE");
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void existsBySlug_ShouldReturnTrue_WhenSlugExists() {
        boolean result = companyRepository.existsBySlug("tech-solutions");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsBySlug_ShouldReturnFalse_WhenSlugNotExists() {
        boolean result = companyRepository.existsBySlug("non-existent-slug");
        
        assertThat(result).isFalse();
    }
    
    @Test
    void existsBySlug_ShouldReturnTrue_ForInactiveCompany() {
        boolean result = companyRepository.existsBySlug("inactive-company");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void findAll_ShouldReturnAllCompanies() {
        List<Company> result = companyRepository.findAll();
        
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Company::getName)
                .containsExactlyInAnyOrder(
                        "Tech Solutions Inc", 
                        "Digital Marketing Corp", 
                        "Inactive Company"
                );
    }
    
    @Test
    void findById_ShouldReturnCompany_WhenExists() {
        Optional<Company> result = companyRepository.findById(company1Id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tech Solutions Inc");
        assertThat(result.get().getId()).isEqualTo(company1Id);
    }
    
    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        UUID nonExistentId = UUID.randomUUID();
        Optional<Company> result = companyRepository.findById(nonExistentId);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void save_ShouldPersistNewCompany() {
        Company newCompany = Company.builder()
                .name("New Test Company")
                .slug("new-test-company")
                .isActive(true)
                .planType("BASIC")
                .maxUsers(20)
                .build();
        
        Company saved = companyRepository.save(newCompany);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Test Company");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
    
    @Test
    void delete_ShouldRemoveCompany() {
        companyRepository.deleteById(company3Id);
        
        Optional<Company> result = companyRepository.findById(company3Id);
        assertThat(result).isEmpty();
        
        List<Company> allCompanies = companyRepository.findAll();
        assertThat(allCompanies).hasSize(2);
    }
}