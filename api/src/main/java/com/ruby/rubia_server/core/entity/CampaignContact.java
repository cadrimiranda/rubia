package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign; // A qual campanha este contato pertence

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact; // O contato importado associado a esta campanha

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer; // O cliente (se o contato foi convertido) associado à conversa da campanha

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status", nullable = false)
    @Builder.Default
    private CampaignContactStatus status = CampaignContactStatus.PENDING; // Status do contato na campanha (PENDING, SENT, FAILED, CONVERTED, OPT_OUT)

    @Column(name = "message_sent_at")
    private LocalDateTime messageSentAt; // Quando a mensagem inicial da campanha foi enviada para este contato

    @Column(name = "response_received_at")
    private LocalDateTime responseReceivedAt; // Quando a primeira resposta do contato foi recebida

    @Column(columnDefinition = "TEXT")
    private String notes; // Notas relevantes sobre este contato na campanha

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Quando o contato foi adicionado à campanha

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}