package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
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
class CustomerServiceTest {
    
    @Mock
    private CustomerRepository customerRepository;
    
    @Mock
    private CompanyRepository companyRepository;
    
    @InjectMocks
    private CustomerService customerService;
    
    private Customer customer;
    private Company company;
    private CreateCustomerDTO createDTO;
    private UpdateCustomerDTO updateDTO;
    private UUID customerId;
    private UUID companyId;
    
    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        
        company = Company.builder()
                .id(companyId)
                .name("Test Company")
                .slug("test-company")
                .isActive(true)
                .build();
        
        customer = Customer.builder()
                .id(customerId)
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .profileUrl("profile.jpg")
                .isBlocked(false)
                .company(company)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        createDTO = CreateCustomerDTO.builder()
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .profileUrl("profile.jpg")
                // companyId é obtido do contexto, não passado no DTO
                .isBlocked(false)
                .build();
        
        updateDTO = UpdateCustomerDTO.builder()
                .name("João Silva Atualizado")
                .whatsappId("wa_001_updated")
                .build();
    }
    
    @Test
    void create_ShouldCreateCustomer_WhenValidData() {
        when(customerRepository.existsByPhoneAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.existsByWhatsappIdAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        
        CustomerDTO result = customerService.create(createDTO, companyId);
        
        assertThat(result).isNotNull();
        assertThat(result.getPhone()).isEqualTo("+5511999999001");
        assertThat(result.getName()).isEqualTo("João Silva");
        assertThat(result.getWhatsappId()).isEqualTo("wa_001");
        assertThat(result.getIsBlocked()).isFalse();
        
        verify(customerRepository).existsByPhoneAndCompanyId("+5511999999001", companyId);
        verify(customerRepository).existsByWhatsappIdAndCompanyId("wa_001", companyId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenPhoneAlreadyExists() {
        when(customerRepository.existsByPhoneAndCompanyId(anyString(), any(UUID.class))).thenReturn(true);
        
        assertThatThrownBy(() -> customerService.create(createDTO, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
        
        verify(customerRepository).existsByPhoneAndCompanyId("+5511999999001", companyId);
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    void create_ShouldThrowException_WhenWhatsappIdAlreadyExists() {
        when(customerRepository.existsByPhoneAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.existsByWhatsappIdAndCompanyId(anyString(), any(UUID.class))).thenReturn(true);
        
        assertThatThrownBy(() -> customerService.create(createDTO, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já existe");
        
        verify(customerRepository).existsByPhoneAndCompanyId("+5511999999001", companyId);
        verify(customerRepository).existsByWhatsappIdAndCompanyId("wa_001", companyId);
        verify(customerRepository, never()).save(any(Customer.class));
    }
    
    @Test
    void findById_ShouldReturnCustomer_WhenExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        CustomerDTO result = customerService.findById(customerId, companyId);
        
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customerId);
        assertThat(result.getPhone()).isEqualTo("+5511999999001");
        
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    void findById_ShouldThrowException_WhenNotExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> customerService.findById(customerId, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(customerRepository).findById(customerId);
    }
    
    @Test
    void findByPhone_ShouldReturnCustomer_WhenExists() {
        when(customerRepository.findByPhoneAndCompanyId("+5511999999001", companyId)).thenReturn(Optional.of(customer));
        
        CustomerDTO result = customerService.findByPhoneAndCompany("+5511999999001", companyId);
        
        assertThat(result).isNotNull();
        assertThat(result.getPhone()).isEqualTo("+5511999999001");
        
        verify(customerRepository).findByPhoneAndCompanyId("+5511999999001", companyId);
    }
    
    @Test
    void findAll_ShouldReturnAllActiveCustomers() {
        when(customerRepository.findByCompanyId(companyId)).thenReturn(List.of(customer));
        
        List<CustomerDTO> result = customerService.findAllByCompany(companyId);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("João Silva");
        
        verify(customerRepository).findByCompanyId(companyId);
    }
    
    
    
    @Test
    void update_ShouldUpdateCustomer_WhenValidData() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByWhatsappIdAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        
        CustomerDTO result = customerService.update(customerId, updateDTO, companyId);
        
        assertThat(result).isNotNull();
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    void blockCustomer_ShouldBlockCustomer_WhenExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        
        CustomerDTO result = customerService.blockCustomer(customerId, companyId);
        
        assertThat(result).isNotNull();
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    void unblockCustomer_ShouldUnblockCustomer_WhenExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        
        CustomerDTO result = customerService.unblockCustomer(customerId, companyId);
        
        assertThat(result).isNotNull();
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).save(any(Customer.class));
    }
    
    @Test
    void delete_ShouldDeleteCustomer_WhenExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        customerService.delete(customerId, companyId);
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository).deleteById(customerId);
    }
    
    @Test
    void delete_ShouldThrowException_WhenNotExists() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> customerService.delete(customerId, companyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não encontrado");
        
        verify(customerRepository).findById(customerId);
        verify(customerRepository, never()).deleteById(any());
    }
    
    @Test
    void normalizePhoneNumber_ShouldFormatCorrectly() {
        // Test cases for phone normalization
        assertThat(customerService.normalizePhoneNumber("11999999999")).isEqualTo("+5511999999999");
        assertThat(customerService.normalizePhoneNumber("5511999999999")).isEqualTo("+5511999999999");
        assertThat(customerService.normalizePhoneNumber("+5511999999999")).isEqualTo("+5511999999999");
        assertThat(customerService.normalizePhoneNumber("(11) 99999-9999")).isEqualTo("+5511999999999");
        assertThat(customerService.normalizePhoneNumber(null)).isNull();
    }
}