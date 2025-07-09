package com.ruby.rubia_server.auth;

import com.ruby.rubia_server.config.CompanyContextResolver;
import com.ruby.rubia_server.config.JwtService;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CompanyContextResolver companyContextResolver;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @InjectMocks
    private AuthService authService;

    private CompanyGroup companyGroup;
    private Company company;
    private User user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        companyGroup = CompanyGroup.builder()
                .id(UUID.randomUUID())
                .name("Test Company Group")
                .build();

        company = Company.builder()
                .id(UUID.randomUUID())
                .name("Test Company")
                .slug("test-company")
                .companyGroup(companyGroup)
                .build();

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .role(UserRole.ADMIN)
                .company(company)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        // Given
        try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
            mockedRequestContextHolder.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
            when(companyContextResolver.resolveCompany(httpServletRequest)).thenReturn(Optional.of(company));
            when(userRepository.findByEmailAndCompanyGroupId(loginRequest.getEmail(), companyGroup.getId()))
                    .thenReturn(Optional.of(user));
            when(jwtService.generateToken(anyString(), any(UUID.class), anyString()))
                    .thenReturn("jwt-token");

            // When
            AuthResponse response = authService.login(loginRequest);

            // Then
            assertNotNull(response);
            assertEquals("jwt-token", response.getToken());
            assertEquals(company.getId().toString(), response.getCompanyId());
            assertEquals(company.getSlug(), response.getCompanySlug());
            assertNotNull(response.getUser());
            assertEquals(user.getId(), response.getUser().getId());
            assertEquals(user.getName(), response.getUser().getName());
            assertEquals(user.getEmail(), response.getUser().getEmail());
            assertEquals(companyGroup.getId(), response.getUser().getCompanyGroupId());
            assertEquals(companyGroup.getName(), response.getUser().getCompanyGroupName());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateToken(user.getEmail(), companyGroup.getId(), company.getSlug());
        }
    }

    @Test
    void login_ShouldThrowException_WhenCompanyNotFound() {
        // Given
        try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
            mockedRequestContextHolder.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
            when(companyContextResolver.resolveCompany(httpServletRequest)).thenReturn(Optional.empty());

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class, 
                () -> authService.login(loginRequest));
            assertEquals("Company not found. Please check the subdomain.", exception.getMessage());
        }
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFoundInCompanyGroup() {
        // Given
        try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
            mockedRequestContextHolder.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
            when(companyContextResolver.resolveCompany(httpServletRequest)).thenReturn(Optional.of(company));
            when(userRepository.findByEmailAndCompanyGroupId(loginRequest.getEmail(), companyGroup.getId()))
                    .thenReturn(Optional.empty());

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class, 
                () -> authService.login(loginRequest));
            assertEquals("User not found for this company group", exception.getMessage());
        }
    }

    @Test
    void login_ShouldThrowException_WhenAuthenticationFails() {
        // Given
        try (MockedStatic<RequestContextHolder> mockedRequestContextHolder = mockStatic(RequestContextHolder.class)) {
            mockedRequestContextHolder.when(RequestContextHolder::currentRequestAttributes)
                    .thenReturn(servletRequestAttributes);
            when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
            when(companyContextResolver.resolveCompany(httpServletRequest)).thenReturn(Optional.of(company));
            when(userRepository.findByEmailAndCompanyGroupId(loginRequest.getEmail(), companyGroup.getId()))
                    .thenReturn(Optional.of(user));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // When & Then
            AuthenticationException exception = assertThrows(AuthenticationException.class, 
                () -> authService.login(loginRequest));
            assertEquals("Invalid credentials", exception.getMessage());
        }
    }

    @Test
    void refresh_ShouldReturnAuthResponse_WhenTokenIsValid() {
        // Given
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");
        
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("valid-refresh-token", user.getEmail())).thenReturn(true);
        when(jwtService.generateToken(anyString(), any(UUID.class), anyString())).thenReturn("new-jwt-token");

        // When
        AuthResponse response = authService.refresh(refreshRequest);

        // Then
        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals(company.getId().toString(), response.getCompanyId());
        assertEquals(company.getSlug(), response.getCompanySlug());
        assertNotNull(response.getUser());
        assertEquals(companyGroup.getId(), response.getUser().getCompanyGroupId());
        assertEquals(companyGroup.getName(), response.getUser().getCompanyGroupName());

        verify(jwtService).generateToken(user.getEmail(), companyGroup.getId(), company.getSlug());
    }

    @Test
    void refresh_ShouldThrowException_WhenUserNotFound() {
        // Given
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("valid-refresh-token");
        
        when(jwtService.extractUsername("valid-refresh-token")).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, 
            () -> authService.refresh(refreshRequest));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void refresh_ShouldThrowException_WhenTokenIsInvalid() {
        // Given
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("invalid-refresh-token");
        
        when(jwtService.extractUsername("invalid-refresh-token")).thenReturn(user.getEmail());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid("invalid-refresh-token", user.getEmail())).thenReturn(false);

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, 
            () -> authService.refresh(refreshRequest));
        assertEquals("Invalid refresh token", exception.getMessage());
    }

    @Test
    void refresh_ShouldThrowException_WhenUsernameIsNull() {
        // Given
        RefreshRequest refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("token-with-null-username");
        
        when(jwtService.extractUsername("token-with-null-username")).thenReturn(null);

        // When & Then
        AuthenticationException exception = assertThrows(AuthenticationException.class, 
            () -> authService.refresh(refreshRequest));
        assertEquals("Invalid refresh token", exception.getMessage());
    }
}