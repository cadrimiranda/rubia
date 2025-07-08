package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.ConversationMedia;
import com.ruby.rubia_server.core.enums.MediaType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationMediaRepository extends BaseCompanyEntityRepository<ConversationMedia> {
    
    List<ConversationMedia> findByConversationId(UUID conversationId);
    
    List<ConversationMedia> findByMediaType(MediaType mediaType);
    
    List<ConversationMedia> findByCompanyIdAndMediaType(UUID companyId, MediaType mediaType);
    
    List<ConversationMedia> findByConversationIdAndMediaType(UUID conversationId, MediaType mediaType);
    
    List<ConversationMedia> findByUploadedByUserId(UUID uploadedByUserId);
    
    List<ConversationMedia> findByUploadedByCustomerId(UUID uploadedByCustomerId);
    
    long countByConversationId(UUID conversationId);
    
    long countByMediaType(MediaType mediaType);
    
    long countByCompanyIdAndMediaType(UUID companyId, MediaType mediaType);
    
    @Query("SELECT COALESCE(SUM(cm.fileSizeBytes), 0) FROM ConversationMedia cm WHERE cm.company.id = :companyId")
    Long getTotalFileSizeByCompanyId(@Param("companyId") UUID companyId);
    
    @Query("SELECT COALESCE(SUM(cm.fileSizeBytes), 0) FROM ConversationMedia cm WHERE cm.conversation.id = :conversationId")
    Long getTotalFileSizeByConversationId(@Param("conversationId") UUID conversationId);
    
    @Query("SELECT COALESCE(SUM(cm.fileSizeBytes), 0) FROM ConversationMedia cm WHERE cm.mediaType = :mediaType")
    Long getTotalFileSizeByMediaType(@Param("mediaType") MediaType mediaType);
}