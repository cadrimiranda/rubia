package com.ruby.rubia_server.core.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ruby.rubia_server.core.enums.CompanyPlanType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"departments"})
@EqualsAndHashCode(exclude = {"departments"})
public class Company {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String slug;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "logo_url")
    private String logoUrl;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "plan_type")
    @Builder.Default
    private CompanyPlanType planType = CompanyPlanType.BASIC;
    
    @Column(name = "max_users")
    @Builder.Default
    private Integer maxUsers = 10;
    
    @Column(name = "max_whatsapp_numbers")
    @Builder.Default
    private Integer maxWhatsappNumbers = 1;
    
    @Column(name = "max_ai_agents")
    @Builder.Default
    private Integer maxAiAgents = 1;
    
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("company-departments")
    private List<Department> departments;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("company-whatsapp")
    private List<WhatsAppInstance> whatsappInstances;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_group_id", nullable = false)
    @JsonBackReference("companygroup-companies")
    private CompanyGroup companyGroup;
}