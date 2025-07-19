package com.ruby.rubia_server.messaging.service;

import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.entity.*;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.UserRole;
import com.ruby.rubia_server.core.repository.*;
import com.ruby.rubia_server.core.entity.Department;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.service.CustomerService;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.service.MessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Transactional
class WhatsAppMessagingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private CustomerService customerService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CompanyGroupRepository companyGroupRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    // Mock external messaging dependencies
    @MockBean
    private MessagingService messagingService;

    private Company testCompany;
    private User testUser;
    private CompanyGroup testCompanyGroup;
    private Department testDepartment;

    @BeforeEach
    void setUp() {
        // Create test company group
        testCompanyGroup = CompanyGroup.builder()
            .name("Test Group")
            .build();
        testCompanyGroup = companyGroupRepository.save(testCompanyGroup);

        // Create test company
        testCompany = Company.builder()
            .name("Test Blood Bank")
            .slug("test-blood-bank")
            .contactEmail("test@bloodbank.com")
            .companyGroup(testCompanyGroup)
            .maxWhatsappNumbers(5)
            .build();
        testCompany = companyRepository.save(testCompany);

        // Create test department
        testDepartment = Department.builder()
            .name("Test Department")
            .company(testCompany)
            .build();
        testDepartment = departmentRepository.save(testDepartment);

        // Create test user with WhatsApp
        testUser = User.builder()
            .company(testCompany)
            .department(testDepartment)
            .name("Test Agent")
            .email("agent@bloodbank.com")
            .passwordHash("encoded_password")
            .role(UserRole.AGENT)
            .whatsappNumber("+5511999999999")
            .isWhatsappActive(true)
            .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void shouldCreateCustomerAndConversationFromWhatsAppMessage() {
        // Given - New WhatsApp message from unknown customer
        String customerPhone = "+5511888888888";
        String messageContent = "Olá, gostaria de doar sangue";
        
        // When - Process incoming WhatsApp message (simulate the flow)
        // 1. Customer doesn't exist, so create new one
        CustomerDTO newCustomer = customerService.create(
            com.ruby.rubia_server.core.dto.CreateCustomerDTO.builder()
                .phone(customerPhone)
                .name("WhatsApp " + customerPhone.substring(customerPhone.length() - 4))
                .build(),
            testCompany.getId()
        );

        // 2. Create conversation for the customer
        ConversationDTO conversation = conversationService.create(
            com.ruby.rubia_server.core.dto.CreateConversationDTO.builder()
                .customerId(newCustomer.getId())
                .channel(Channel.WHATSAPP)
                .status(ConversationStatus.ENTRADA)
                .priority(1)
                .build(),
            testCompany.getId()
        );

        // Then - Verify customer and conversation were created
        assertThat(newCustomer).isNotNull();
        assertThat(newCustomer.getPhone()).isEqualTo(customerPhone);
        assertThat(newCustomer.getName()).startsWith("WhatsApp");

        assertThat(conversation).isNotNull();
        assertThat(conversation.getChannel()).isEqualTo(Channel.WHATSAPP);
        assertThat(conversation.getStatus()).isEqualTo(ConversationStatus.ENTRADA);
        assertThat(conversation.getCustomerId()).isEqualTo(newCustomer.getId());

        // Verify in database
        List<Customer> customers = customerRepository.findAll();
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getPhone()).isEqualTo(customerPhone);

        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
        assertThat(conversations.get(0).getChannel()).isEqualTo(Channel.WHATSAPP);
    }

    @Test
    void shouldReuseExistingActiveConversation() {
        // Given - Existing customer with active conversation
        CustomerDTO existingCustomer = customerService.create(
            com.ruby.rubia_server.core.dto.CreateCustomerDTO.builder()
                .phone("+5511777777777")
                .name("Existing Customer")
                .build(),
            testCompany.getId()
        );

        ConversationDTO existingConversation = conversationService.create(
            com.ruby.rubia_server.core.dto.CreateConversationDTO.builder()
                .customerId(existingCustomer.getId())
                .channel(Channel.WHATSAPP)
                .status(ConversationStatus.ESPERANDO)
                .priority(1)
                .build(),
            testCompany.getId()
        );

        // When - Customer sends another message (simulate finding existing conversation)
        List<ConversationDTO> customerConversations = conversationService
            .findByCustomerAndCompany(existingCustomer.getId(), testCompany.getId());

        // Filter for active WhatsApp conversations
        ConversationDTO activeConversation = customerConversations.stream()
            .filter(conv -> conv.getChannel() == Channel.WHATSAPP)
            .filter(conv -> conv.getStatus() == ConversationStatus.ENTRADA ||
                           conv.getStatus() == ConversationStatus.ESPERANDO)
            .findFirst()
            .orElse(null);

        // Then - Should reuse existing conversation
        assertThat(activeConversation).isNotNull();
        assertThat(activeConversation.getId()).isEqualTo(existingConversation.getId());
        assertThat(activeConversation.getStatus()).isEqualTo(ConversationStatus.ESPERANDO);

        // Should not create new conversation
        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(1);
    }

    @Test
    void shouldCreateNewConversationForFinalizedCustomer() {
        // Given - Customer with finalized conversation
        CustomerDTO customer = customerService.create(
            com.ruby.rubia_server.core.dto.CreateCustomerDTO.builder()
                .phone("+5511666666666")
                .name("Returning Customer")
                .build(),
            testCompany.getId()
        );

        ConversationDTO finalizedConversation = conversationService.create(
            com.ruby.rubia_server.core.dto.CreateConversationDTO.builder()
                .customerId(customer.getId())
                .channel(Channel.WHATSAPP)
                .status(ConversationStatus.FINALIZADOS)
                .priority(1)
                .build(),
            testCompany.getId()
        );

        // When - Customer sends new message after finalized conversation
        List<ConversationDTO> customerConversations = conversationService
            .findByCustomerAndCompany(customer.getId(), testCompany.getId());

        // Look for active conversations (should find none)
        boolean hasActiveConversation = customerConversations.stream()
            .anyMatch(conv -> conv.getChannel() == Channel.WHATSAPP &&
                            (conv.getStatus() == ConversationStatus.ENTRADA ||
                             conv.getStatus() == ConversationStatus.ESPERANDO));

        // Since no active conversation, create new one
        ConversationDTO newConversation = null;
        if (!hasActiveConversation) {
            newConversation = conversationService.create(
                com.ruby.rubia_server.core.dto.CreateConversationDTO.builder()
                    .customerId(customer.getId())
                    .channel(Channel.WHATSAPP)
                    .status(ConversationStatus.ENTRADA)
                    .priority(1)
                    .build(),
                testCompany.getId()
            );
        }

        // Then - Should create new conversation
        assertThat(hasActiveConversation).isFalse();
        assertThat(newConversation).isNotNull();
        assertThat(newConversation.getId()).isNotEqualTo(finalizedConversation.getId());
        assertThat(newConversation.getStatus()).isEqualTo(ConversationStatus.ENTRADA);

        // Should have 2 conversations now
        List<Conversation> conversations = conversationRepository.findAll();
        assertThat(conversations).hasSize(2);
    }

    @Test
    void shouldFindCompanyByUserWhatsAppNumber() {
        // Given - WhatsApp number associated with a user
        String whatsappNumber = "+5511999999999"; // testUser's number

        // When - Find company by WhatsApp number (simulate the lookup)
        User userWithWhatsApp = userRepository.findAll().stream()
            .filter(u -> u.getWhatsappNumber() != null)
            .filter(u -> u.getWhatsappNumber().equals(whatsappNumber))
            .filter(User::getIsWhatsappActive)
            .findFirst()
            .orElse(null);

        // Then - Should find the company
        assertThat(userWithWhatsApp).isNotNull();
        assertThat(userWithWhatsApp.getCompany().getId()).isEqualTo(testCompany.getId());
        assertThat(userWithWhatsApp.getWhatsappNumber()).isEqualTo(whatsappNumber);
    }

    @Test
    void shouldNotFindCompanyForUnknownWhatsAppNumber() {
        // Given - Unknown WhatsApp number
        String unknownNumber = "+5511555555555";

        // When - Try to find company
        User userWithWhatsApp = userRepository.findAll().stream()
            .filter(u -> u.getWhatsappNumber() != null)
            .filter(u -> u.getWhatsappNumber().equals(unknownNumber))
            .filter(User::getIsWhatsappActive)
            .findFirst()
            .orElse(null);

        // Then - Should not find any company
        assertThat(userWithWhatsApp).isNull();
    }
}