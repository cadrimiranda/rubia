package com.ruby.rubia_server.config;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyContextResolver {

    private final CompanyRepository companyRepository;

    public Optional<Company> resolveCompany(HttpServletRequest request) {
        // Try to get company from JWT token first (if authenticated)
        UUID companyIdFromToken = (UUID) request.getAttribute("companyId");
        if (companyIdFromToken != null) {
            return companyRepository.findById(companyIdFromToken);
        }

        // Fallback to subdomain resolution
        String host = request.getHeader("Host");
        if (host == null) {
            log.warn("No Host header found in request");
            return Optional.empty();
        }

        String companySlug = extractCompanySlugFromHost(host);
        if (companySlug == null) {
            log.debug("No company slug found in host: {}", host);
            return Optional.empty();
        }

        Optional<Company> company = companyRepository.findBySlug(companySlug);
        if (company.isEmpty()) {
            log.warn("Company not found for slug: {}", companySlug);
        }

        return company;
    }

    private String extractCompanySlugFromHost(String host) {
        // Remove port if present
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }

        // Handle different patterns:
        // company.rubia.com -> company
        // localhost -> null (development)
        // rubia.com -> null (main domain)
        
        if (host.equals("localhost") || host.equals("rubia.com") || host.equals("www.rubia.com")) {
            return null;
        }

        // Extract subdomain
        String[] parts = host.split("\\.");
        if (parts.length >= 3) {
            // company.rubia.com
            return parts[0];
        }

        // If only one part and not localhost, treat as company slug for development
        if (parts.length == 1 && !host.equals("localhost")) {
            return parts[0];
        }

        return null;
    }

    public UUID getCompanyId(HttpServletRequest request) {
        return resolveCompany(request)
                .map(Company::getId)
                .orElse(null);
    }
}