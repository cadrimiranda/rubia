package com.ruby.rubia_server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.service.WhatsAppInstanceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppSetupFilter extends OncePerRequestFilter {

    private final WhatsAppInstanceService whatsappInstanceService;
    private final CompanyContextResolver companyContextResolver;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    // URLs that don't require WhatsApp setup
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/whatsapp-setup",
        "/api/zapi/activation",
        "/actuator",
        "/error",
        "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        log.debug("Processing WhatsApp setup filter for URI: {}", requestURI);

        // Skip filter for excluded paths
        if (shouldSkipFilter(requestURI)) {
            log.debug("Skipping WhatsApp setup filter for excluded path: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        // Skip for non-API requests
        if (!requestURI.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Get JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                // No token, let security handle it
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            
            // Validate token and get company context
            String username = jwtService.extractUsername(token);
            if (username == null || !jwtService.isTokenValid(token, username)) {
                filterChain.doFilter(request, response);
                return;
            }

            // Resolve company from context
            Optional<Company> companyOpt = companyContextResolver.resolveCompany(request);
            if (companyOpt.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            Company company = companyOpt.get();
            
            // Check if company needs WhatsApp setup
            if (!whatsappInstanceService.hasConfiguredInstance(company)) {
                log.info("Company {} requires WhatsApp setup, redirecting", company.getSlug());
                
                // Return 428 Precondition Required with setup requirement
                response.setStatus(HttpStatus.PRECONDITION_REQUIRED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                var errorResponse = new WhatsAppSetupRequiredResponse(
                    "WhatsApp setup required",
                    "Company must configure at least one WhatsApp instance before accessing this resource",
                    "/api/whatsapp-setup"
                );
                
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            log.debug("Company {} has configured WhatsApp instances, proceeding", company.getSlug());
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in WhatsApp setup filter", e);
            filterChain.doFilter(request, response);
        }
    }

    private boolean shouldSkipFilter(String requestURI) {
        return EXCLUDED_PATHS.stream().anyMatch(requestURI::startsWith);
    }

    // Response class for WhatsApp setup requirement
    public record WhatsAppSetupRequiredResponse(
        String error,
        String message,
        String setupUrl
    ) {}
}