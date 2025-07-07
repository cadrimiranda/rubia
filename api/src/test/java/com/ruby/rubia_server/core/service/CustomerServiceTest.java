package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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

    private Company company;
    private Customer customer;
    private CreateCustomerDTO createCustomerDTO;
    private UUID companyId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        company = Company.builder().id(companyId).name("Test Company").build();

        createCustomerDTO = CreateCustomerDTO.builder()
                .name("Test Customer")
                .phone("+5511987654321")
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name(createCustomerDTO.getName())
                .phone(createCustomerDTO.getPhone())
                .company(company)
                .build();
    }

    @Test
    void createCustomer_Success() {
        // Arrange
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.existsByPhoneAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // Act
        CustomerDTO result = customerService.create(createCustomerDTO, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(customer.getId(), result.getId());
        assertEquals(createCustomerDTO.getName(), result.getName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void createCustomer_FailsWhenPhoneExists() {
        // Arrange
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.existsByPhoneAndCompanyId(createCustomerDTO.getPhone(), companyId)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            customerService.create(createCustomerDTO, companyId);
        });
        assertEquals("Cliente com telefone '" + createCustomerDTO.getPhone() + "' já existe nesta empresa", exception.getMessage());
        verify(customerRepository, never()).save(any(Customer.class));
    }
}
