package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyService {
    
    @Autowired
    private CompanyRepository companyRepository;
    
    public List<Company> findAll() {
        return companyRepository.findAll();
    }
    
    public List<Company> findActiveCompanies() {
        return companyRepository.findByIsActiveTrue();
    }
    
    public Optional<Company> findById(UUID id) {
        return companyRepository.findById(id);
    }
    
    public Optional<Company> findBySlug(String slug) {
        return companyRepository.findBySlug(slug);
    }
    
    public Company save(Company company) {
        return companyRepository.save(company);
    }
    
    public Company create(Company company) {
        if (companyRepository.existsBySlug(company.getSlug())) {
            throw new RuntimeException("Company with slug already exists: " + company.getSlug());
        }
        return companyRepository.save(company);
    }
    
    public Company update(UUID id, Company companyData) {
        Company existingCompany = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found: " + id));
        
        existingCompany.setName(companyData.getName());
        existingCompany.setDescription(companyData.getDescription());
        existingCompany.setContactEmail(companyData.getContactEmail());
        existingCompany.setContactPhone(companyData.getContactPhone());
        existingCompany.setLogoUrl(companyData.getLogoUrl());
        existingCompany.setIsActive(companyData.getIsActive());
        existingCompany.setPlanType(companyData.getPlanType());
        existingCompany.setMaxUsers(companyData.getMaxUsers());
        existingCompany.setMaxWhatsappNumbers(companyData.getMaxWhatsappNumbers());
        
        return companyRepository.save(existingCompany);
    }
    
    public void deleteById(UUID id) {
        companyRepository.deleteById(id);
    }
    
    public boolean existsBySlug(String slug) {
        return companyRepository.existsBySlug(slug);
    }
    
    public List<Company> findByPlanType(String planType) {
        return companyRepository.findByPlanType(planType);
    }
}