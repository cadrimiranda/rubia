package com.ruby.rubia_server.core.entity;

import com.ruby.rubia_server.core.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "faqs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FAQ implements BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    @ElementCollection
    @CollectionTable(name = "faq_keywords", joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "keyword", length = 100)
    @Builder.Default
    private List<String> keywords = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "faq_triggers", joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "trigger_phrase", length = 200)
    @Builder.Default
    private List<String> triggers = new ArrayList<>();

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "success_rate")
    @Builder.Default
    private Double successRate = 0.0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by_user_id")
    private User lastEditedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Helper methods
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    public void updateSuccessRate(boolean wasSuccessful, int totalUsages) {
        if (totalUsages <= 0) {
            this.successRate = 0.0;
            return;
        }
        
        // Recalculate success rate based on new data
        // This would typically be done with more sophisticated tracking
        // For now, we'll use a simple approach
        double currentSuccesses = (this.successRate / 100.0) * (totalUsages - 1);
        if (wasSuccessful) {
            currentSuccesses += 1;
        }
        this.successRate = (currentSuccesses / totalUsages) * 100.0;
    }

    // Soft delete
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }

    public void restore() {
        this.deletedAt = null;
        this.isActive = true;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}