package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.entity.Message;
import com.ruby.rubia_server.dto.campaign.CreateCampaignDTO;
import com.ruby.rubia_server.dto.campaign.UpdateCampaignDTO;
import com.ruby.rubia_server.core.dto.CreateCustomerDTO;
import com.ruby.rubia_server.core.dto.UpdateCustomerDTO;
import com.ruby.rubia_server.core.dto.CustomerDTO;
import com.ruby.rubia_server.core.dto.CreateCampaignContactDTO;
import com.ruby.rubia_server.core.dto.CreateConversationDTO;
import com.ruby.rubia_server.core.dto.CreateMessageDTO;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import com.ruby.rubia_server.core.enums.ConversationType;
import com.ruby.rubia_server.core.enums.MessageStatus;
import com.ruby.rubia_server.core.enums.SenderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignProcessingService {

    private final CampaignService campaignService;
    private final CampaignContactService campaignContactService;
    private final CustomerService customerService;
    private final PhoneService phoneService;
    private final MessageTemplateService messageTemplateService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CampaignMessagingService campaignMessagingService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String QUEUE_KEY = "rubia:campaign:queue";

    public static class CampaignProcessingResult {
        private final Campaign campaign;
        private final List<CampaignContact> contacts;
        private final List<String> errors;
        private final int processed;
        private final int created;
        private final int duplicates;

        public CampaignProcessingResult(Campaign campaign, List<CampaignContact> contacts, 
                                     List<String> errors, int processed, int created, int duplicates) {
            this.campaign = campaign;
            this.contacts = contacts;
            this.errors = errors;
            this.processed = processed;
            this.created = created;
            this.duplicates = duplicates;
        }

        public Campaign getCampaign() { return campaign; }
        public List<CampaignContact> getContacts() { return contacts; }
        public List<String> getErrors() { return errors; }
        public int getProcessed() { return processed; }
        public int getCreated() { return created; }
        public int getDuplicates() { return duplicates; }
    }

    public static class ContactData {
        private String name;
        private String phone;
        private String email;
        private String cpf;
        private String rg;
        private String bloodType;
        private String rhFactor;
        private String city;
        private String state;
        private LocalDate birthDate;
        private LocalDate lastDonation;
        private LocalDate secondLastDonation;
        private LocalDate thirdLastDonation;

        // Getters e Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCpf() { return cpf; }
        public void setCpf(String cpf) { this.cpf = cpf; }
        public String getRg() { return rg; }
        public void setRg(String rg) { this.rg = rg; }
        public String getBloodType() { return bloodType; }
        public void setBloodType(String bloodType) { this.bloodType = bloodType; }
        public String getRhFactor() { return rhFactor; }
        public void setRhFactor(String rhFactor) { this.rhFactor = rhFactor; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public LocalDate getBirthDate() { return birthDate; }
        public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
        public LocalDate getLastDonation() { return lastDonation; }
        public void setLastDonation(LocalDate lastDonation) { this.lastDonation = lastDonation; }
        public LocalDate getSecondLastDonation() { return secondLastDonation; }
        public void setSecondLastDonation(LocalDate secondLastDonation) { this.secondLastDonation = secondLastDonation; }
        public LocalDate getThirdLastDonation() { return thirdLastDonation; }
        public void setThirdLastDonation(LocalDate thirdLastDonation) { this.thirdLastDonation = thirdLastDonation; }
    }

    @Transactional
    public CampaignProcessingResult processExcelAndCreateCampaign(
            MultipartFile file,
            String campaignName,
            String description,
            UUID companyId,
            UUID userId,
            LocalDate startDate,
            LocalDate endDate,
            String sourceSystem,
            List<UUID> templateIds) throws IOException {

        log.info("Iniciando processamento de campanha: {}", campaignName);

        // Validar templates
        List<MessageTemplate> templates = validateTemplates(templateIds, companyId);
        
        // Processar arquivo Excel
        List<ContactData> contactsData = parseExcelFile(file);
        log.info("Processados {} contatos do arquivo Excel", contactsData.size());

        // Criar campanha
        Campaign campaign = createCampaign(campaignName, description, companyId, userId, 
                                         startDate, endDate, sourceSystem, templates.get(0));

        // Processar contatos e criar CampaignContacts
        List<CampaignContact> campaignContacts = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int processed = 0;
        int created = 0;
        int duplicates = 0;

        for (ContactData contactData : contactsData) {
            try {
                processed++;
                
                // Buscar ou criar customer
                CustomerDTO customer = findOrCreateCustomer(contactData, companyId);
                
                // Verificar se já existe um CampaignContact para este customer NESTA campanha
                List<CampaignContact> existingContacts = campaignContactService.findByCustomerId(customer.getId());
                boolean alreadyInThisCampaign = existingContacts.stream()
                    .anyMatch(cc -> cc.getCampaign().getId().equals(campaign.getId()));
                
                if (alreadyInThisCampaign) {
                    duplicates++;
                    log.debug("Customer {} já existe na campanha {}", customer.getName(), campaign.getName());
                    continue;
                }
                
                // Verificar se o customer tem OPT_OUT global (em qualquer campanha)
                boolean hasGlobalOptOut = existingContacts.stream()
                    .anyMatch(cc -> cc.getStatus() == CampaignContactStatus.OPT_OUT);
                
                if (hasGlobalOptOut) {
                    duplicates++;
                    log.debug("Customer {} optou por não receber mensagens (opt-out global)", customer.getName());
                    continue;
                }

                // Criar CampaignContact, Conversa e Mensagem DRAFT
                CampaignContact campaignContact = createCampaignContact(campaign, customer, templates, companyId);
                campaignContacts.add(campaignContact);
                created++;

            } catch (Exception e) {
                log.error("Erro ao processar contato {}: {}", contactData.getName(), e.getMessage());
                errors.add(String.format("Erro ao processar %s: %s", contactData.getName(), e.getMessage()));
            }
        }

        // Atualizar estatísticas da campanha
        campaign.setTotalContacts(created);
        campaign.setStatus(CampaignStatus.ACTIVE);
        UpdateCampaignDTO updateDTO = UpdateCampaignDTO.builder()
            .totalContacts(created)
            .status(CampaignStatus.ACTIVE)
            .build();
        campaignService.update(campaign.getId(), updateDTO);

        log.info("Campanha {} criada com {} contatos. Processados: {}, Criados: {}, Duplicados: {}", 
                campaignName, created, processed, created, duplicates);

        // Adicionar campanha à fila segura de processamento se houver contatos criados
        if (created > 0) {
            log.info("Adicionando campanha {} à fila segura com {} contatos", 
                    campaign.getId(), created);
            
            try {
                // Adicionar contatos pendentes diretamente à fila do CampaignQueueProcessor
                enqueueCampaignContacts(campaign, companyId.toString());
            } catch (Exception e) {
                log.error("Erro ao adicionar campanha {} à fila: {}", 
                        campaign.getId(), e.getMessage());
                throw new RuntimeException("Falha ao adicionar campanha à fila de processamento", e);
            }
        }

        return new CampaignProcessingResult(campaign, campaignContacts, errors, processed, created, duplicates);
    }

    /**
     * Adiciona contatos de campanha pendentes diretamente à fila do CampaignQueueProcessor
     */
    private void enqueueCampaignContacts(Campaign campaign, String companyId) {
        log.info("🔄 Enfileirando contatos da campanha {} para processamento", campaign.getId());
        
        // Buscar contatos pendentes da campanha
        List<CampaignContact> pendingContacts = campaignContactService
            .findByCampaignIdAndStatus(campaign.getId(), CampaignContactStatus.PENDING);
        
        if (pendingContacts.isEmpty()) {
            log.info("Nenhum contato pendente para campanha {}", campaign.getId());
            return;
        }
        
        int enqueued = 0;
        long baseTimestamp = System.currentTimeMillis();
        
        for (int i = 0; i < pendingContacts.size(); i++) {
            CampaignContact contact = pendingContacts.get(i);
            
            try {
                // Criar item da fila
                CampaignQueueProcessor.CampaignQueueItem queueItem = 
                    new CampaignQueueProcessor.CampaignQueueItem(
                        campaign.getId(),
                        contact.getId(),
                        companyId
                    );
                
                // Serializar para JSON
                String itemJson = objectMapper.writeValueAsString(queueItem);
                
                // Adicionar à fila Redis com timestamp escalonado para evitar picos
                long timestamp = baseTimestamp + (i * 1000); // 1 segundo de intervalo entre cada
                redisTemplate.opsForZSet().add(QUEUE_KEY, itemJson, timestamp);
                
                enqueued++;
                
            } catch (Exception e) {
                log.error("❌ Erro ao enfileirar contato {}: {}", contact.getId(), e.getMessage());
            }
        }
        
        log.info("✅ {} contatos da campanha {} adicionados à fila", enqueued, campaign.getId());
    }

    private List<MessageTemplate> validateTemplates(List<UUID> templateIds, UUID companyId) {
        List<MessageTemplate> templates = new ArrayList<>();
        for (UUID templateId : templateIds) {
            Optional<MessageTemplate> optionalTemplate = messageTemplateService.findById(templateId);
            if (optionalTemplate.isEmpty()) {
                throw new IllegalArgumentException("Template não encontrado: " + templateId);
            }
            
            MessageTemplate template = optionalTemplate.get();
            if (!template.getCompany().getId().equals(companyId)) {
                throw new IllegalArgumentException("Template não pertence a esta empresa");
            }
            templates.add(template);
        }
        
        if (templates.isEmpty()) {
            throw new IllegalArgumentException("Pelo menos um template deve ser selecionado");
        }
        
        return templates;
    }

    private List<ContactData> parseExcelFile(MultipartFile file) throws IOException {
        List<ContactData> contacts = new ArrayList<>();
        
        // Validar tipo de arquivo
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx") && !filename.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("Arquivo deve ser do tipo Excel (.xlsx ou .xls)");
        }
        
        // Validar tamanho do arquivo
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo está vazio");
        }
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Arquivo Excel não possui planilhas");
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Verificar se a planilha tem dados
            if (sheet.getLastRowNum() < 1) {
                throw new IllegalArgumentException("Planilha não possui dados ou apenas cabeçalho");
            }
            
            // Assumir que a primeira linha contém os headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Primeira linha (cabeçalho) está vazia");
            }
            
            // Mapear colunas baseado nos headers
            Map<String, Integer> columnMap = mapColumns(headerRow);
            
            // Validar se as colunas essenciais existem
            if (!columnMap.containsKey("nome")) {
                throw new IllegalArgumentException("Coluna 'Nome' é obrigatória no arquivo Excel");
            }
            
            // Processar dados
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                ContactData contact = parseRowToContact(row, columnMap);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
            
            if (contacts.isEmpty()) {
                throw new IllegalArgumentException("Nenhum contato válido encontrado no arquivo");
            }
            
        } catch (Exception e) {
            log.error("Erro ao processar arquivo Excel: {}", e.getMessage());
            if (e.getCause() instanceof org.apache.poi.openxml4j.exceptions.InvalidFormatException) {
                throw new IllegalArgumentException("Arquivo Excel corrompido ou em formato inválido. Certifique-se de que é um arquivo .xlsx válido");
            }
            throw new IOException("Erro ao processar arquivo Excel: " + e.getMessage(), e);
        }
        
        return contacts;
    }

    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String header = cell.getStringCellValue().toLowerCase().trim();
                columnMap.put(header, i);
            }
        }
        
        return columnMap;
    }

    private ContactData parseRowToContact(Row row, Map<String, Integer> columnMap) {
        ContactData contact = new ContactData();
        
        try {
            // Nome
            contact.setName(getStringValue(row, columnMap.get("nome")));
            
            // Telefone (combinar DDD + Telefone Celular)
            String ddd = getStringValue(row, columnMap.get("dddtelefonecelular"));
            String telefone = getStringValue(row, columnMap.get("telefonecelular"));
            if (ddd != null && telefone != null) {
                contact.setPhone(ddd + telefone);
            } else if (telefone != null) {
                contact.setPhone(telefone);
            }
            
            // CPF
            contact.setCpf(getStringValue(row, columnMap.get("cpf")));
            
            // RG
            contact.setRg(getStringValue(row, columnMap.get("rg")));
            
            // Tipo sanguíneo
            contact.setBloodType(getStringValue(row, columnMap.get("tiposanguineo")));
            
            // Fator RH
            contact.setRhFactor(getStringValue(row, columnMap.get("fatorrh")));
            
            // Cidade
            contact.setCity(getStringValue(row, columnMap.get("cidade")));
            
            // Estado
            contact.setState(getStringValue(row, columnMap.get("estado")));
            
            // Data de nascimento
            Integer birthDateColumn = columnMap.get("datanascimento");
            if (birthDateColumn != null) {
                LocalDate birthDate = getDateValue(row, birthDateColumn);
                contact.setBirthDate(birthDate);
                if (birthDate != null) {
                    log.info("✅ Data de nascimento processada: {} para contato: {}", birthDate, contact.getName());
                } else {
                    log.warn("⚠️ Falha ao processar data de nascimento para contato: {}", contact.getName());
                }
            } else {
                log.warn("❌ Coluna 'DataNascimento' não encontrada no arquivo Excel. Colunas disponíveis: {}", columnMap.keySet());
            }
            
            // Datas de doação
            contact.setLastDonation(getDateValue(row, columnMap.get("dataultimadoacao")));
            contact.setSecondLastDonation(getDateValue(row, columnMap.get("datapenultimadoacao")));
            contact.setThirdLastDonation(getDateValue(row, columnMap.get("dataantepenultimadoacao")));
            
            // Validar dados mínimos
            if (contact.getName() == null || contact.getName().trim().isEmpty()) {
                return null;
            }
            
            return contact;
            
        } catch (Exception e) {
            log.error("Erro ao processar linha do Excel: {}", e.getMessage());
            return null;
        }
    }

    private String getStringValue(Row row, Integer columnIndex) {
        if (columnIndex == null) return null;
        
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            default:
                return null;
        }
    }

    private LocalDate getDateValue(Row row, Integer columnIndex) {
        if (columnIndex == null) return null;
        
        Cell cell = row.getCell(columnIndex);
        if (cell == null) return null;
        
        try {
            // Tentar como data formatada do Excel
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                LocalDate date = cell.getLocalDateTimeCellValue().toLocalDate();
                log.debug("📅 Data processada como numérica: {}", date);
                return date;
            }
            
            // Tentar como string no formato ISO (2003-09-18 00:00:00.000)
            if (cell.getCellType() == CellType.STRING) {
                String dateStr = cell.getStringCellValue().trim();
                log.debug("📅 Processando data como string: '{}'", dateStr);
                
                if (dateStr.isEmpty()) {
                    return null;
                }
                
                // Remover timestamp se existir (2003-09-18 00:00:00.000 -> 2003-09-18)
                if (dateStr.contains(" ")) {
                    dateStr = dateStr.split(" ")[0];
                }
                
                // Tentar diferentes formatos
                try {
                    LocalDate date = LocalDate.parse(dateStr); // ISO format: 2003-09-18
                    log.debug("📅 Data processada como ISO: {}", date);
                    return date;
                } catch (Exception e1) {
                    // Tentar formato brasileiro: dd/MM/yyyy
                    try {
                        String[] parts = dateStr.split("/");
                        if (parts.length == 3) {
                            int day = Integer.parseInt(parts[0]);
                            int month = Integer.parseInt(parts[1]);
                            int year = Integer.parseInt(parts[2]);
                            LocalDate date = LocalDate.of(year, month, day);
                            log.debug("📅 Data processada como dd/MM/yyyy: {}", date);
                            return date;
                        }
                    } catch (Exception e2) {
                        log.warn("Falha ao processar data '{}' nos formatos ISO e dd/MM/yyyy", dateStr);
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Erro ao processar data na linha {}, coluna {}: {}", 
                    row.getRowNum(), columnIndex, e.getMessage());
        }
        
        return null;
    }

    private Campaign createCampaign(String name, String description, UUID companyId, UUID userId,
                                  LocalDate startDate, LocalDate endDate, String sourceSystem,
                                  MessageTemplate initialTemplate) {
        CreateCampaignDTO createDTO = CreateCampaignDTO.builder()
            .name(name)
            .description(description)
            .companyId(companyId)
            .createdByUserId(userId)
            .startDate(startDate)
            .endDate(endDate)
            .sourceSystemName(sourceSystem)
            .initialMessageTemplateId(initialTemplate.getId())
            .status(CampaignStatus.DRAFT)
            .totalContacts(0)
            .build();
        
        return campaignService.create(createDTO);
    }

    private CustomerDTO findOrCreateCustomer(ContactData contactData, UUID companyId) {
        // Tentar encontrar por telefone primeiro
        if (contactData.getPhone() != null) {
            String normalizedPhone = phoneService.normalize(contactData.getPhone());
            CustomerDTO existingCustomer = customerService.findByPhoneAndCompany(normalizedPhone, companyId);
            if (existingCustomer != null) {
                // Customer existe - verificar se precisa atualizar dados
                boolean needsUpdate = false;
                UpdateCustomerDTO updateDTO = UpdateCustomerDTO.builder()
                    .name(contactData.getName() != null ? contactData.getName() : existingCustomer.getName())
                    .phone(contactData.getPhone() != null ? contactData.getPhone() : existingCustomer.getPhone())
                    .bloodType(contactData.getBloodType() != null ? contactData.getBloodType() : existingCustomer.getBloodType())
                    .addressCity(contactData.getCity() != null ? contactData.getCity() : existingCustomer.getAddressCity())
                    .addressState(contactData.getState() != null ? contactData.getState() : existingCustomer.getAddressState())
                    .birthDate(contactData.getBirthDate() != null ? contactData.getBirthDate() : existingCustomer.getBirthDate())
                    .lastDonationDate(contactData.getLastDonation() != null ? contactData.getLastDonation() : existingCustomer.getLastDonationDate())
                    .build();
                
                // Verificar se houve mudanças nos dados importantes
                if (contactData.getBirthDate() != null && !contactData.getBirthDate().equals(existingCustomer.getBirthDate())) {
                    needsUpdate = true;
                    log.info("🔄 Atualizando birthDate do customer {}: {} -> {}", 
                            existingCustomer.getName(), existingCustomer.getBirthDate(), contactData.getBirthDate());
                }
                
                if (contactData.getBloodType() != null && !contactData.getBloodType().equals(existingCustomer.getBloodType())) {
                    needsUpdate = true;
                    log.info("🔄 Atualizando bloodType do customer {}: {} -> {}", 
                            existingCustomer.getName(), existingCustomer.getBloodType(), contactData.getBloodType());
                }
                
                if (contactData.getLastDonation() != null && !contactData.getLastDonation().equals(existingCustomer.getLastDonationDate())) {
                    needsUpdate = true;
                    log.info("🔄 Atualizando lastDonation do customer {}: {} -> {}", 
                            existingCustomer.getName(), existingCustomer.getLastDonationDate(), contactData.getLastDonation());
                }
                
                if (needsUpdate) {
                    try {
                        return customerService.update(existingCustomer.getId(), updateDTO, companyId);
                    } catch (Exception e) {
                        log.warn("Erro ao atualizar customer {}: {}", existingCustomer.getName(), e.getMessage());
                        return existingCustomer; // Retornar dados antigos se falhar
                    }
                } else {
                    log.debug("Customer {} já tem dados atualizados", existingCustomer.getName());
                    return existingCustomer;
                }
            }
        }
        
        // Criar novo customer usando DTO
        CreateCustomerDTO createDTO = CreateCustomerDTO.builder()
            .name(contactData.getName())
            .phone(contactData.getPhone())
            .email(contactData.getEmail())
            .cpf(contactData.getCpf())
            .rg(contactData.getRg())
            .bloodType(contactData.getBloodType())
            .rhFactor(contactData.getRhFactor())
            .addressCity(contactData.getCity())
            .addressState(contactData.getState())
            .birthDate(contactData.getBirthDate())
            .lastDonationDate(contactData.getLastDonation())
            .isBlocked(false)
            .build();
        
        return customerService.create(createDTO, companyId);
    }

    private CampaignContact createCampaignContact(Campaign campaign, CustomerDTO customerDTO, List<MessageTemplate> templates, UUID companyId) {
        // Distribuir templates de forma rotativa
        int templateIndex = Math.abs(customerDTO.hashCode()) % templates.size();
        MessageTemplate selectedTemplate = templates.get(templateIndex);
        
        // Criar CampaignContact
        CreateCampaignContactDTO createDTO = CreateCampaignContactDTO.builder()
            .campaignId(campaign.getId())
            .customerId(customerDTO.getId())
            .status(CampaignContactStatus.PENDING)
            .build();
        
        CampaignContact campaignContact = campaignContactService.create(createDTO);
        
        // Verificar se já existe uma conversa para este customer + campanha
        var existingConversation = conversationService.findByCustomerIdAndCampaignId(customerDTO.getId(), campaign.getId());
        
        var conversation = existingConversation.orElseGet(() -> {
            // Criar Conversa para esta campanha + customer
            CreateConversationDTO conversationDTO = CreateConversationDTO.builder()
                .customerId(customerDTO.getId())
                .channel(Channel.WHATSAPP) // Assumindo WhatsApp por padrão
                .status(ConversationStatus.ENTRADA) // Status inicial
                .conversationType(ConversationType.ONE_TO_ONE)
                .campaignId(campaign.getId()) // Associar à campanha
                .build();
            
            return conversationService.create(conversationDTO, companyId);
        });
        
        // Verificar se já existe uma mensagem DRAFT nesta conversa
        boolean hasDraftMessage = messageService.hasDraftMessage(conversation.getId());
        
        if (!hasDraftMessage) {
            // Personalizar o conteúdo do template antes de criar a mensagem DRAFT
            String personalizedContent = personalizeTemplateContent(selectedTemplate.getContent(), customerDTO);
            
            // Criar Mensagem DRAFT com o template personalizado
            CreateMessageDTO messageDTO = CreateMessageDTO.builder()
                .conversationId(conversation.getId())
                .companyId(companyId) // Necessário para validações
                .content(personalizedContent) // Conteúdo do template personalizado
                .senderType(SenderType.AGENT) // Será enviado pelo agente
                .status(MessageStatus.DRAFT) // Status DRAFT
                .messageTemplateId(selectedTemplate.getId()) // Referência ao template
                .campaignContactId(campaignContact.getId()) // Relacionamento direto
                .build();
            
            messageService.create(messageDTO);
        }
        
        log.debug("Criada conversa {} com mensagem DRAFT para customer {} na campanha {}", 
                 conversation.getId(), customerDTO.getName(), campaign.getName());
        
        return campaignContact;
    }

    /**
     * Personaliza o conteúdo do template substituindo variáveis pelos dados do cliente
     */
    private String personalizeTemplateContent(String template, CustomerDTO customer) {
        String message = template;
        
        if (customer != null && customer.getName() != null) {
            message = message.replace("{{nome}}", customer.getName());
        } else {
            message = message.replace("{{nome}}", "");
        }
        
        return message;
    }
}