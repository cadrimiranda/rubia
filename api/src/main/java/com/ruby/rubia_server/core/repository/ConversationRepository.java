package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.enums.Channel;
import com.ruby.rubia_server.core.enums.ConversationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    
    
    
    
    // Company-scoped methods
    List<Conversation> findByStatusAndCompanyId(ConversationStatus status, UUID companyId);
    
    Page<Conversation> findByStatusAndCompanyId(ConversationStatus status, UUID companyId, Pageable pageable);
    
    List<Conversation> findByCustomerIdAndCompanyId(UUID customerId, UUID companyId);
    
    List<Conversation> findByAssignedUserIdAndCompanyId(UUID assignedUserId, UUID companyId);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = :status AND c.company.id = :companyId ORDER BY c.priority DESC, c.updatedAt DESC")
    List<Conversation> findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(@Param("status") ConversationStatus status, @Param("companyId") UUID companyId);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = :status AND c.company.id = :companyId ORDER BY c.priority DESC, c.updatedAt DESC")
    Page<Conversation> findByStatusAndCompanyOrderedByPriorityAndUpdatedAt(@Param("status") ConversationStatus status, @Param("companyId") UUID companyId, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = 'ENTRADA' AND c.assignedUser IS NULL AND c.company.id = :companyId")
    List<Conversation> findUnassignedEntranceConversationsByCompany(@Param("companyId") UUID companyId);
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = :status AND c.company.id = :companyId")
    long countByStatusAndCompany(@Param("status") ConversationStatus status, @Param("companyId") UUID companyId);
    
    List<Conversation> findByCompanyId(UUID companyId);
}