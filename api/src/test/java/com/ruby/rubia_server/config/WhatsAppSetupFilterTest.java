package com.ruby.rubia_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.service.WhatsAppInstanceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppSetupFilterTest {

    @Mock
    private WhatsAppInstanceService whatsappInstanceService;

    @Mock
    private CompanyContextResolver companyContextResolver;

    @Mock
    private JwtService jwtService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private WhatsAppSetupFilter whatsappSetupFilter;

    private Company testCompany;
    private StringWriter responseWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {
        CompanyGroup companyGroup = CompanyGroup.builder()
            .id(UUID.randomUUID())
            .name("Test Group")
            .build();

        testCompany = Company.builder()
            .id(UUID.randomUUID())
            .name("Test Company")
            .slug("test-company")
            .companyGroup(companyGroup)
            .build();

        responseWriter = new StringWriter();
        printWriter = new PrintWriter(responseWriter);
    }

    @Test
    void doFilterInternal_WithExcludedPath_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithNonApiPath_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/public/index.html");

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithoutAuthorizationHeader_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithInvalidAuthorizationHeader_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("InvalidHeader");

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("invalid-token", "user@example.com")).thenReturn(false);

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithoutCompanyContext_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
        when(companyContextResolver.resolveCompany(request)).thenReturn(Optional.empty());

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithConfiguredWhatsApp_ShouldProceed() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
        when(companyContextResolver.resolveCompany(request)).thenReturn(Optional.of(testCompany));
        when(whatsappInstanceService.hasConfiguredInstance(testCompany)).thenReturn(true);

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService).hasConfiguredInstance(testCompany);
        verify(response, never()).setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
    }

    @Test
    void doFilterInternal_WithoutConfiguredWhatsApp_ShouldReturn428() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(true);
        when(companyContextResolver.resolveCompany(request)).thenReturn(Optional.of(testCompany));
        when(whatsappInstanceService.hasConfiguredInstance(testCompany)).thenReturn(false);
        when(response.getWriter()).thenReturn(printWriter);
        
        String expectedJson = "{\"error\":\"WhatsApp setup required\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedJson);

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);
        verify(filterChain, never()).doFilter(request, response);
        verify(whatsappInstanceService).hasConfiguredInstance(testCompany);
        
        // Verify the error response was written
        ArgumentCaptor<Object> errorResponseCaptor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(errorResponseCaptor.capture());
        
        Object errorResponse = errorResponseCaptor.getValue();
        assertNotNull(errorResponse);
    }

    @Test
    void doFilterInternal_WithWhatsAppSetupPath_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/whatsapp-setup/status");

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithZApiActivationPath_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/zapi/activation/status");

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithActuatorPath_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithException_ShouldProceedWithoutBlocking() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenThrow(new RuntimeException("JWT parsing error"));

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        // Should not block the request even if there's an exception
    }

    @Test
    void doFilterInternal_WithNullUsernameFromToken_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn(null);

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }

    @Test
    void doFilterInternal_WithValidTokenButInvalidUser_ShouldSkipFilter() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/conversations");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("valid-token", "user@example.com")).thenReturn(false);

        // Act
        whatsappSetupFilter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(whatsappInstanceService, never()).hasConfiguredInstance(any());
    }
}