package com.ruby.rubia_server.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.MessagingProvider;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.WhatsAppInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc 
@Transactional
class WhatsAppSetupControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private WhatsAppInstanceRepository whatsappInstanceRepository;

    private Company testCompany;
    private WhatsAppInstance testInstance;

    @BeforeEach
    void setUp() {
        // Clean up
        whatsappInstanceRepository.deleteAll();
        companyRepository.deleteAll();
        companyGroupRepository.deleteAll();

        // Create test data
        CompanyGroup companyGroup = CompanyGroup.builder()
            .name("Test Group")
            .description("Test Group Description")
            .build();
        companyGroup = companyGroupRepository.save(companyGroup);

        testCompany = Company.builder()
            .name("Test Company")
            .slug("test-company")
            .companyGroup(companyGroup)
            .maxWhatsappNumbers(3)
            .isActive(true)
            .build();
        testCompany = companyRepository.save(testCompany);

        testInstance = WhatsAppInstance.builder()
            .company(testCompany)
            .phoneNumber("5511999999999")
            .displayName("Test WhatsApp")
            .provider(MessagingProvider.Z_API)
            .isActive(true)
            .isPrimary(true)
            .createdAt(LocalDateTime.now())
            .build();
        testInstance = whatsappInstanceRepository.save(testInstance);
    }

    @Test
    @WithMockUser
    void getSetupStatus_WithConfiguredInstance_ShouldReturnCorrectStatus() throws Exception {
        mockMvc.perform(get("/api/whatsapp-setup/status")
                .header("Host", "test-company.localhost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiresSetup").value(false))
            .andExpect(jsonPath("$.hasConfiguredInstance").value(true))
            .andExpect(jsonPath("$.hasConfiguredInstance").value(true))
            .andExpect(jsonPath("$.totalInstances").value(1))
            .andExpect(jsonPath("$.maxAllowedInstances").value(3))
            .andExpect(jsonPath("$.instances", hasSize(1)))
            .andExpect(jsonPath("$.instances[0].phoneNumber").value("5511999999999"))
            .andExpect(jsonPath("$.instances[0].isPrimary").value(true));
    }

    @Test
    @WithMockUser
    void getSetupStatus_WithNotConfiguredInstance_ShouldReturnRequiresSetup() throws Exception {
        // Update instance to inactive to simulate not configured
        testInstance.setIsActive(false);
        whatsappInstanceRepository.save(testInstance);

        mockMvc.perform(get("/api/whatsapp-setup/status")
                .header("Host", "test-company.localhost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.requiresSetup").value(true))
            .andExpect(jsonPath("$.hasConfiguredInstance").value(false))
            .andExpect(jsonPath("$.hasConfiguredInstance").value(false));
    }

    @Test
    @WithMockUser
    void createInstance_ValidRequest_ShouldCreateInstance() throws Exception {
        Map<String, String> request = Map.of(
            "phoneNumber", "5511988887777",
            "displayName", "New WhatsApp Instance"
        );

        mockMvc.perform(post("/api/whatsapp-setup/create-instance")
                .header("Host", "test-company.localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.phoneNumber").value("5511988887777"))
            .andExpect(jsonPath("$.displayName").value("New WhatsApp Instance"))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.isPrimary").value(false)); // Should not be primary (second instance)
    }

    @Test
    @WithMockUser
    void createInstance_ExceedsLimit_ShouldReturnBadRequest() throws Exception {
        // Create instances up to the limit
        testCompany.setMaxWhatsappNumbers(1);
        companyRepository.save(testCompany);

        Map<String, String> request = Map.of(
            "phoneNumber", "5511988887777",
            "displayName", "Over Limit Instance"
        );

        mockMvc.perform(post("/api/whatsapp-setup/create-instance")
                .header("Host", "test-company.localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createInstance_DuplicatePhone_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = Map.of(
            "phoneNumber", "5511999999999", // Same as existing instance
            "displayName", "Duplicate Phone Instance"
        );

        mockMvc.perform(post("/api/whatsapp-setup/create-instance")
                .header("Host", "test-company.localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void configureInstance_ValidRequest_ShouldUpdateConfiguration() throws Exception {
        Map<String, String> request = Map.of(
            "instanceId", "ZAPI123456",
            "accessToken", "token123456"
        );

        mockMvc.perform(post("/api/whatsapp-setup/" + testInstance.getId() + "/configure")
                .header("Host", "test-company.localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instanceId").value("ZAPI123456"))
            .andExpect(jsonPath("$.instanceId").value("ZAPI123456"));
    }

    @Test
    @WithMockUser
    void configureInstance_InvalidInstanceId_ShouldReturnNotFound() throws Exception {
        Map<String, String> request = Map.of(
            "instanceId", "ZAPI123456",
            "accessToken", "token123456"
        );

        mockMvc.perform(post("/api/whatsapp-setup/00000000-0000-0000-0000-000000000000/configure")
                .header("Host", "test-company.localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void activateInstance_ValidRequest_ShouldReturnActivationResult() throws Exception {
        mockMvc.perform(post("/api/whatsapp-setup/" + testInstance.getId() + "/activate")
                .header("Host", "test-company.localhost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instanceId").value(testInstance.getId().toString()))
            .andExpect(jsonPath("$.success").exists());
    }

    @Test
    @WithMockUser
    void setPrimaryInstance_ValidRequest_ShouldUpdatePrimary() throws Exception {
        // Create another instance
        WhatsAppInstance anotherInstance = WhatsAppInstance.builder()
            .company(testCompany)
            .phoneNumber("5511988887777")
            .displayName("Another WhatsApp")
            .provider(MessagingProvider.Z_API)
            .isActive(true)
            .isPrimary(false)
            .build();
        anotherInstance = whatsappInstanceRepository.save(anotherInstance);

        mockMvc.perform(post("/api/whatsapp-setup/" + anotherInstance.getId() + "/set-primary")
                .header("Host", "test-company.localhost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Primary instance updated"));
    }

    @Test
    @WithMockUser
    void deactivateInstance_ValidRequest_ShouldDeactivateInstance() throws Exception {
        mockMvc.perform(delete("/api/whatsapp-setup/" + testInstance.getId())
                .header("Host", "test-company.localhost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Instance deactivated"));
    }

    @Test
    @WithMockUser
    void getAvailableProviders_ShouldReturnProvidersList() throws Exception {
        mockMvc.perform(get("/api/whatsapp-setup/providers")
                .header("Host", "test-company.localhost"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$[0].provider").exists())
            .andExpect(jsonPath("$[0].name").exists())
            .andExpect(jsonPath("$[0].description").exists())
            .andExpect(jsonPath("$[0].available").exists());
    }

    @Test
    void getSetupStatus_WithoutAuthentication_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/whatsapp-setup/status")
                .header("Host", "test-company.localhost"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void createInstance_WithInvalidPhoneFormat_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = Map.of(
            "phoneNumber", "invalid-phone",
            "displayName", "Invalid Phone Instance"
        );

        mockMvc.perform(post("/api/whatsapp-setup/create-instance")
                .header("Host", "test-company.localhost")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createInstance_WithEmptyCompanyContext_ShouldReturnBadRequest() throws Exception {
        Map<String, String> request = Map.of(
            "phoneNumber", "5511988887777",
            "displayName", "No Company Instance"
        );

        mockMvc.perform(post("/api/whatsapp-setup/create-instance")
                // No Host header - no company context
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
}