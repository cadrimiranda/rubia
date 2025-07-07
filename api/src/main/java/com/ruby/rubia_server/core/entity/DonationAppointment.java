package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.enums.DonationAppointmentStatus;
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
@Table(name = "donation_appointments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonationAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // A qual empresa este agendamento pertence

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer; // Cliente para quem o agendamento foi feito

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation; // Conversa que levou ao agendamento (opcional)

    @Column(name = "external_appointment_id", nullable = false)
    private String externalAppointmentId; // ID do agendamento no sistema RealBlood

    @Column(name = "appointment_date_time", nullable = false)
    private LocalDateTime appointmentDateTime; // Data e hora do agendamento

    @Enumerated(EnumType.STRING)
    @Column(name = "appointment_status", nullable = false)
    @Builder.Default
    private DonationAppointmentStatus status = DonationAppointmentStatus.SCHEDULED; // Status do agendamento

    @Column(name = "confirmation_url")
    private String confirmationUrl; // URL de confirmação ou detalhes do agendamento, se fornecida pela API

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // Notas adicionais sobre o agendamento

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}