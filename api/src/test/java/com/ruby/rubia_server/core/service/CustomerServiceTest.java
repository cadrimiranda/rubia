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

import java.time.LocalDate;
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
                .bloodType("O+")
                .height(175)
                .weight(70.5)
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

        customer = Customer.builder()
                .id(customerId)
                .name(createCustomerDTO.getName())
                .phone(createCustomerDTO.getPhone())
                .bloodType(createCustomerDTO.getBloodType())
                .height(createCustomerDTO.getHeight())
                .weight(createCustomerDTO.getWeight())
                .birthDate(createCustomerDTO.getBirthDate())
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
        assertEquals(createCustomerDTO.getPhone(), result.getPhone());
        assertEquals(createCustomerDTO.getBloodType(), result.getBloodType());
        assertEquals(createCustomerDTO.getHeight(), result.getHeight());
        assertEquals(createCustomerDTO.getWeight(), result.getWeight());
        assertEquals(createCustomerDTO.getBirthDate(), result.getBirthDate());
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

    @Test
    void createCustomer_WithAllFields_Success() {
        // Arrange
        CreateCustomerDTO fullCustomerDTO = CreateCustomerDTO.builder()
                .name("João Silva")
                .phone("+5511999887766")
                .bloodType("AB+")
                .height(180)
                .weight(85.0)
                .birthDate(LocalDate.of(1985, 5, 15))
                .lastDonationDate(LocalDate.of(2023, 12, 1))
                .nextEligibleDonationDate(LocalDate.of(2024, 6, 1))
                .profileUrl("https://example.com/profile.jpg")
                .build();

        Customer fullCustomer = Customer.builder()
                .id(customerId)
                .name(fullCustomerDTO.getName())
                .phone(fullCustomerDTO.getPhone())
                .bloodType(fullCustomerDTO.getBloodType())
                .height(fullCustomerDTO.getHeight())
                .weight(fullCustomerDTO.getWeight())
                .birthDate(fullCustomerDTO.getBirthDate())
                .lastDonationDate(fullCustomerDTO.getLastDonationDate())
                .nextEligibleDonationDate(fullCustomerDTO.getNextEligibleDonationDate())
                .profileUrl(fullCustomerDTO.getProfileUrl())
                .company(company)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.existsByPhoneAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(fullCustomer);

        // Act
        CustomerDTO result = customerService.create(fullCustomerDTO, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(fullCustomer.getId(), result.getId());
        assertEquals(fullCustomerDTO.getName(), result.getName());
        assertEquals(fullCustomerDTO.getPhone(), result.getPhone());
        assertEquals(fullCustomerDTO.getBloodType(), result.getBloodType());
        assertEquals(fullCustomerDTO.getHeight(), result.getHeight());
        assertEquals(fullCustomerDTO.getWeight(), result.getWeight());
        assertEquals(fullCustomerDTO.getBirthDate(), result.getBirthDate());
        assertEquals(fullCustomerDTO.getLastDonationDate(), result.getLastDonationDate());
        assertEquals(fullCustomerDTO.getNextEligibleDonationDate(), result.getNextEligibleDonationDate());
        assertEquals(fullCustomerDTO.getProfileUrl(), result.getProfileUrl());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void createCustomer_WithOptionalFields_Success() {
        // Arrange
        CreateCustomerDTO minimalCustomerDTO = CreateCustomerDTO.builder()
                .name("Maria Santos")
                .phone("+5511888777666")
                .build();

        Customer minimalCustomer = Customer.builder()
                .id(customerId)
                .name(minimalCustomerDTO.getName())
                .phone(minimalCustomerDTO.getPhone())
                .company(company)
                .build();

        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(customerRepository.existsByPhoneAndCompanyId(anyString(), any(UUID.class))).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(minimalCustomer);

        // Act
        CustomerDTO result = customerService.create(minimalCustomerDTO, companyId);

        // Assert
        assertNotNull(result);
        assertEquals(minimalCustomer.getId(), result.getId());
        assertEquals(minimalCustomerDTO.getName(), result.getName());
        assertEquals(minimalCustomerDTO.getPhone(), result.getPhone());
        assertNull(result.getBloodType());
        assertNull(result.getHeight());
        assertNull(result.getWeight());
        assertNull(result.getBirthDate());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }
}
