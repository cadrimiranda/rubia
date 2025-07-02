package com.ruby.rubia_server.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // A qual empresa este contato pertence

    @Column(nullable = false, length = 20)
    private String phone; // Número de telefone do contato

    private String name;

    @Column(name = "whatsapp_id")
    private String whatsappId; // Se disponível, o ID do WhatsApp

    @Column(name = "profile_url")
    private String profileUrl; // URL da foto de perfil, se disponível

    @Column(name = "source_system_name", nullable = false)
    private String sourceSystemName; // Nome do sistema de origem (Ex: "CRM Externo", "Planilha Excel", "Marketing App")

    @Column(name = "source_system_id")
    private String sourceSystemId; // ID original do contato no sistema de origem (se houver)

    // Novos campos para contexto de doação/informações adicionais
    @Column(name = "birth_date")
    private LocalDate birthDate; // Data de nascimento do contato

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate; // Última data de doação (para doadores)

    @Column(name = "next_eligible_donation_date")
    private LocalDate nextEligibleDonationDate; // Próxima data elegível para doação

    @CreationTimestamp
    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt; // Quando o contato foi importado

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true)
    private Customer customer;
}