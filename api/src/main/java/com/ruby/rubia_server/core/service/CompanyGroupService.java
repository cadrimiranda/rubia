package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CompanyGroupDTO;
import com.ruby.rubia_server.core.dto.CreateCompanyGroupDTO;
import com.ruby.rubia_server.core.dto.UpdateCompanyGroupDTO;
import com.ruby.rubia_server.core.entity.CompanyGroup;
import com.ruby.rubia_server.core.repository.CompanyGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompanyGroupService {

    private final CompanyGroupRepository companyGroupRepository;

    public CompanyGroupDTO create(CreateCompanyGroupDTO createDTO) {
        log.info("Creating company group with name: {}", createDTO.getName());
        if (companyGroupRepository.findByName(createDTO.getName()).isPresent()) {
            throw new IllegalArgumentException("Company group with name '" + createDTO.getName() + "' already exists");
        }

        CompanyGroup companyGroup = CompanyGroup.builder()
                .name(createDTO.getName())
                .description(createDTO.getDescription())
                .isActive(createDTO.getIsActive())
                .build();

        CompanyGroup savedCompanyGroup = companyGroupRepository.save(companyGroup);
        log.info("Company group created successfully with id: {}", savedCompanyGroup.getId());
        return toDTO(savedCompanyGroup);
    }

    @Transactional(readOnly = true)
    public List<CompanyGroupDTO> findAll() {
        return companyGroupRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CompanyGroupDTO findById(UUID id) {
        return companyGroupRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Company group not found with id: " + id));
    }

    public CompanyGroupDTO update(UUID id, UpdateCompanyGroupDTO updateDTO) {
        log.info("Updating company group with id: {}", id);
        CompanyGroup existingCompanyGroup = companyGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Company group not found with id: " + id));

        if (updateDTO.getName() != null && !updateDTO.getName().equals(existingCompanyGroup.getName())) {
            if (companyGroupRepository.findByName(updateDTO.getName()).isPresent()) {
                throw new IllegalArgumentException("Company group with name '" + updateDTO.getName() + "' already exists");
            }
            existingCompanyGroup.setName(updateDTO.getName());
        }

        if (updateDTO.getDescription() != null) {
            existingCompanyGroup.setDescription(updateDTO.getDescription());
        }

        if (updateDTO.getIsActive() != null) {
            existingCompanyGroup.setIsActive(updateDTO.getIsActive());
        }

        CompanyGroup updatedCompanyGroup = companyGroupRepository.save(existingCompanyGroup);
        log.info("Company group updated successfully with id: {}", updatedCompanyGroup.getId());
        return toDTO(updatedCompanyGroup);
    }

    public void delete(UUID id) {
        log.info("Deleting company group with id: {}", id);
        if (!companyGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("Company group not found with id: " + id);
        }
        companyGroupRepository.deleteById(id);
        log.info("Company group deleted successfully");
    }

    private CompanyGroupDTO toDTO(CompanyGroup companyGroup) {
        return CompanyGroupDTO.builder()
                .id(companyGroup.getId())
                .name(companyGroup.getName())
                .description(companyGroup.getDescription())
                .isActive(companyGroup.getIsActive())
                .createdAt(companyGroup.getCreatedAt())
                .updatedAt(companyGroup.getUpdatedAt())
                .build();
    }
}
