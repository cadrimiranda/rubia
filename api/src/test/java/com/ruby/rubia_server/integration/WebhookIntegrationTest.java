package com.ruby.rubia_server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.config.AbstractIntegrationTest;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.repository.MessageRepository;
import com.ruby.rubia_server.core.repository.CustomerRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.WhatsAppInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "messaging.provider=zapi"
})
@Transactional
class WebhookIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private WhatsAppInstanceRepository whatsAppInstanceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Company testCompany;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    private void setupTestData() {
        // Create test company
        testCompany = Company.builder()
                .name("Test Company")
                .build();
        testCompany = companyRepository.save(testCompany);

        // Create WhatsApp instance
        WhatsAppInstance instance = WhatsAppInstance.builder()
                .phoneNumber("5511999999999")
                .company(testCompany)
                .build();
        whatsAppInstanceRepository.save(instance);

        // Create test customer
        testCustomer = Customer.builder()
                .name("Test Customer")
                .phone("5511888888888")
                .company(testCompany)
                .build();
        testCustomer = customerRepository.save(testCustomer);
    }

    @Test
    void shouldProcessIncomingTextMessageFromCustomer() throws Exception {
        // Arrange - Simulate incoming message from customer
        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG123",
            "5511888888888", // from customer
            "5511999999999", // to our number
            false, // fromMe = false
            "Olá, preciso de ajuda!"
        );

        // Act - Send webhook
        mockMvc.perform(post("/api/messaging/webhook/zapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
                .andDo(result -> {
                    System.out.println("Response Status: " + result.getResponse().getStatus());
                    System.out.println("Response Body: " + result.getResponse().getContentAsString());
                })
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        // Assert - Check message was saved correctly
        Optional<Message> savedMessage = messageRepository.findByExternalMessageId("MSG123");
        assertThat(savedMessage).isPresent();
        
        Message message = savedMessage.get();
        assertThat(message.getContent()).isEqualTo("Olá, preciso de ajuda!");
        assertThat(message.getSenderType()).isEqualTo(SenderType.CUSTOMER);
        assertThat(message.getExternalMessageId()).isEqualTo("MSG123");
        assertThat(message.getConversation()).isNotNull();
    }

    @Test
    void shouldProcessOutgoingTextMessageFromAgent() throws Exception {
        // Arrange - Simulate outgoing message from agent (fromMe = true)
        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG456",
            "5511999999999", // from our number
            "5511888888888", // to customer
            true, // fromMe = true
            "Olá! Como posso ajudar?"
        );

        // Act - Send webhook
        mockMvc.perform(post("/api/messaging/webhook/zapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        // Assert - Check message was saved correctly as AGENT
        Optional<Message> savedMessage = messageRepository.findByExternalMessageId("MSG456");
        assertThat(savedMessage).isPresent();
        
        Message message = savedMessage.get();
        assertThat(message.getContent()).isEqualTo("Olá! Como posso ajudar?");
        assertThat(message.getSenderType()).isEqualTo(SenderType.AGENT);
        assertThat(message.getExternalMessageId()).isEqualTo("MSG456");
        assertThat(message.getConversation()).isNotNull();
    }

    @Test
    void shouldCreateConversationIfNotExists() throws Exception {
        // Arrange - Create new customer without conversation
        Customer newCustomer = Customer.builder()
                .name("New Customer")
                .phone("5511777777777")
                .company(testCompany)
                .build();
        customerRepository.save(newCustomer);

        Map<String, Object> webhookPayload = createWebhookPayload(
            "MSG789",
            "5511777777777", // from new customer
            "5511999999999", // to our number
            false, // fromMe = false
            "Primeira mensagem"
        );

        // Act
        mockMvc.perform(post("/api/messaging/webhook/zapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(webhookPayload)))
                .andExpect(status().isOk());

        // Assert - Message was created
        Optional<Message> savedMessage = messageRepository.findByExternalMessageId("MSG789");
        assertThat(savedMessage).isPresent();
        assertThat(savedMessage.get().getConversation()).isNotNull();
    }

    @Test
    void shouldHandleBothIncomingAndOutgoingMessagesInSameConversation() throws Exception {
        // Arrange & Act - Send incoming message
        Map<String, Object> incomingPayload = createWebhookPayload(
            "MSG_IN_001",
            "5511888888888",
            "5511999999999",
            false,
            "Cliente pergunta"
        );

        mockMvc.perform(post("/api/messaging/webhook/zapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incomingPayload)))
                .andExpect(status().isOk());

        // Act - Send outgoing message
        Map<String, Object> outgoingPayload = createWebhookPayload(
            "MSG_OUT_001",
            "5511999999999",
            "5511888888888",
            true,
            "Resposta do agente"
        );

        mockMvc.perform(post("/api/messaging/webhook/zapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(outgoingPayload)))
                .andExpect(status().isOk());

        // Assert - Both messages saved in same conversation
        Optional<Message> incomingMessage = messageRepository.findByExternalMessageId("MSG_IN_001");
        Optional<Message> outgoingMessage = messageRepository.findByExternalMessageId("MSG_OUT_001");

        assertThat(incomingMessage).isPresent();
        assertThat(outgoingMessage).isPresent();

        assertThat(incomingMessage.get().getSenderType()).isEqualTo(SenderType.CUSTOMER);
        assertThat(outgoingMessage.get().getSenderType()).isEqualTo(SenderType.AGENT);

        // Same conversation
        assertThat(incomingMessage.get().getConversation().getId())
                .isEqualTo(outgoingMessage.get().getConversation().getId());
    }

    @Test
    void shouldIgnoreMessageStatusCallbacks() throws Exception {
        // Arrange - Create MessageStatusCallback payload
        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("type", "MessageStatusCallback");
        statusPayload.put("messageId", "MSG999");
        statusPayload.put("status", "delivered");

        long messageCountBefore = messageRepository.count();

        // Act
        mockMvc.perform(post("/api/messaging/webhook/zapi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusPayload)))
                .andExpect(status().isOk())
                .andExpect(content().string("MessageStatusCallback ignored"));

        // Assert - No message was created
        long messageCountAfter = messageRepository.count();
        assertThat(messageCountAfter).isEqualTo(messageCountBefore);
    }

    private Map<String, Object> createWebhookPayload(String messageId, String phone, String connectedPhone, 
                                                    boolean fromMe, String messageText) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", "TEST_INSTANCE");
        payload.put("messageId", messageId);
        payload.put("phone", phone);
        payload.put("connectedPhone", connectedPhone);
        payload.put("fromMe", fromMe);
        payload.put("momment", System.currentTimeMillis() / 1000);
        payload.put("type", "ReceivedCallback");
        payload.put("senderName", fromMe ? "Eu" : "Customer Name");
        payload.put("isGroup", false);
        payload.put("messageExisting", true);

        // Add text message
        Map<String, Object> textData = new HashMap<>();
        textData.put("message", messageText);
        payload.put("text", textData);

        return payload;
    }
}