package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.ChatLidMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatLidMappingRepository extends JpaRepository<ChatLidMapping, UUID> {

    /**
     * Busca mapping por chatLid (usado no webhook)
     */
    Optional<ChatLidMapping> findByChatLid(String chatLid);

    /**
     * Busca mapping por conversationId (verificar se já existe mapping)
     */
    Optional<ChatLidMapping> findByConversationId(UUID conversationId);

    /**
     * Busca mappings por telefone e empresa (ordenados por data)
     */
    List<ChatLidMapping> findByPhoneAndCompanyIdOrderByCreatedAtDesc(String phone, UUID companyId);

    /**
     * Busca o mapping mais recente por telefone e empresa
     */
    @Query("SELECT m FROM ChatLidMapping m WHERE m.phone = :phone AND m.companyId = :companyId ORDER BY m.createdAt DESC LIMIT 1")
    Optional<ChatLidMapping> findMostRecentByPhoneAndCompanyId(@Param("phone") String phone, @Param("companyId") UUID companyId);

    /**
     * Verifica se chatLid já existe
     */
    boolean existsByChatLid(String chatLid);

    /**
     * Remove mappings antigos para limpeza (opcional)
     */
    @Modifying
    @Query("DELETE FROM ChatLidMapping m WHERE m.companyId = :companyId AND m.createdAt < :before")
    int deleteByCompanyIdAndCreatedAtBefore(@Param("companyId") UUID companyId, @Param("before") LocalDateTime before);

    /**
     * Busca mappings por instância WhatsApp (para debugging)
     */
    List<ChatLidMapping> findByWhatsappInstanceId(UUID whatsappInstanceId);

    /**
     * Conta mappings por empresa (para métricas)
     */
    long countByCompanyId(UUID companyId);

    /**
     * Busca mappings criados por campanhas
     */
    List<ChatLidMapping> findByFromCampaignTrueAndCompanyId(UUID companyId);

    /**
     * Busca mappings de campanhas ativas por telefone, ordenados por data (mais recente primeiro)
     */
    @Query("SELECT m FROM ChatLidMapping m WHERE m.phone = :phone AND m.companyId = :companyId AND m.fromCampaign = true AND m.campaignId IS NOT NULL ORDER BY m.createdAt DESC")
    List<ChatLidMapping> findActiveCampaignMappingsByPhoneAndCompany(@Param("phone") String phone, @Param("companyId") UUID companyId);

    /**
     * Busca o mapping de campanha mais recente por telefone
     */
    @Query("SELECT m FROM ChatLidMapping m WHERE m.phone = :phone AND m.companyId = :companyId AND m.fromCampaign = true AND m.campaignId IS NOT NULL ORDER BY m.createdAt DESC LIMIT 1")
    Optional<ChatLidMapping> findMostRecentCampaignMappingByPhone(@Param("phone") String phone, @Param("companyId") UUID companyId);
}