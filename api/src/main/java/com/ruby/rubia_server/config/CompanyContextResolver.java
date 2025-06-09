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
        String host = request.getHeader("Origin");
        if (host != null) {
            // Extract host from Origin URL (http://rubia.localhost:3000 -> rubia.localhost:3000)
            try {
                java.net.URL url = new java.net.URL(host);
                host = url.getHost();
                if (url.getPort() != -1) {
                    host = host + ":" + url.getPort();
                }
            } catch (Exception e) {
                log.debug("Failed to parse Origin header: {}", host);
                host = null;
            }
        }
        
        // Fallback to Host header if Origin not available
        if (host == null) {
            host = request.getHeader("Host");
        }
        
        if (host == null) {
            log.warn("No Origin or Host header found in request");
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
        
//        if (host.equals("localhost") || host.startsWith("localhost:") || host.equals("rubia.com") || host.equals("www.rubia.com")) {
//            return null;
//        }
//
//        // Handle rubia.localhost pattern
//        if (host.equals("rubia.localhost") || host.startsWith("rubia.localhost:")) {
//            return "rubia";
//        }

        // Extract subdomain
        String[] parts = host.split("\\.");
        if (parts.length >= 1) {
            // company.rubia.com
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