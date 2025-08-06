package com.ruby.rubia_server.core.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ruby.rubia_server.core.enums.MessagingProvider;
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
    @JsonBackReference("company-whatsapp")
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
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
    
    @Column(name = "configuration_data", columnDefinition = "TEXT")
    private String configurationData;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Helper methods
    public boolean isConfigured() {
        return instanceId != null && accessToken != null;
    }
    
    public boolean isConnected() {
        return isConfigured() && isActive;
    }
    
    public boolean needsConfiguration() {
        return !isConfigured();
    }
}