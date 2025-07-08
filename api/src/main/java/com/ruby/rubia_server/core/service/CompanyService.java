package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CompanyDTO;
import com.ruby.rubia_server.core.dto.CreateCompanyDTO;
import com.ruby.rubia_server.core.dto.UpdateCompanyDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.ruby.rubia_server.core.enums.CompanyPlanType;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyGroupRepository companyGroupRepository;
    
    
    
    public List<CompanyDTO> findAll() {
        return companyRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<CompanyDTO> findActiveCompanies() {
        return companyRepository.findByIsActiveTrue().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<CompanyDTO> findById(UUID id) {
        return companyRepository.findById(id)
                .map(this::toDTO);
    }

    public Optional<CompanyDTO> findBySlug(String slug) {
        return companyRepository.findBySlug(slug)
                .map(this::toDTO);
    }
    
    public CompanyDTO create(CreateCompanyDTO createDTO) {
        log.info("Creating company with slug: {}", createDTO.getSlug());
        if (companyRepository.existsBySlug(createDTO.getSlug())) {
            throw new IllegalArgumentException("Company with slug already exists: " + createDTO.getSlug());
        }

        CompanyGroup companyGroup = companyGroupRepository.findById(createDTO.getCompanyGroupId())
                .orElseThrow(() -> new IllegalArgumentException("Company Group not found: " + createDTO.getCompanyGroupId()));

        Company company = Company.builder()
                .name(createDTO.getName())
                .slug(createDTO.getSlug())
                .description(createDTO.getDescription())
                .contactEmail(createDTO.getContactEmail())
                .contactPhone(createDTO.getContactPhone())
                .logoUrl(createDTO.getLogoUrl())
                .isActive(createDTO.getIsActive())
                .planType(createDTO.getPlanType())
                .maxUsers(createDTO.getMaxUsers())
                .maxWhatsappNumbers(createDTO.getMaxWhatsappNumbers())
                .companyGroup(companyGroup)
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("Company created successfully with id: {}", savedCompany.getId());
        return toDTO(savedCompany);
    }

    public CompanyDTO update(UUID id, UpdateCompanyDTO updateDTO) {
        log.info("Updating company with id: {}", id);
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + id));

        Optional.ofNullable(updateDTO.getName()).ifPresent(existingCompany::setName);
        Optional.ofNullable(updateDTO.getDescription()).ifPresent(existingCompany::setDescription);
        Optional.ofNullable(updateDTO.getContactEmail()).ifPresent(existingCompany::setContactEmail);
        Optional.ofNullable(updateDTO.getContactPhone()).ifPresent(existingCompany::setContactPhone);
        Optional.ofNullable(updateDTO.getLogoUrl()).ifPresent(existingCompany::setLogoUrl);
        Optional.ofNullable(updateDTO.getIsActive()).ifPresent(existingCompany::setIsActive);
        Optional.ofNullable(updateDTO.getPlanType()).ifPresent(existingCompany::setPlanType);
        Optional.ofNullable(updateDTO.getMaxUsers()).ifPresent(existingCompany::setMaxUsers);
        Optional.ofNullable(updateDTO.getMaxWhatsappNumbers()).ifPresent(existingCompany::setMaxWhatsappNumbers);

        Company updatedCompany = companyRepository.save(existingCompany);
        log.info("Company updated successfully with id: {}", updatedCompany.getId());
        return toDTO(updatedCompany);
    }

    public void deleteById(UUID id) {
        companyRepository.deleteById(id);
    }

    public boolean existsBySlug(String slug) {
        return companyRepository.existsBySlug(slug);
    }

    public List<CompanyDTO> findByPlanType(CompanyPlanType planType) {
        return companyRepository.findByPlanType(planType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private CompanyDTO toDTO(Company company) {
        return CompanyDTO.builder()
                .id(company.getId())
                .name(company.getName())
                .slug(company.getSlug())
                .description(company.getDescription())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .logoUrl(company.getLogoUrl())
                .isActive(company.getIsActive())
                .planType(company.getPlanType())
                .maxUsers(company.getMaxUsers())
                .maxWhatsappNumbers(company.getMaxWhatsappNumbers())
                .companyGroupId(company.getCompanyGroup() != null ? company.getCompanyGroup().getId() : null)
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .build();
    }
}