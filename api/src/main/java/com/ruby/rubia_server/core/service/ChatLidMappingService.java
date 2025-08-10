package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.entity.ChatLidMapping;
import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.repository.ChatLidMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service para gerenciar mapeamento entre chatLid do WhatsApp e conversas internas
 * Resolve problema de conversas iniciadas por campanhas que não possuem chatLid
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatLidMappingService {

    private final ChatLidMappingRepository repository;
    private final ConversationService conversationService;
    private final PhoneService phoneService;

    /**
     * Encontra ou cria mapping para webhook do WhatsApp
     * Estratégia: busca por chatLid, se não existir busca conversa por telefone e cria mapping
     */
    @Transactional
    public ChatLidMapping findOrCreateMapping(String chatLid, String phone, UUID companyId, UUID instanceId) {
        validateInput(chatLid, phone, companyId);

        

        // Primeiro tenta encontrar mapping existente
        Optional<ChatLidMapping> existingMapping = repository.findByChatLid(chatLid);
        if (existingMapping.isPresent()) {
            
            return existingMapping.get();
        }

        

        // Se não existe, busca conversa existente por telefone
        // Como não temos método direto, vamos usar uma abordagem alternativa
        log.info("📞 Nenhuma conversa encontrada para chatLid {}, será criada dinamicamente via webhook", chatLid);
        
        // Retornar mapping temporário - a conversa será criada quando necessário
        ChatLidMapping tempMapping = ChatLidMapping.builder()
            .chatLid(chatLid)
            .phone(phone)
            .companyId(companyId)
            .whatsappInstanceId(instanceId)
            .fromCampaign(false)
            .build();

        // Não salvar ainda - será salvo quando conversa for criada
        return tempMapping;
    }

    /**
     * Busca conversa pelo chatLid (principal método usado no webhook)
     */
    public Optional<Conversation> findConversationByChatLid(String chatLid) {
        

        return repository.findByChatLid(chatLid)
            .flatMap(mapping -> {
                
                // Como findById precisa de companyId, vamos buscar pela entidade
                return Optional.ofNullable(conversationService.findById(mapping.getConversationId(), mapping.getCompanyId()))
                    .map(dto -> {
                        // Converter DTO para Entity (simplificado)
                        Conversation conversation = new Conversation();
                        conversation.setId(dto.getId());
                        conversation.setChatLid(dto.getChatLid());
                        // outros campos necessários seriam preenchidos aqui
                        return conversation;
                    });
            });
    }

    /**
     * Cria mapping para campanhas (sem chatLid inicial)
     */
    @Transactional
    public ChatLidMapping createMappingForCampaign(UUID conversationId, String phone, UUID companyId, UUID instanceId) {
        

        ChatLidMapping campaignMapping = ChatLidMapping.builder()
            .conversationId(conversationId)
            .phone(phone)
            .companyId(companyId)
            .whatsappInstanceId(instanceId)
            .fromCampaign(true)
            // chatLid será null até receber resposta do cliente
            .build();

        ChatLidMapping saved = repository.save(campaignMapping);
        log.info("📊 Mapping de campanha criado: conversationId={}", saved.getConversationId());

        return saved;
    }

    /**
     * Cria mapping para campanhas com ID da campanha (sem chatLid inicial)
     */
    @Transactional
    public ChatLidMapping createMappingForCampaign(UUID conversationId, String phone, UUID companyId, UUID instanceId, UUID campaignId) {
        

        ChatLidMapping campaignMapping = ChatLidMapping.builder()
            .conversationId(conversationId)
            .phone(phone)
            .companyId(companyId)
            .whatsappInstanceId(instanceId)
            .fromCampaign(true)
            .campaignId(campaignId)
            // chatLid será null até receber resposta do cliente
            .build();

        ChatLidMapping saved = repository.save(campaignMapping);
        log.info("📊 Mapping de campanha criado: conversationId={}", saved.getConversationId());

        return saved;
    }

    /**
     * Atualiza mapping de campanha com chatLid quando cliente responde
     */
    @Transactional
    public Optional<ChatLidMapping> updateMappingWithChatLid(UUID conversationId, String chatLid) {
        

        return repository.findByConversationId(conversationId)
            .map(mapping -> {
                mapping.setChatLid(chatLid);
                ChatLidMapping updated = repository.save(mapping);
                log.info("🔄 Mapping atualizado com chatLid: {}", chatLid);
                return updated;
            });
    }

    /**
     * Busca mappings por telefone (para análise/debugging)
     */
    public List<ChatLidMapping> findMappingsByPhone(String phone, UUID companyId) {
        return repository.findByPhoneAndCompanyIdOrderByCreatedAtDesc(phone, companyId);
    }

    /**
     * Busca mapping por conversationId
     */
    public Optional<ChatLidMapping> findMappingByConversationId(UUID conversationId) {
        return repository.findByConversationId(conversationId);
    }

    /**
     * Verifica se chatLid já existe
     */
    public boolean existsByChatLid(String chatLid) {
        return repository.existsByChatLid(chatLid);
    }

    /**
     * Limpeza de mappings antigos (tarefa de manutenção)
     */
    @Transactional
    public int cleanupOldMappings(UUID companyId, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int deleted = repository.deleteByCompanyIdAndCreatedAtBefore(companyId, cutoffDate);
        
        if (deleted > 0) {
            log.info("🧹 Removidos {} mappings antigos da empresa {}", deleted, companyId);
        }
        
        return deleted;
    }

    /**
     * Estatísticas por empresa
     */
    public long countMappingsByCompany(UUID companyId) {
        return repository.countByCompanyId(companyId);
    }

    /**
     * Lista mappings criados por campanhas
     */
    public List<ChatLidMapping> findCampaignMappings(UUID companyId) {
        return repository.findByFromCampaignTrueAndCompanyId(companyId);
    }

    /**
     * Busca mappings de campanhas ativas com variações de telefone
     */
    public List<ChatLidMapping> findActiveCampaignMappingsWithVariations(String phone, UUID companyId) {
        List<ChatLidMapping> mappings = new ArrayList<>();
        String[] phoneVariations = phoneService.generatePhoneVariations(phone);
        
        for (String phoneVariation : phoneVariations) {
            if (phoneVariation != null) {
                List<ChatLidMapping> variationMappings = repository.findActiveCampaignMappingsByPhoneAndCompany(phoneVariation, companyId);
                mappings.addAll(variationMappings);
            }
        }
        
        // Ordenar por data de criação (mais recente primeiro)
        mappings.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return mappings;
    }

    /**
     * Busca o mapping de campanha mais recente por telefone (com variações)
     */
    public Optional<ChatLidMapping> findMostRecentActiveCampaignMapping(String phone, UUID companyId) {
        String[] phoneVariations = phoneService.generatePhoneVariations(phone);
        
        for (String phoneVariation : phoneVariations) {
        log.debug("🔗 Buscando conversa usando findMostRecentActiveCampaignMapping - phone: {}", phoneVariation);
            if (phoneVariation != null) {
                Optional<ChatLidMapping> mapping = repository.findMostRecentCampaignMappingByPhone(phoneVariation, companyId);
                if (mapping.isPresent()) {
                    return mapping;
                }
            }
        }
        
        return Optional.empty();
    }

    private void validateInput(String chatLid, String phone, UUID companyId) {
        if (chatLid == null || chatLid.trim().isEmpty()) {
            throw new IllegalArgumentException("ChatLid não pode ser null ou vazio");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone não pode ser null ou vazio");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("CompanyId não pode ser null");
        }
    }
}