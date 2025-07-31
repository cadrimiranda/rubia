package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.adapter.MessagingAdapter;
import com.ruby.rubia_server.core.adapter.impl.ZApiAdapter;
import com.ruby.rubia_server.core.entity.MessageResult;
import com.ruby.rubia_server.core.entity.IncomingMessage;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.WhatsAppInstance;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.SenderType;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.MessageType;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.WhatsAppInstanceRepository;
import com.ruby.rubia_server.core.service.CustomerService;
import com.ruby.rubia_server.core.service.ConversationService;
import com.ruby.rubia_server.core.service.MessageService;
import com.ruby.rubia_server.core.service.WebSocketNotificationService;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.ConversationDTO;
import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.MessageDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
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
    private WhatsAppInstanceRepository whatsAppInstanceRepository;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private WebSocketNotificationService webSocketNotificationService;
    
    @Autowired
    private PhoneService phoneService;
    
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
            logger.debug("Processing message from: {} via {}", 
                incomingMessage.getFrom(), incomingMessage.getProvider());
            
            String fromNumber = phoneService.extractFromProvider(incomingMessage.getFrom());
            
            Company company;
            if ("z-api".equals(incomingMessage.getProvider())) {
                // Use connectedPhone from Z-API webhook to identify the correct company
                String connectedPhone = incomingMessage.getConnectedPhone();
                if (connectedPhone != null && !connectedPhone.trim().isEmpty()) {
                    company = findCompanyByWhatsAppInstanceWithVariations(connectedPhone);
                } else {
                    logger.error("No connectedPhone in Z-API webhook. Cannot identify company.");
                    throw new RuntimeException("Missing connectedPhone in Z-API webhook");
                }
            } else {
                String toNumber = phoneService.extractFromProvider(incomingMessage.getTo());
                company = findCompanyByWhatsAppNumber(toNumber);
            }
            
            if (company == null) {
                logger.warn("No company found for incoming message from: {}", fromNumber);
                return;
            }
            
            // Find or create customer with phone variations
            Customer customer = findOrCreateCustomerWithVariations(incomingMessage, company);
            
            // Find or create conversation using chatLid
            ConversationDTO conversation = findOrCreateConversationByChatLid(incomingMessage.getChatLid(), customer);
            
            // Save message and notify
            MessageDTO savedMessage = messageService.createFromIncomingMessage(incomingMessage, conversation.getId());
            webSocketNotificationService.notifyNewMessage(savedMessage, conversation);
            
            logger.info("Message processed: {} -> {}", incomingMessage.getFrom(), conversation.getId());
            
        } catch (Exception e) {
            logger.error("Error processing incoming message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process incoming message", e);
        }
    }
    
    
    private Company findCompanyByWhatsAppInstanceWithVariations(String connectedPhone) {
        if (connectedPhone == null || connectedPhone.trim().isEmpty()) {
            logger.warn("ConnectedPhone is null or empty");
            return null;
        }
        
        String[] phoneVariations = phoneService.generatePhoneVariations(connectedPhone);
        
        for (String variation : phoneVariations) {
            if (variation != null) {
                Company company = findCompanyByWhatsAppInstance(variation);
                if (company != null) {
                    logger.info("Found company: {}", company.getName());
                    return company;
                }
            }
        }
        
        logger.warn("No company found for connectedPhone: {}", connectedPhone);
        return null;
    }

    private Company findCompanyByWhatsAppNumber(String whatsappNumber) {
        if (whatsappNumber == null || whatsappNumber.trim().isEmpty()) {
            logger.warn("WhatsApp number is null or empty");
            return null;
        }
        
        // Normalize the WhatsApp number to match stored format
        String normalizedNumber = phoneService.normalize(whatsappNumber);
        
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

    private Customer findOrCreateCustomerWithVariations(IncomingMessage incomingMessage, Company company) {
        String fromNumber = phoneService.extractFromProvider(incomingMessage.getFrom());
        String[] phoneVariations = phoneService.generatePhoneVariations(fromNumber);
        
        // Try to find existing customer with any variation
        for (String variation : phoneVariations) {
            if (variation != null) {
                CustomerDTO customerDTO = customerService.findByPhoneAndCompany(variation, company.getId());
                if (customerDTO != null) {
                    logger.info("Found customer: {} ({})", customerDTO.getName(), customerDTO.getPhone());
                    return Customer.builder()
                        .id(customerDTO.getId())
                        .phone(customerDTO.getPhone())
                        .name(customerDTO.getName())
                        .company(company)
                        .build();
                }
            }
        }
        
        // Customer not found with any variation, create new one
        logger.info("Creating new customer for: {}", fromNumber);
        
        return createCustomerFromWhatsApp(incomingMessage, company);
    }
    
    private Customer createCustomerFromWhatsApp(IncomingMessage incomingMessage, Company company) {
        String phoneNumber = phoneService.extractFromProvider(incomingMessage.getFrom());
        logger.info("Creating new customer for WhatsApp number: {} in company: {}", 
            phoneNumber, company.getName());
        
        // Use senderName from Z-API if available, otherwise generate default name
        String customerName = (incomingMessage.getSenderName() != null && !incomingMessage.getSenderName().trim().isEmpty()) 
            ? incomingMessage.getSenderName().trim()
            : phoneService.generateDefaultName(phoneNumber);
        
        CreateCustomerDTO createDTO = CreateCustomerDTO.builder()
            .phone(phoneNumber)
            .name(customerName)
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
    
    private ConversationDTO findOrCreateConversationByChatLid(String chatLid, Customer customer) {
        logger.debug("Looking for conversation with chatLid: {} for customer: {}", chatLid, customer.getId());
        
        // First, try to find conversation by chatLid if provided
        if (chatLid != null && !chatLid.trim().isEmpty()) {
            Optional<ConversationDTO> conversationByChatLid = conversationService.findByChatLid(chatLid);
            if (conversationByChatLid.isPresent()) {
                logger.debug("Found existing conversation by chatLid: {}", conversationByChatLid.get().getId());
                return conversationByChatLid.get();
            }
        }
        
        // Fallback to existing logic: find by customer and company
        List<ConversationDTO> customerConversations = conversationService
            .findByCustomerAndCompany(customer.getId(), customer.getCompany().getId());
        
        logger.debug("Found {} conversations for customer {}", customerConversations.size(), customer.getId());
        
        // Look for active WhatsApp conversations (ENTRADA or ESPERANDO status)
        Optional<ConversationDTO> existingConversation = customerConversations.stream()
            .filter(conv -> {
                boolean isWhatsApp = conv.getChannel() == Channel.WHATSAPP;
                boolean isActive = conv.getStatus() == ConversationStatus.ENTRADA || conv.getStatus() == ConversationStatus.ESPERANDO;
                return isWhatsApp && isActive;
            })
            .findFirst();
        
        if (existingConversation.isPresent()) {
            logger.debug("Using existing WhatsApp conversation: {}", existingConversation.get().getId());
            // Update the conversation with chatLid if it doesn't have one
            ConversationDTO conversation = existingConversation.get();
            if (chatLid != null && !chatLid.trim().isEmpty() && conversation.getChatLid() == null) {
                logger.info("Updating conversation {} with chatLid: {}", conversation.getId(), chatLid);
                conversationService.updateChatLid(conversation.getId(), chatLid);
                conversation.setChatLid(chatLid);
            }
            return conversation;
        }
        
        // No active conversation found, create a new one
        logger.info("Creating new conversation for customer: {} with chatLid: {}", customer.getId(), chatLid);
        
        CreateConversationDTO createDTO = CreateConversationDTO.builder()
            .customerId(customer.getId())
            .channel(Channel.WHATSAPP)
            .status(ConversationStatus.ENTRADA)
            .priority(1)
            .chatLid(chatLid)
            .build();
        
        return conversationService.create(createDTO, customer.getCompany().getId());
    }

    private ConversationDTO findOrCreateConversation(Customer customer) {
        return findOrCreateConversationByChatLid(null, customer);
    }
    
    public Company findCompanyByZApiInstance(String instanceId) {
        return companyRepository.findAll().stream()
            .filter(Company::getIsActive)
            .findFirst()
            .orElse(null);
    }

    /**
     * Encontra a empresa pela instância WhatsApp usando o número conectado
     */
    public Company findCompanyByWhatsAppInstance(String connectedPhone) {
        if (connectedPhone == null || connectedPhone.trim().isEmpty()) {
            logger.warn("Connected phone is null or empty");
            return null;
        }
        
        // Normalize phone to standard format (+55DDDnúmero)
        String normalizedPhone = phoneService.normalize(connectedPhone);
        
        Optional<WhatsAppInstance> instanceOptional = whatsAppInstanceRepository
            .findByPhoneNumberAndIsActiveTrue(normalizedPhone);

        if (instanceOptional.isPresent()) {
            WhatsAppInstance instance = instanceOptional.get();
            return instance.getCompany();
        }

        logger.debug("No company found for WhatsApp phone: {}", connectedPhone);
        return null;
    }

    public MessageResult sendImageByUrl(String to, String imageUrl, String caption) {
        if (currentAdapter instanceof ZApiAdapter zapiAdapter) {
            return zapiAdapter.sendImageByUrl(to, imageUrl, caption);
        }
        return MessageResult.error("Image sending not supported by current adapter", getCurrentProvider());
    }

    public MessageResult sendDocumentByUrl(String to, String documentUrl, String caption, String fileName) {
        if (currentAdapter instanceof ZApiAdapter zapiAdapter) {
            return zapiAdapter.sendDocumentByUrl(to, documentUrl, caption, fileName);
        }
        return MessageResult.error("Document sending not supported by current adapter", getCurrentProvider());
    }

    public MessageResult sendFileBase64(String to, String base64Data, String fileName, String caption) {
        if (currentAdapter instanceof ZApiAdapter zapiAdapter) {
            return zapiAdapter.sendFileBase64(to, base64Data, fileName, caption);
        }
        return MessageResult.error("Base64 file sending not supported by current adapter", getCurrentProvider());
    }

    public String uploadFile(MultipartFile file) {
        if (currentAdapter instanceof ZApiAdapter zapiAdapter) {
            return zapiAdapter.uploadFile(file);
        }
        throw new RuntimeException("File upload not supported by current adapter");
    }

    public MessageResult sendMediaByUrl(String to, String mediaUrl, String caption) {
        return currentAdapter.sendMediaMessage(to, mediaUrl, caption);
    }
}