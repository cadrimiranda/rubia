package com.ruby.rubia_server.config;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyContextResolverTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private CompanyContextResolver companyContextResolver;

    private CompanyGroup companyGroup;
    private Company company;
    private UUID companyGroupId;

    @BeforeEach
    void setUp() {
        companyGroupId = UUID.randomUUID();
        companyGroup = CompanyGroup.builder()
                .id(companyGroupId)
                .name("Test Company Group")
                .build();

        company = Company.builder()
                .id(UUID.randomUUID())
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .build();
    }

    @Test
    void resolveCompany_ShouldReturnCompany_WhenCompanyGroupIdInRequest() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(companyGroupId);
        when(companyRepository.findByCompanyGroupId(companyGroupId)).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(company, result.get());
    }

    @Test
    void resolveCompany_ShouldReturnEmpty_WhenCompanyGroupIdNotFoundInRepository() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(companyGroupId);
        when(companyRepository.findByCompanyGroupId(companyGroupId)).thenReturn(Optional.empty());

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveCompany_ShouldFallbackToOriginHeader_WhenNoCompanyGroupIdInRequest() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn("http://test-company.localhost:3000");
        when(companyRepository.findBySlug("test-company")).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(company, result.get());
    }

    @Test
    void resolveCompany_ShouldFallbackToHostHeader_WhenNoOriginHeader() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn(null);
        when(httpServletRequest.getHeader("Host")).thenReturn("test-company.localhost:3000");
        when(companyRepository.findBySlug("test-company")).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(company, result.get());
    }

    @Test
    void resolveCompany_ShouldReturnEmpty_WhenNoHeadersPresent() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn(null);
        when(httpServletRequest.getHeader("Host")).thenReturn(null);

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveCompany_ShouldReturnEmpty_WhenCompanySlugNotFound() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn("http://nonexistent.localhost:3000");
        when(companyRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveCompany_ShouldHandleOriginWithoutPort() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn("http://test-company.localhost");
        when(companyRepository.findBySlug("test-company")).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(company, result.get());
    }

    @Test
    void resolveCompany_ShouldHandleHttpsOrigin() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn("https://test-company.rubia.com");
        when(companyRepository.findBySlug("test-company")).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(company, result.get());
    }

    @Test
    void resolveCompany_ShouldHandleMalformedOriginGracefully() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn("not-a-valid-url");
        when(httpServletRequest.getHeader("Host")).thenReturn("test-company.localhost:3000");
        when(companyRepository.findBySlug("test-company")).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent());
        assertEquals(company, result.get());
    }

    @Test
    void getCompanyId_ShouldReturnCompanyId_WhenCompanyExists() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(companyGroupId);
        when(companyRepository.findByCompanyGroupId(companyGroupId)).thenReturn(Optional.of(company));

        // When
        UUID result = companyContextResolver.getCompanyId(httpServletRequest);

        // Then
        assertEquals(company.getId(), result);
    }

    @Test
    void getCompanyId_ShouldReturnNull_WhenCompanyNotFound() {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(companyGroupId);
        when(companyRepository.findByCompanyGroupId(companyGroupId)).thenReturn(Optional.empty());

        // When
        UUID result = companyContextResolver.getCompanyId(httpServletRequest);

        // Then
        assertNull(result);
    }

    @Test
    void resolveCompany_ShouldExtractSubdomainCorrectly_FromVariousHostFormats() {
        // Test cases for different host formats
        testSubdomainExtraction("company1.localhost", "company1");
        testSubdomainExtraction("company2.localhost:3000", "company2");
        testSubdomainExtraction("company3.rubia.com", "company3");
        testSubdomainExtraction("company4.rubia.com:8080", "company4");
    }

    private void testSubdomainExtraction(String host, String expectedSlug) {
        // Given
        when(httpServletRequest.getAttribute("companyGroupId")).thenReturn(null);
        when(httpServletRequest.getHeader("Origin")).thenReturn(null);
        when(httpServletRequest.getHeader("Host")).thenReturn(host);
        when(companyRepository.findBySlug(expectedSlug)).thenReturn(Optional.of(company));

        // When
        Optional<Company> result = companyContextResolver.resolveCompany(httpServletRequest);

        // Then
        assertTrue(result.isPresent(), "Should find company for host: " + host);
        assertEquals(company, result.get());
    }
}