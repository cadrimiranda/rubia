package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Customer;
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
class CustomerRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    private Customer customer1;
    private Customer customer2;
    private Customer customer3;
    private Company company1;
    private Company company2;
    private UUID company1Id;
    private UUID company2Id;
    
    @BeforeEach
    void setUp() {
        company1Id = UUID.randomUUID();
        company2Id = UUID.randomUUID();
        
        company1 = Company.builder()
                .id(company1Id)
                .name("Company 1")
                .slug("company1")
                .isActive(true)
                .build();
        
        company2 = Company.builder()
                .id(company2Id)
                .name("Company 2")
                .slug("company2")
                .isActive(true)
                .build();
        
        entityManager.persistAndFlush(company1);
        entityManager.persistAndFlush(company2);
        
        customer1 = Customer.builder()
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .profileUrl("profile1.jpg")
                .isBlocked(false)
                .company(company1)
                .build();
        
        customer2 = Customer.builder()
                .phone("+5511999999002")
                .name("Maria Santos")
                .whatsappId("wa_002")
                .profileUrl("profile2.jpg")
                .isBlocked(true)
                .company(company1)
                .build();
        
        customer3 = Customer.builder()
                .phone("+5511999999003")
                .name("Carlos Lima")
                .whatsappId("wa_003")
                .profileUrl("profile3.jpg")
                .isBlocked(false)
                .company(company2)
                .build();
        
        entityManager.persistAndFlush(customer1);
        entityManager.persistAndFlush(customer2);
        entityManager.persistAndFlush(customer3);
    }
    
    @Test
    void findByPhoneAndCompanyId_ShouldReturnCustomer_WhenExists() {
        Optional<Customer> result = customerRepository.findByPhoneAndCompanyId("+5511999999001", company1Id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("João Silva");
        assertThat(result.get().getPhone()).isEqualTo("+5511999999001");
        assertThat(result.get().getCompany().getId()).isEqualTo(company1Id);
    }
    
    @Test
    void findByPhoneAndCompanyId_ShouldReturnEmpty_WhenNotExists() {
        Optional<Customer> result = customerRepository.findByPhoneAndCompanyId("+5511999999999", company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByPhoneAndCompanyId_ShouldReturnEmpty_WhenExistsInDifferentCompany() {
        Optional<Customer> result = customerRepository.findByPhoneAndCompanyId("+5511999999003", company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByWhatsappIdAndCompanyId_ShouldReturnCustomer_WhenExists() {
        Optional<Customer> result = customerRepository.findByWhatsappIdAndCompanyId("wa_001", company1Id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("João Silva");
        assertThat(result.get().getWhatsappId()).isEqualTo("wa_001");
    }
    
    @Test
    void findByWhatsappIdAndCompanyId_ShouldReturnEmpty_WhenNotExists() {
        Optional<Customer> result = customerRepository.findByWhatsappIdAndCompanyId("wa_999", company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByCompanyId_ShouldReturnAllCustomersFromCompany() {
        List<Customer> result = customerRepository.findByCompanyId(company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Customer::getName)
                .containsExactlyInAnyOrder("João Silva", "Maria Santos");
    }
    
    @Test
    void findByCompanyId_ShouldReturnEmptyList_WhenNoCustomers() {
        UUID emptyCompanyId = UUID.randomUUID();
        List<Customer> result = customerRepository.findByCompanyId(emptyCompanyId);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByIsBlockedFalseAndCompanyId_ShouldReturnOnlyActiveCustomers() {
        List<Customer> result = customerRepository.findByIsBlockedFalseAndCompanyId(company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("João Silva");
        assertThat(result.get(0).getIsBlocked()).isFalse();
    }
    
    @Test
    void findByIsBlockedTrueAndCompanyId_ShouldReturnOnlyBlockedCustomers() {
        List<Customer> result = customerRepository.findByIsBlockedTrueAndCompanyId(company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Maria Santos");
        assertThat(result.get(0).getIsBlocked()).isTrue();
    }
    
    @Test
    void findActiveCustomersByCompanyOrderedByName_ShouldReturnOrderedActiveCustomers() {
        List<Customer> result = customerRepository.findActiveCustomersByCompanyOrderedByName(company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("João Silva");
        assertThat(result.get(0).getIsBlocked()).isFalse();
    }
    
    @Test
    void searchByNameOrPhoneAndCompany_ShouldFindByName() {
        List<Customer> result = customerRepository.searchByNameOrPhoneAndCompany("joão", company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("João Silva");
    }
    
    @Test
    void searchByNameOrPhoneAndCompany_ShouldFindByPhone() {
        List<Customer> result = customerRepository.searchByNameOrPhoneAndCompany("999999001", company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhone()).isEqualTo("+5511999999001");
    }
    
    @Test
    void searchByNameOrPhoneAndCompany_ShouldReturnEmpty_WhenNotFound() {
        List<Customer> result = customerRepository.searchByNameOrPhoneAndCompany("inexistente", company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void existsByPhoneAndCompanyId_ShouldReturnTrue_WhenExists() {
        boolean result = customerRepository.existsByPhoneAndCompanyId("+5511999999001", company1Id);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByPhoneAndCompanyId_ShouldReturnFalse_WhenNotExists() {
        boolean result = customerRepository.existsByPhoneAndCompanyId("+5511999999999", company1Id);
        
        assertThat(result).isFalse();
    }
    
    @Test
    void existsByPhoneAndCompanyId_ShouldReturnFalse_WhenExistsInDifferentCompany() {
        boolean result = customerRepository.existsByPhoneAndCompanyId("+5511999999003", company1Id);
        
        assertThat(result).isFalse();
    }
    
    @Test
    void existsByWhatsappIdAndCompanyId_ShouldReturnTrue_WhenExists() {
        boolean result = customerRepository.existsByWhatsappIdAndCompanyId("wa_001", company1Id);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByWhatsappIdAndCompanyId_ShouldReturnFalse_WhenNotExists() {
        boolean result = customerRepository.existsByWhatsappIdAndCompanyId("wa_999", company1Id);
        
        assertThat(result).isFalse();
    }
    
    @Test
    void countActiveCustomersByCompany_ShouldReturnCorrectCount() {
        long result = customerRepository.countActiveCustomersByCompany(company1Id);
        
        assertThat(result).isEqualTo(1);
    }
    
    @Test
    void countActiveCustomersByCompany_ShouldReturnZero_WhenNoActiveCustomers() {
        long result = customerRepository.countActiveCustomersByCompany(company2Id);
        
        assertThat(result).isEqualTo(1); // customer3 is active in company2
    }
}