package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.entity.Customer;
import com.ruby.rubia_server.core.entity.MessageTemplate;
import com.ruby.rubia_server.dto.campaign.CreateCampaignDTO;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
    private final MessageTemplateService messageTemplateService;

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
            LocalDateTime startDate,
            LocalDateTime endDate,
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
                Customer customer = findOrCreateCustomer(contactData, companyId);
                
                // Verificar se já existe um CampaignContact para este customer
                List<CampaignContact> existingContacts = campaignContactService.findByCustomerId(customer.getId());
                boolean isDuplicate = existingContacts.stream()
                    .anyMatch(cc -> cc.getStatus() == CampaignContactStatus.OPT_OUT);
                
                if (isDuplicate) {
                    duplicates++;
                    log.debug("Customer {} já optou por não receber mensagens", customer.getName());
                    continue;
                }

                // Criar CampaignContact
                CampaignContact campaignContact = createCampaignContact(campaign, customer, templates);
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
        campaignService.update(campaign);

        log.info("Campanha {} criada com {} contatos. Processados: {}, Criados: {}, Duplicados: {}", 
                campaignName, created, processed, created, duplicates);

        return new CampaignProcessingResult(campaign, campaignContacts, errors, processed, created, duplicates);
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
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Assumir que a primeira linha contém os headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Arquivo Excel vazio ou inválido");
            }
            
            // Mapear colunas baseado nos headers
            Map<String, Integer> columnMap = mapColumns(headerRow);
            
            // Processar dados
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                ContactData contact = parseRowToContact(row, columnMap);
                if (contact != null) {
                    contacts.add(contact);
                }
            }
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
            
            // Telefone (combinar DDD + Telefone)
            String ddd = getStringValue(row, columnMap.get("ddd"));
            String telefone = getStringValue(row, columnMap.get("telefone"));
            if (ddd != null && telefone != null) {
                contact.setPhone(ddd + telefone);
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
            contact.setBirthDate(getDateValue(row, columnMap.get("datanascimento")));
            
            // Datas de doação
            contact.setLastDonation(getDateValue(row, columnMap.get("dataultima")));
            contact.setSecondLastDonation(getDateValue(row, columnMap.get("datapenultima")));
            contact.setThirdLastDonation(getDateValue(row, columnMap.get("dataantepenultima")));
            
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
        
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        
        return null;
    }

    private Campaign createCampaign(String name, String description, UUID companyId, UUID userId,
                                  LocalDateTime startDate, LocalDateTime endDate, String sourceSystem,
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

    private Customer findOrCreateCustomer(ContactData contactData, UUID companyId) {
        // Tentar encontrar por telefone primeiro
        if (contactData.getPhone() != null) {
            String normalizedPhone = customerService.normalizePhoneNumber(contactData.getPhone());
            Customer existingCustomer = customerService.findByPhoneAndCompany(normalizedPhone, companyId);
            if (existingCustomer != null) {
                return existingCustomer;
            }
        }
        
        // Criar novo customer
        Customer customer = Customer.builder()
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
        
        return customerService.create(customer, companyId);
    }

    private CampaignContact createCampaignContact(Campaign campaign, Customer customer, List<MessageTemplate> templates) {
        // Distribuir templates de forma rotativa
        MessageTemplate selectedTemplate = templates.get(customer.hashCode() % templates.size());
        
        CampaignContact campaignContact = CampaignContact.builder()
            .campaign(campaign)
            .customer(customer)
            .status(CampaignContactStatus.PENDING)
            .build();
        
        return campaignContactService.create(campaignContact);
    }
}