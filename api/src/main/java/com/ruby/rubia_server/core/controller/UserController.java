package com.ruby.rubia_server.core.controller;

import com.ruby.rubia_server.core.dto.CreateUserDTO;
import com.ruby.rubia_server.core.dto.UpdateUserDTO;
import com.ruby.rubia_server.core.dto.UserDTO;
import com.ruby.rubia_server.core.dto.UserLoginDTO;
import com.ruby.rubia_server.core.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody CreateUserDTO createDTO) {
        log.info("Creating user: {}", createDTO.getEmail());
        
        try {
            UserDTO created = userService.create(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.warn("Error creating user: {}", e.getMessage());
            throw e;
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> findById(@PathVariable UUID id) {
        log.debug("Finding user by id: {}", id);
        
        try {
            UserDTO user = userService.findById(id);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("User not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> findByEmail(@PathVariable String email) {
        log.debug("Finding user by email: {}", email);
        
        try {
            UserDTO user = userService.findByEmail(email);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            log.warn("User not found: {}", email);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<UserDTO>> findAll(
            @RequestParam(required = false) UUID departmentId) {
        log.debug("Finding users, departmentId: {}", departmentId);
        
        List<UserDTO> users = departmentId != null 
            ? userService.findByDepartment(departmentId)
            : userService.findAll();
            
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/available-agents")
    public ResponseEntity<List<UserDTO>> findAvailableAgents(
            @RequestParam(required = false) UUID departmentId) {
        log.debug("Finding available agents, departmentId: {}", departmentId);
        
        List<UserDTO> agents = departmentId != null
            ? userService.findAvailableAgentsByDepartment(departmentId)
            : userService.findAvailableAgents();
            
        return ResponseEntity.ok(agents);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable UUID id, 
                                         @Valid @RequestBody UpdateUserDTO updateDTO) {
        log.info("Updating user: {}", id);
        
        try {
            UserDTO updated = userService.update(id, updateDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating user: {}", e.getMessage());
            if (e.getMessage().contains("não encontrado")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
    
    @PutMapping("/{id}/online-status")
    public ResponseEntity<UserDTO> updateOnlineStatus(@PathVariable UUID id, 
                                                      @RequestParam boolean isOnline) {
        log.info("Updating online status for user: {} to {}", id, isOnline);
        
        try {
            UserDTO updated = userService.updateOnlineStatus(id, isOnline);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error updating online status: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PutMapping("/{userId}/assign-department/{departmentId}")
    public ResponseEntity<UserDTO> assignToDepartment(@PathVariable UUID userId,
                                                      @PathVariable UUID departmentId) {
        log.info("Assigning user {} to department {}", userId, departmentId);
        
        try {
            UserDTO updated = userService.assignToDepartment(userId, departmentId);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.warn("Error assigning user to department: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Boolean> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        log.info("Login attempt for email: {}", loginDTO.getEmail());
        
        boolean isValid = userService.validateLogin(loginDTO);
        
        if (isValid) {
            log.info("Login successful for email: {}", loginDTO.getEmail());
            return ResponseEntity.ok(true);
        } else {
            log.warn("Login failed for email: {}", loginDTO.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting user: {}", id);
        
        try {
            userService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Error deleting user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}