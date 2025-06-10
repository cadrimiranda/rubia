package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.dto.CreateUserDTO;
import com.ruby.rubia_server.core.dto.UpdateUserDTO;
import com.ruby.rubia_server.core.dto.UserDTO;
import com.ruby.rubia_server.core.dto.UserLoginDTO;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.DepartmentRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public UserDTO create(CreateUserDTO createDTO) {
        log.info("Creating user with email: {}", createDTO.getEmail());
        
        if (userRepository.existsByEmailAndCompanyId(createDTO.getEmail(), createDTO.getCompanyId())) {
            throw new IllegalArgumentException("Usuário com email '" + createDTO.getEmail() + "' já existe nesta empresa");
        }
        
        Company company = companyRepository.findById(createDTO.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
        
        Department department = null;
        if (createDTO.getDepartmentId() != null) {
            department = departmentRepository.findById(createDTO.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
        }
        
        User user = User.builder()
                .name(createDTO.getName())
                .email(createDTO.getEmail())
                .passwordHash(passwordEncoder.encode(createDTO.getPassword()))
                .company(company)
                .department(department)
                .role(createDTO.getRole())
                .avatarUrl(createDTO.getAvatarUrl())
                .birthDate(createDTO.getBirthDate())
                .weight(createDTO.getWeight())
                .height(createDTO.getHeight())
                .address(createDTO.getAddress())
                .isOnline(false)
                .build();
        
        User saved = userRepository.save(user);
        log.info("User created successfully with id: {}", saved.getId());
        
        return toDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public UserDTO findById(UUID id) {
        log.debug("Finding user by id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        return toDTO(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO findByEmailAndCompany(String email, UUID companyId) {
        log.debug("Finding user by email: {} for company: {}", email, companyId);
        
        User user = userRepository.findByEmailAndCompanyId(email, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado nesta empresa"));
        
        return toDTO(user);
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> findAllByCompany(UUID companyId) {
        log.debug("Finding all users for company: {}", companyId);
        
        return userRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> findByDepartmentAndCompany(UUID departmentId, UUID companyId) {
        log.debug("Finding users by department: {} for company: {}", departmentId, companyId);
        
        return userRepository.findByDepartmentIdAndCompanyId(departmentId, companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> findAvailableAgentsByCompany(UUID companyId) {
        log.debug("Finding available agents for company: {}", companyId);
        
        return userRepository.findAvailableAgentsByCompany(companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    @Transactional(readOnly = true)
    public List<UserDTO> findAvailableAgentsByDepartmentAndCompany(UUID departmentId, UUID companyId) {
        log.debug("Finding available agents by department: {} for company: {}", departmentId, companyId);
        
        return userRepository.findAvailableAgentsByDepartmentAndCompany(departmentId, companyId)
                .stream()
                .map(this::toDTO)
                .toList();
    }
    
    public UserDTO update(UUID id, UpdateUserDTO updateDTO) {
        log.info("Updating user with id: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        if (updateDTO.getName() != null) {
            user.setName(updateDTO.getName());
        }
        
        if (updateDTO.getEmail() != null) {
            if (!updateDTO.getEmail().equals(user.getEmail()) && 
                userRepository.existsByEmailAndCompanyId(updateDTO.getEmail(), user.getCompany().getId())) {
                throw new IllegalArgumentException("Usuário com email '" + updateDTO.getEmail() + "' já existe nesta empresa");
            }
            user.setEmail(updateDTO.getEmail());
        }
        
        if (updateDTO.getPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(updateDTO.getPassword()));
        }
        
        if (updateDTO.getDepartmentId() != null) {
            Department department = departmentRepository.findById(updateDTO.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
            user.setDepartment(department);
        }
        
        if (updateDTO.getRole() != null) {
            user.setRole(updateDTO.getRole());
        }
        
        if (updateDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(updateDTO.getAvatarUrl());
        }
        
        if (updateDTO.getBirthDate() != null) {
            user.setBirthDate(updateDTO.getBirthDate());
        }
        
        if (updateDTO.getWeight() != null) {
            user.setWeight(updateDTO.getWeight());
        }
        
        if (updateDTO.getHeight() != null) {
            user.setHeight(updateDTO.getHeight());
        }
        
        if (updateDTO.getAddress() != null) {
            user.setAddress(updateDTO.getAddress());
        }
        
        User updated = userRepository.save(user);
        log.info("User updated successfully");
        
        return toDTO(updated);
    }
    
    public UserDTO updateOnlineStatus(UUID id, boolean isOnline) {
        log.info("Updating online status for user: {} to {}", id, isOnline);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        user.setIsOnline(isOnline);
        if (!isOnline) {
            user.setLastSeen(LocalDateTime.now());
        }
        
        User updated = userRepository.save(user);
        log.info("User online status updated successfully");
        
        return toDTO(updated);
    }
    
    public UserDTO assignToDepartment(UUID userId, UUID departmentId) {
        log.info("Assigning user {} to department {}", userId, departmentId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Departamento não encontrado"));
        
        user.setDepartment(department);
        User updated = userRepository.save(user);
        
        log.info("User assigned to department successfully");
        return toDTO(updated);
    }
    
    @Transactional(readOnly = true)
    public boolean validateLoginByCompany(UserLoginDTO loginDTO, UUID companyId) {
        log.debug("Validating login for email: {} in company: {}", loginDTO.getEmail(), companyId);
        
        User user = userRepository.findByEmailAndCompanyId(loginDTO.getEmail(), companyId)
                .orElse(null);
        
        if (user == null) {
            return false;
        }
        
        return passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash());
    }
    
    public void delete(UUID id) {
        log.info("Deleting user with id: {}", id);
        
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Usuário não encontrado");
        }
        
        userRepository.deleteById(id);
        log.info("User deleted successfully");
    }
    
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .companyId(user.getCompany() != null ? user.getCompany().getId() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .isOnline(user.getIsOnline())
                .lastSeen(user.getLastSeen())
                .birthDate(user.getBirthDate())
                .weight(user.getWeight())
                .height(user.getHeight())
                .address(user.getAddress())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}