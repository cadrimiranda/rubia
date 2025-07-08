package com.ruby.rubia_server.core.base;

import com.ruby.rubia_server.core.entity.Company;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BaseEntity {
    UUID getId();
    void setId(UUID id);
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
    Company getCompany();
    void setCompany(Company company);
    
    // Métodos utilitários
    default boolean isNew() {
        return getId() == null;
    }
    
    default boolean belongsToCompany(UUID companyId) {
        return getCompany() != null && getCompany().getId().equals(companyId);
    }
}