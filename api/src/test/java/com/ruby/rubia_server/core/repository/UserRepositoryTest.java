package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    private User user1;
    private User user2;
    private User user3;
    private User user4;
    private Company company1;
    private Company company2;
    private Department department1;
    private Department department2;
    private UUID company1Id;
    private UUID company2Id;
    
    @BeforeEach
    void setUp() {
        company1Id = UUID.randomUUID();
        company2Id = UUID.randomUUID();
        
        company1 = Company.builder()
                .id(company1Id)
                .name("Company 1")
                .slug("company1")
                .isActive(true)
                .build();
        
        company2 = Company.builder()
                .id(company2Id)
                .name("Company 2")
                .slug("company2")
                .isActive(true)
                .build();
        
        entityManager.persistAndFlush(company1);
        entityManager.persistAndFlush(company2);
        
        department1 = Department.builder()
                .name("Suporte")
                .description("Departamento de suporte")
                .company(company1)
                .autoAssign(true)
                .build();
        
        department2 = Department.builder()
                .name("Vendas")
                .description("Departamento de vendas")
                .company(company1)
                .autoAssign(false)
                .build();
        
        entityManager.persistAndFlush(department1);
        entityManager.persistAndFlush(department2);
        
        user1 = User.builder()
                .name("João Silva")
                .email("joao@company1.com")
                .passwordHash("hash123")
                .role(UserRole.AGENT)
                .company(company1)
                .department(department1)
                .isOnline(true)
                .isWhatsappActive(true)
                .whatsappNumber("+5511999999001")
                .avatarUrl("avatar1.jpg")
                .build();
        
        user2 = User.builder()
                .name("Maria Santos")
                .email("maria@company1.com")
                .passwordHash("hash456")
                .role(UserRole.SUPERVISOR)
                .company(company1)
                .department(department2)
                .isOnline(false)
                .isWhatsappActive(false)
                .avatarUrl("avatar2.jpg")
                .build();
        
        user3 = User.builder()
                .name("Carlos Lima")
                .email("carlos@company1.com")
                .passwordHash("hash789")
                .role(UserRole.AGENT)
                .company(company1)
                .department(department1)
                .isOnline(true)
                .isWhatsappActive(true)
                .whatsappNumber("+5511999999003")
                .build();
        
        user4 = User.builder()
                .name("Ana Costa")
                .email("ana@company2.com")
                .passwordHash("hash999")
                .role(UserRole.AGENT)
                .company(company2)
                .department(null)
                .isOnline(true)
                .isWhatsappActive(false)
                .build();
        
        entityManager.persistAndFlush(user1);
        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);
        entityManager.persistAndFlush(user4);
    }
    
    @Test
    void findByEmail_ShouldReturnUser_WhenExists() {
        Optional<User> result = userRepository.findByEmail("joao@company1.com");
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("João Silva");
        assertThat(result.get().getEmail()).isEqualTo("joao@company1.com");
    }
    
    @Test
    void findByEmail_ShouldReturnEmpty_WhenNotExists() {
        Optional<User> result = userRepository.findByEmail("inexistente@test.com");
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByEmailAndCompanyId_ShouldReturnUser_WhenExistsInCompany() {
        Optional<User> result = userRepository.findByEmailAndCompanyId("joao@company1.com", company1Id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("João Silva");
        assertThat(result.get().getCompany().getId()).isEqualTo(company1Id);
    }
    
    @Test
    void findByEmailAndCompanyId_ShouldReturnEmpty_WhenExistsInDifferentCompany() {
        Optional<User> result = userRepository.findByEmailAndCompanyId("ana@company2.com", company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByWhatsappNumberAndCompanyId_ShouldReturnUser_WhenExists() {
        Optional<User> result = userRepository.findByWhatsappNumberAndCompanyId("+5511999999001", company1Id);
        
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("João Silva");
        assertThat(result.get().getWhatsappNumber()).isEqualTo("+5511999999001");
    }
    
    @Test
    void findByWhatsappNumberAndCompanyId_ShouldReturnEmpty_WhenNotExists() {
        Optional<User> result = userRepository.findByWhatsappNumberAndCompanyId("+5511999999999", company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByCompanyId_ShouldReturnAllUsersFromCompany() {
        List<User> result = userRepository.findByCompanyId(company1Id);
        
        assertThat(result).hasSize(3);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Maria Santos", "Carlos Lima");
    }
    
    @Test
    void findByCompanyId_ShouldNotReturnUsersFromOtherCompanies() {
        List<User> result = userRepository.findByCompanyId(company1Id);
        
        assertThat(result).hasSize(3);
        assertThat(result).extracting(user -> user.getCompany().getId())
                .containsOnly(company1Id);
    }
    
    @Test
    void findByDepartmentIdAndCompanyId_ShouldReturnUsersFromDepartment() {
        List<User> result = userRepository.findByDepartmentIdAndCompanyId(department1.getId(), company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Carlos Lima");
    }
    
    @Test
    void findByIsOnlineTrueAndCompanyId_ShouldReturnOnlineUsers() {
        List<User> result = userRepository.findByIsOnlineTrueAndCompanyId(company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Carlos Lima");
        assertThat(result).allMatch(User::getIsOnline);
    }
    
    @Test
    void findByRoleAndCompanyId_ShouldReturnUsersWithRole() {
        List<User> result = userRepository.findByRoleAndCompanyId(UserRole.AGENT, company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Carlos Lima");
        assertThat(result).allMatch(user -> user.getRole() == UserRole.AGENT);
    }
    
    @Test
    void findByIsWhatsappActiveTrueAndCompanyId_ShouldReturnWhatsappActiveUsers() {
        List<User> result = userRepository.findByIsWhatsappActiveTrueAndCompanyId(company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Carlos Lima");
        assertThat(result).allMatch(User::getIsWhatsappActive);
    }
    
    @Test
    void findAvailableAgentsByDepartmentAndCompany_ShouldReturnOnlineAgentsFromDepartment() {
        List<User> result = userRepository.findAvailableAgentsByDepartmentAndCompany(department1.getId(), company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Carlos Lima");
        assertThat(result).allMatch(User::getIsOnline);
        assertThat(result).allMatch(user -> user.getDepartment().getId().equals(department1.getId()));
    }
    
    @Test
    void findAvailableAgentsByCompany_ShouldReturnOnlineAgents() {
        List<User> result = userRepository.findAvailableAgentsByCompany(company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getName)
                .containsExactlyInAnyOrder("João Silva", "Carlos Lima");
        assertThat(result).allMatch(User::getIsOnline);
        assertThat(result).allMatch(user -> user.getRole() == UserRole.AGENT);
    }
    
    @Test
    void findByCompanyIdOrderedByName_ShouldReturnOrderedUsers() {
        List<User> result = userRepository.findByCompanyIdOrderedByName(company1Id);
        
        assertThat(result).hasSize(3);
        // Should be ordered by name: Carlos Lima, João Silva, Maria Santos
        assertThat(result.get(0).getName()).isEqualTo("Carlos Lima");
        assertThat(result.get(1).getName()).isEqualTo("João Silva");
        assertThat(result.get(2).getName()).isEqualTo("Maria Santos");
    }
    
    @Test
    void existsByEmail_ShouldReturnTrue_WhenExists() {
        boolean result = userRepository.existsByEmail("joao@company1.com");
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByEmail_ShouldReturnFalse_WhenNotExists() {
        boolean result = userRepository.existsByEmail("inexistente@test.com");
        
        assertThat(result).isFalse();
    }
    
    @Test
    void existsByEmailAndCompanyId_ShouldReturnTrue_WhenExistsInCompany() {
        boolean result = userRepository.existsByEmailAndCompanyId("joao@company1.com", company1Id);
        
        assertThat(result).isTrue();
    }
    
    @Test
    void existsByEmailAndCompanyId_ShouldReturnFalse_WhenExistsInDifferentCompany() {
        boolean result = userRepository.existsByEmailAndCompanyId("ana@company2.com", company1Id);
        
        assertThat(result).isFalse();
    }
    
    @Test
    void countByDepartmentIdAndCompanyId_ShouldReturnCorrectCount() {
        long result = userRepository.countByDepartmentIdAndCompanyId(department1.getId(), company1Id);
        
        assertThat(result).isEqualTo(2);
    }
    
    @Test
    void countByDepartmentIdAndCompanyId_ShouldReturnZero_WhenNoneFound() {
        UUID nonExistentDeptId = UUID.randomUUID();
        long result = userRepository.countByDepartmentIdAndCompanyId(nonExistentDeptId, company1Id);
        
        assertThat(result).isEqualTo(0);
    }
    
    @Test
    void countByCompanyId_ShouldReturnCorrectCount() {
        long result = userRepository.countByCompanyId(company1Id);
        
        assertThat(result).isEqualTo(3);
    }
    
    @Test
    void countByCompanyId_ShouldNotCountUsersFromOtherCompanies() {
        long result = userRepository.countByCompanyId(company2Id);
        
        assertThat(result).isEqualTo(1); // Only user4 from company2
    }
}