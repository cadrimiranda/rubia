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
@Table(name = "customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 20)
    private String phone; // Número de telefone do contato/cliente

    private String name; // Nome do contato/cliente

    @Column(name = "whatsapp_id")
    private String whatsappId; // Se disponível, o ID do WhatsApp

    @Column(name = "profile_url")
    private String profileUrl; // URL da foto de perfil, se disponível

    @Column(name = "is_blocked")
    @Builder.Default
    private Boolean isBlocked = false; // Indica se o cliente está bloqueado para comunicação

    // CAMPOS INTEGRADOS DA ANTIGA ENTIDADE 'CONTACT'
    @Column(name = "source_system_name")
    private String sourceSystemName; // Nome do sistema de origem (Ex: "CRM Externo", "Planilha Excel", "Marketing App", "Organic")

    @Column(name = "source_system_id")
    private String sourceSystemId; // ID original do contato no sistema de origem (se houver)

    @Column(name = "imported_at", updatable = false)
    private LocalDateTime importedAt; // Quando o registro foi criado/importado (era o CreationTimestamp de Contact)

    @Column(name = "birth_date")
    private LocalDate birthDate; // Data de nascimento do cliente

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate; // Última data de doação (para doadores)

    @Column(name = "next_eligible_donation_date")
    private LocalDate nextEligibleDonationDate; // Próxima data elegível para doação

    @Column(name = "blood_type", length = 10)
    private String bloodType; // Tipo sanguíneo (A+, B+, AB+, O+, A-, B-, AB-, O-)

    @Column(name = "height")
    private Integer height; // Altura em centímetros

    @Column(name = "weight")
    private Double weight; // Peso em quilogramas

    // CAMPOS DE ENDEREÇO
    @Column(name = "address_street")
    private String addressStreet; // Rua/Avenida

    @Column(name = "address_number")
    private String addressNumber; // Número

    @Column(name = "address_complement")
    private String addressComplement; // Complemento (apartamento, bloco, etc.)

    @Column(name = "address_postal_code", length = 10)
    private String addressPostalCode; // CEP

    @Column(name = "address_city")
    private String addressCity; // Cidade

    @Column(name = "address_state", length = 20)
    private String addressState; // Estado (UF)

    // CAMPOS ADICIONAIS PARA CAMPANHA
    @Column(name = "email")
    private String email; // Email do cliente
    
    @Column(name = "cpf", length = 11)
    private String cpf; // CPF do cliente
    
    @Column(name = "rg", length = 20)
    private String rg; // RG do cliente
    
    @Column(name = "rh_factor", length = 3)
    private String rhFactor; // Fator RH (+, -)

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // Timestamp de criação do registro (para clientes orgânicos)

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}