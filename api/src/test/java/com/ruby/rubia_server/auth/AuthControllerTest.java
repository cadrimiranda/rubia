package com.ruby.rubia_server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RefreshRequest refreshRequest;
    private AuthResponse authResponse;
    private Company testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new Company();
        testCompany.setId(UUID.randomUUID());
        testCompany.setName("Test Company");
        testCompany.setSlug("test");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        refreshRequest = new RefreshRequest();
        refreshRequest.setRefreshToken("refresh-token");

        UserInfo userInfo = UserInfo.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .role("AGENT")
                .companyId(testCompany.getId())
                .companyName(testCompany.getName())
                .companySlug(testCompany.getSlug())
                .build();

        authResponse = AuthResponse.builder()
                .token("jwt-token")
                .user(userInfo)
                .expiresIn(3600)
                .companyId(testCompany.getId().toString())
                .companySlug(testCompany.getSlug())
                .build();
    }

    @Test
    @WithMockUser
    void login_WithValidCredentials_ShouldReturnAuthResponse() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.role").value("AGENT"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.companyId").value(testCompany.getId().toString()))
                .andExpect(jsonPath("$.companySlug").value("test"));

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @WithMockUser
    void login_WithInvalidCredentials_ShouldReturnError() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @WithMockUser
    void login_WithCompanyNotFound_ShouldReturnError() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Company not found. Please check the subdomain."));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @WithMockUser
    void login_WithUserNotFoundInCompany_ShouldReturnError() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("User not found for this company"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).login(any(LoginRequest.class));
    }

    @Test
    @WithMockUser
    void refresh_WithValidToken_ShouldReturnNewAuthResponse() throws Exception {
        when(authService.refresh(any(RefreshRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.companySlug").value("test"));

        verify(authService, times(1)).refresh(any(RefreshRequest.class));
    }

    @Test
    @WithMockUser
    void refresh_WithInvalidToken_ShouldReturnError() throws Exception {
        when(authService.refresh(any(RefreshRequest.class)))
                .thenThrow(new RuntimeException("Invalid refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isInternalServerError());

        verify(authService, times(1)).refresh(any(RefreshRequest.class));
    }

    @Test
    void login_WithoutAuthentication_ShouldRequireNoAuth() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void login_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void refresh_WithMalformedJson_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"invalid-json"))
                .andExpect(status().isBadRequest());
    }
}