package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "campaign_contacts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignContact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false) // Era 'contact_id'
    private Customer customer; // O cliente (agora a entidade consolidada) associado a esta campanha

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_status", nullable = false)
    @Builder.Default
    private CampaignContactStatus status = CampaignContactStatus.PENDING;

    @Column(name = "message_sent_at")
    private LocalDateTime messageSentAt;

    @Column(name = "response_received_at")
    private LocalDateTime responseReceivedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}