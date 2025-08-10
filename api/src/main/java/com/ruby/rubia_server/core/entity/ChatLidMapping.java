package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapeamento entre chatLid do WhatsApp e conversas internas
 * Resolve problema de conversas de campanha que não possuem chatLid inicial
 */
@Entity
@Table(name = "chat_lid_mappings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatLidMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    /**
     * ChatLid recebido do WhatsApp (ex: "269161355821173@lid")
     */
    @Column(name = "chat_lid", nullable = false, unique = true, length = 100)
    private String chatLid;

    /**
     * ID da conversa interna associada
     */
    @Column(name = "conversation_id", nullable = false, columnDefinition = "uuid")
    private UUID conversationId;

    /**
     * Telefone do cliente (para busca rápida)
     */
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    /**
     * ID da empresa (para isolamento multi-tenant)
     */
    @Column(name = "company_id", nullable = false, columnDefinition = "uuid")
    private UUID companyId;

    /**
     * Instância WhatsApp que criou o mapping
     */
    @Column(name = "whatsapp_instance_id", columnDefinition = "uuid")
    private UUID whatsappInstanceId;

    /**
     * Indica se mapping foi criado por campanha (sem chatLid inicial)
     */
    @Column(name = "from_campaign", nullable = false)
    @Builder.Default
    private Boolean fromCampaign = false;

    /**
     * ID da campanha que originou este mapping (nullable para mappings não-campanha)
     */
    @Column(name = "campaign_id", columnDefinition = "uuid")
    private UUID campaignId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}