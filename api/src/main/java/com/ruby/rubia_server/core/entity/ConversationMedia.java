package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.base.BaseEntity;
import com.ruby.rubia_server.core.enums.MediaType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "conversation_media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMedia implements BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company; // Para multi-tenancy

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation; // A conversa à qual esta mídia pertence

    @Column(name = "file_url", nullable = false)
    private String fileUrl; // A URL/caminho para o arquivo armazenado (ex: no S3, GCS)

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType; // Tipo de mídia (ex: IMAGE, VIDEO, AUDIO, DOCUMENT)

    @Column(name = "mime_type", length = 100)
    private String mimeType; // Tipo MIME do arquivo (ex: "image/jpeg", "application/pdf")

    @Column(name = "original_file_name", length = 255)
    private String originalFileName; // Nome original do arquivo antes de ser salvo no storage

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes; // Tamanho do arquivo em bytes

    @Column(name = "checksum", length = 64) // Opcional: para verificação de integridade (ex: SHA-256)
    private String checksum;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt; // Quando a mídia foi armazenada/registrada

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Opcional: Quem enviou/recebeu este arquivo originalmente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_customer_id")
    private Customer uploadedByCustomer;
}