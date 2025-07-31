package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.AudioMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AudioMessageRepository extends JpaRepository<AudioMessage, UUID> {
    
    Optional<AudioMessage> findByMessageId(String messageId);
    
    List<AudioMessage> findByStatus(AudioMessage.ProcessingStatus status);
    
    List<AudioMessage> findByFromNumber(String fromNumber);
    
    List<AudioMessage> findByFromNumberOrderByCreatedAtDesc(String fromNumber);
    
    List<AudioMessage> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByMessageId(String messageId);
    
    @Query("SELECT a FROM AudioMessage a WHERE a.status = :status ORDER BY a.createdAt ASC")
    List<AudioMessage> findPendingAudioMessages(@Param("status") AudioMessage.ProcessingStatus status);
    
    @Query("SELECT a FROM AudioMessage a WHERE a.direction = :direction ORDER BY a.createdAt DESC")
    List<AudioMessage> findByDirection(@Param("direction") AudioMessage.MessageDirection direction);
    
    @Query("SELECT COUNT(a) FROM AudioMessage a WHERE a.status = :status")
    long countByStatus(@Param("status") AudioMessage.ProcessingStatus status);
}