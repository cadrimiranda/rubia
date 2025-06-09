package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.enums.ConversationChannel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ConversationRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    private Conversation conversation1;
    private Conversation conversation2;
    private Conversation conversation3;
    private Conversation conversation4;
    private Company company1;
    private Company company2;
    private Customer customer1;
    private Customer customer2;
    private User user1;
    private Department department1;
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
        
        entityManager.persistAndFlush(department1);
        
        customer1 = Customer.builder()
                .phone("+5511999999001")
                .name("João Silva")
                .whatsappId("wa_001")
                .company(company1)
                .isBlocked(false)
                .build();
        
        customer2 = Customer.builder()
                .phone("+5511999999002")
                .name("Maria Santos")
                .whatsappId("wa_002")
                .company(company2)
                .isBlocked(false)
                .build();
        
        entityManager.persistAndFlush(customer1);
        entityManager.persistAndFlush(customer2);
        
        user1 = User.builder()
                .name("Agent Silva")
                .email("agent@company1.com")
                .passwordHash("hash123")
                .role(UserRole.AGENT)
                .company(company1)
                .department(department1)
                .isOnline(true)
                .build();
        
        entityManager.persistAndFlush(user1);
        
        conversation1 = Conversation.builder()
                .customer(customer1)
                .assignedUser(user1)
                .department(department1)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .isPinned(false)
                .company(company1)
                .build();
        
        conversation2 = Conversation.builder()
                .customer(customer1)
                .assignedUser(null)
                .department(department1)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(2)
                .isPinned(true)
                .company(company1)
                .build();
        
        conversation3 = Conversation.builder()
                .customer(customer1)
                .assignedUser(user1)
                .department(department1)
                .status(ConversationStatus.ESPERANDO)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .isPinned(false)
                .company(company1)
                .build();
        
        conversation4 = Conversation.builder()
                .customer(customer2)
                .assignedUser(null)
                .department(null)
                .status(ConversationStatus.ENTRADA)
                .channel(ConversationChannel.WHATSAPP)
                .priority(1)
                .isPinned(false)
                .company(company2)
                .build();
        
        entityManager.persistAndFlush(conversation1);
        entityManager.persistAndFlush(conversation2);
        entityManager.persistAndFlush(conversation3);
        entityManager.persistAndFlush(conversation4);
    }
    
    @Test
    void findByStatusAndCompanyId_ShouldReturnConversationsWithStatus() {
        List<Conversation> result = conversationRepository.findByStatusAndCompanyId(ConversationStatus.ENTRADA, company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Conversation::getStatus)
                .containsOnly(ConversationStatus.ENTRADA);
    }
    
    @Test
    void findByStatusAndCompanyId_ShouldReturnEmpty_WhenNoneFound() {
        List<Conversation> result = conversationRepository.findByStatusAndCompanyId(ConversationStatus.FINALIZADOS, company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByStatusAndCompanyId_ShouldNotReturnConversationsFromOtherCompanies() {
        List<Conversation> result = conversationRepository.findByStatusAndCompanyId(ConversationStatus.ENTRADA, company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(conv -> conv.getCompany().getId())
                .containsOnly(company1Id);
    }
    
    @Test
    void findByStatusAndCompanyId_WithPageable_ShouldReturnPagedResults() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Conversation> result = conversationRepository.findByStatusAndCompanyId(ConversationStatus.ENTRADA, company1Id, pageable);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }
    
    @Test
    void findByCustomerIdAndCompanyId_ShouldReturnCustomerConversations() {
        List<Conversation> result = conversationRepository.findByCustomerIdAndCompanyId(customer1.getId(), company1Id);
        
        assertThat(result).hasSize(3);
        assertThat(result).extracting(conv -> conv.getCustomer().getId())
                .containsOnly(customer1.getId());
    }
    
    @Test
    void findByCustomerIdAndCompanyId_ShouldReturnEmpty_WhenCustomerFromDifferentCompany() {
        List<Conversation> result = conversationRepository.findByCustomerIdAndCompanyId(customer2.getId(), company1Id);
        
        assertThat(result).isEmpty();
    }
    
    @Test
    void findByAssignedUserIdAndCompanyId_ShouldReturnUserConversations() {
        List<Conversation> result = conversationRepository.findByAssignedUserIdAndCompanyId(user1.getId(), company1Id);
        
        assertThat(result).hasSize(2);
        assertThat(result).extracting(conv -> conv.getAssignedUser().getId())
                .containsOnly(user1.getId());
    }
    
    @Test
    void findByStatusAndCompanyOrderedByPriorityAndUpdatedAt_ShouldReturnOrderedResults() {
        List<Conversation> result = conversationRepository.findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(ConversationStatus.ENTRADA, company1Id);
        
        assertThat(result).hasSize(2);
        // First should be pinned conversation with higher priority
        assertThat(result.get(0).getIsPinned()).isTrue();
        assertThat(result.get(0).getPriority()).isEqualTo(2);
    }
    
    @Test
    void findByStatusAndCompanyOrderedByPriorityAndUpdatedAt_WithPageable_ShouldReturnPagedOrderedResults() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Conversation> result = conversationRepository.findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(ConversationStatus.ENTRADA, company1Id, pageable);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIsPinned()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(2);
    }
    
    @Test
    void findUnassignedEntranceConversationsByCompany_ShouldReturnUnassignedConversations() {
        List<Conversation> result = conversationRepository.findUnassignedEntranceConversationsByCompany(company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAssignedUser()).isNull();
        assertThat(result.get(0).getStatus()).isEqualTo(ConversationStatus.ENTRADA);
    }
    
    @Test
    void findUnassignedEntranceConversationsByCompany_ShouldNotReturnAssignedConversations() {
        List<Conversation> result = conversationRepository.findUnassignedEntranceConversationsByCompany(company1Id);
        
        assertThat(result).hasSize(1);
        assertThat(result).noneMatch(conv -> conv.getAssignedUser() != null);
    }
    
    @Test
    void countByStatusAndCompany_ShouldReturnCorrectCount() {
        long result = conversationRepository.countByStatusAndCompany(ConversationStatus.ENTRADA, company1Id);
        
        assertThat(result).isEqualTo(2);
    }
    
    @Test
    void countByStatusAndCompany_ShouldReturnZero_WhenNoConversations() {
        long result = conversationRepository.countByStatusAndCompany(ConversationStatus.FINALIZADOS, company1Id);
        
        assertThat(result).isEqualTo(0);
    }
    
    @Test
    void countByStatusAndCompany_ShouldNotCountOtherCompanyConversations() {
        long result = conversationRepository.countByStatusAndCompany(ConversationStatus.ENTRADA, company2Id);
        
        assertThat(result).isEqualTo(1); // Only conversation4 from company2
    }
}