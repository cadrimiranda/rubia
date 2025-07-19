package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.adapter.impl.TwilioAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.service.CustomerService;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.service.MessageService;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Service
public class MessagingService {
    
    private static final Logger logger = LoggerFactory.getLogger(MessagingService.class);
    
    private final List<MessagingAdapter> adapters;
    private MessagingAdapter currentAdapter;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    public MessagingService(List<MessagingAdapter> adapters) {
        this.adapters = adapters;
        this.currentAdapter = adapters.isEmpty() ? null : adapters.get(0);
        
        if (currentAdapter != null) {
            logger.info("Initialized with adapter: {}", currentAdapter.getProviderName());
        }
    }
    
    public MessageResult sendMessage(String to, String message) {
        return sendMessage(to, message, null, null);
    }
    
    public MessageResult sendMessage(String to, String message, UUID companyId, UUID userId) {
        if (currentAdapter == null) {
            logger.error("No messaging adapter configured");
            return MessageResult.error("No adapter available", "none");
        }
        
        String fromNumber = null;
        if (userId != null && companyId != null) {
            fromNumber = getUserWhatsappNumber(userId, companyId);
        }
        
        if (currentAdapter instanceof TwilioAdapter twilioAdapter) {
            return twilioAdapter.sendMessage(to, message, fromNumber);
        }
        
        return currentAdapter.sendMessage(to, message);
    }
    
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption) {
        return sendMediaMessage(to, mediaUrl, caption, null, null);
    }
    
    public MessageResult sendMediaMessage(String to, String mediaUrl, String caption, UUID companyId, UUID userId) {
        if (currentAdapter == null) {
            logger.error("No messaging adapter configured");
            return MessageResult.error("No adapter available", "none");
        }
        
        String fromNumber = null;
        if (userId != null && companyId != null) {
            fromNumber = getUserWhatsappNumber(userId, companyId);
        }
        
        if (currentAdapter instanceof TwilioAdapter twilioAdapter) {
            return twilioAdapter.sendMediaMessage(to, mediaUrl, caption, fromNumber);
        }
        
        return currentAdapter.sendMediaMessage(to, mediaUrl, caption);
    }
    
    public IncomingMessage parseIncomingMessage(Object webhookPayload) {
        if (currentAdapter == null) {
            throw new RuntimeException("No messaging adapter configured");
        }
        
        return currentAdapter.parseIncomingMessage(webhookPayload);
    }
    
    public boolean validateWebhook(Object payload, String signature) {
        if (currentAdapter == null) {
            return false;
        }
        
        return currentAdapter.validateWebhook(payload, signature);
    }
    
    public void switchAdapter(String providerName) {
        MessagingAdapter newAdapter = adapters.stream()
            .filter(adapter -> adapter.getProviderName().equals(providerName))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Adapter not found: " + providerName));
        
        this.currentAdapter = newAdapter;
        logger.info("Switched to adapter: {}", providerName);
    }
    
    public String getCurrentProvider() {
        return currentAdapter != null ? currentAdapter.getProviderName() : "none";
    }
    
    public List<String> getAvailableProviders() {
        return adapters.stream()
            .map(MessagingAdapter::getProviderName)
            .toList();
    }
    
    public User findUserByWhatsappNumber(String whatsappNumber, UUID companyId) {
        return userRepository.findByWhatsappNumberAndCompanyId(whatsappNumber, companyId)
                .orElse(null);
    }
    
    private String getUserWhatsappNumber(UUID userId, UUID companyId) {
        return userRepository.findById(userId)
                .filter(user -> user.getCompany().getId().equals(companyId))
                .filter(User::getIsWhatsappActive)
                .map(User::getWhatsappNumber)
                .orElse(null);
    }
    
    public void processIncomingMessage(IncomingMessage incomingMessage) {
        try {
            logger.info("Processing incoming message from: {} to: {}", 
                incomingMessage.getFrom(), incomingMessage.getTo());
            
            // Extract phone numbers
            String fromNumber = extractPhoneNumber(incomingMessage.getFrom());
            String toNumber = extractPhoneNumber(incomingMessage.getTo());
            
            // Determine company based on destination number
            Company company = findCompanyByWhatsAppNumber(toNumber);
            if (company == null) {
                logger.warn("No company found for WhatsApp number: {}", toNumber);
                return;
            }
            
            // Find or create customer
            Customer customer;
            try {
                CustomerDTO customerDTO = customerService.findByPhoneAndCompany(fromNumber, company.getId());
                customer = Customer.builder()
                    .id(customerDTO.getId())
                    .phone(customerDTO.getPhone())
                    .name(customerDTO.getName())
                    .company(company)
                    .build();
            } catch (IllegalArgumentException e) {
                // Customer not found, create new one
                customer = createCustomerFromWhatsApp(fromNumber, company);
            }
            
            // Find or create conversation
            ConversationDTO conversation = findOrCreateConversation(customer);
            
            // Save incoming message
            messageService.createFromIncomingMessage(incomingMessage, conversation.getId());
            
            logger.info("Successfully processed incoming message for conversation: {}", 
                conversation.getId());
            
        } catch (Exception e) {
            logger.error("Error processing incoming message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process incoming message", e);
        }
    }
    
    private String extractPhoneNumber(String twilioNumber) {
        if (twilioNumber == null) {
            return null;
        }
        
        // Remove 'whatsapp:' prefix if present
        String cleanNumber = twilioNumber.replace("whatsapp:", "");
        
        // Normalize the phone number using existing logic
        return customerService.normalizePhoneNumber(cleanNumber);
    }
    
    private Company findCompanyByWhatsAppNumber(String whatsappNumber) {
        if (whatsappNumber == null || whatsappNumber.trim().isEmpty()) {
            logger.warn("WhatsApp number is null or empty");
            return null;
        }
        
        // Normalize the WhatsApp number to match stored format
        String normalizedNumber = customerService.normalizePhoneNumber(whatsappNumber);
        
        // Find active user with this WhatsApp number across all companies
        Optional<User> userOptional = userRepository.findActiveByWhatsappNumber(normalizedNumber);
        User user = userOptional.orElse(null);
        
        if (user != null) {
            logger.info("Found company {} for WhatsApp number {}", 
                user.getCompany().getName(), whatsappNumber);
            return user.getCompany();
        }
        
        // If no user found, try to find by any company's WhatsApp configuration
        // This handles cases where the WhatsApp number might be configured at company level
        logger.warn("No active user found with WhatsApp number: {}. " +
            "Message will be ignored or you may need to configure WhatsApp numbers for users.", 
            whatsappNumber);
        
        return null;
    }
    
    private Customer createCustomerFromWhatsApp(String phoneNumber, Company company) {
        logger.info("Creating new customer for WhatsApp number: {} in company: {}", 
            phoneNumber, company.getName());
        
        // Generate default name from phone number
        String defaultName = "WhatsApp " + phoneNumber.substring(Math.max(0, phoneNumber.length() - 4));
        
        CreateCustomerDTO createDTO = CreateCustomerDTO.builder()
            .phone(phoneNumber)
            .name(defaultName)
            .build();
        
        CustomerDTO customerDTO = customerService.create(createDTO, company.getId());
        
        return Customer.builder()
            .id(customerDTO.getId())
            .phone(customerDTO.getPhone())
            .name(customerDTO.getName())
            .company(company)
            .whatsappId(phoneNumber)
            .isBlocked(false)
            .build();
    }
    
    private ConversationDTO findOrCreateConversation(Customer customer) {
        // First, try to find existing active WhatsApp conversations for this customer
        List<ConversationDTO> customerConversations = conversationService
            .findByCustomerAndCompany(customer.getId(), customer.getCompany().getId());
        
        // Look for active WhatsApp conversations (ENTRADA or ESPERANDO status)
        Optional<ConversationDTO> existingConversation = customerConversations.stream()
            .filter(conv -> conv.getChannel() == Channel.WHATSAPP)
            .filter(conv -> conv.getStatus() == ConversationStatus.ENTRADA || 
                           conv.getStatus() == ConversationStatus.ESPERANDO)
            .findFirst();
        
        if (existingConversation.isPresent()) {
            logger.info("Found existing active WhatsApp conversation: {} for customer: {}", 
                existingConversation.get().getId(), customer.getId());
            return existingConversation.get();
        }
        
        // No active conversation found, create a new one
        logger.info("Creating new WhatsApp conversation for customer: {}", customer.getId());
        
        CreateConversationDTO createDTO = CreateConversationDTO.builder()
            .customerId(customer.getId())
            .channel(Channel.WHATSAPP)
            .status(ConversationStatus.ENTRADA)
            .priority(1)
            .build();
        
        return conversationService.create(createDTO, customer.getCompany().getId());
    }
}