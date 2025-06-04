package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.Conversation;
import com.ruby.rubia_server.core.enums.ConversationChannel;
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
    
    List<Conversation> findByStatus(ConversationStatus status);
    
    Page<Conversation> findByStatus(ConversationStatus status, Pageable pageable);
    
    List<Conversation> findByCustomerId(UUID customerId);
    
    List<Conversation> findByAssignedUserId(UUID assignedUserId);
    
    List<Conversation> findByDepartmentId(UUID departmentId);
    
    List<Conversation> findByIsPinnedTrue();
    
    List<Conversation> findByChannel(ConversationChannel channel);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = :status ORDER BY c.isPinned DESC, c.priority DESC, c.updatedAt DESC")
    List<Conversation> findByStatusOrderedByPriorityAndUpdatedAt(@Param("status") ConversationStatus status);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = :status ORDER BY c.isPinned DESC, c.priority DESC, c.updatedAt DESC")
    Page<Conversation> findByStatusOrderedByPriorityAndUpdatedAt(@Param("status") ConversationStatus status, Pageable pageable);
    
    @Query("SELECT c FROM Conversation c WHERE c.assignedUser.id = :userId AND c.status IN :statuses")
    List<Conversation> findByAssignedUserAndStatuses(@Param("userId") UUID userId, @Param("statuses") List<ConversationStatus> statuses);
    
    @Query("SELECT c FROM Conversation c WHERE c.department.id = :departmentId AND c.status = :status")
    List<Conversation> findByDepartmentAndStatus(@Param("departmentId") UUID departmentId, @Param("status") ConversationStatus status);
    
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId AND c.status != 'FINALIZADOS' ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveConversationsByCustomer(@Param("customerId") UUID customerId);
    
    @Query("SELECT c FROM Conversation c WHERE c.customer.id = :customerId ORDER BY c.updatedAt DESC")
    List<Conversation> findConversationsByCustomerOrderedByUpdatedAt(@Param("customerId") UUID customerId);
    
    @Query("SELECT c FROM Conversation c WHERE c.status = 'ENTRADA' AND c.assignedUser IS NULL")
    List<Conversation> findUnassignedEntranceConversations();
    
    @Query("SELECT c FROM Conversation c WHERE c.updatedAt < :cutoffDate AND c.status = 'FINALIZADOS'")
    List<Conversation> findOldFinalizedConversations(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.status = :status")
    long countByStatus(@Param("status") ConversationStatus status);
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.assignedUser.id = :userId AND c.status IN ('ENTRADA', 'ESPERANDO')")
    long countActiveConversationsByUser(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.department.id = :departmentId AND c.status = :status")
    long countByDepartmentAndStatus(@Param("departmentId") UUID departmentId, @Param("status") ConversationStatus status);
}