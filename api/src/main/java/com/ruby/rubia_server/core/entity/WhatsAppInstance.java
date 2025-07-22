package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.enums.MessagingProvider;
import com.ruby.rubia_server.core.enums.WhatsAppInstanceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"company"})
@EqualsAndHashCode(exclude = {"company"})
public class WhatsAppInstance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    
    @Column(name = "display_name")
    private String displayName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @Builder.Default
    private MessagingProvider provider = MessagingProvider.Z_API;
    
    @Column(name = "instance_id")
    private String instanceId;
    
    @Column(name = "access_token")
    private String accessToken;
    
    @Column(name = "webhook_url")
    private String webhookUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private WhatsAppInstanceStatus status = WhatsAppInstanceStatus.NOT_CONFIGURED;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;
    
    @Column(name = "last_status_check")
    private LocalDateTime lastStatusCheck;
    
    @Column(name = "configuration_data", columnDefinition = "TEXT")
    private String configurationData;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isConfigured() {
        return status != WhatsAppInstanceStatus.NOT_CONFIGURED;
    }
    
    public boolean isConnected() {
        return status == WhatsAppInstanceStatus.CONNECTED;
    }
    
    public boolean needsConfiguration() {
        return status == WhatsAppInstanceStatus.NOT_CONFIGURED || 
               status == WhatsAppInstanceStatus.DISCONNECTED ||
               status == WhatsAppInstanceStatus.ERROR;
    }
}