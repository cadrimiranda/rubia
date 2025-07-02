// src/main/java/com/ruby/rubia_server/core/entity/MessageStatusDetail.java
package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.enums.MessageStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_status_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatusDetail {

    @Id
    @Column(name = "message_id") // A coluna da chave primária é renomeada para message_id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Isso mapeia o ID desta entidade para a chave estrangeira message_id
    @JoinColumn(name = "message_id")
    private Message message; // Referência à mensagem principal

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT; // O status atual da mensagem

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt; // Timestamp de quando a mensagem foi entregue

    @Column(name = "read_at")
    private LocalDateTime readAt; // Timestamp de quando a mensagem foi lida

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Última atualização deste registro de status
}