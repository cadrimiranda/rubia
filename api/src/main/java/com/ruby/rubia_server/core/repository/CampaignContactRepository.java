package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignContactRepository extends JpaRepository<CampaignContact, UUID> {
    
    List<CampaignContact> findByCampaignId(UUID campaignId);
    
    List<CampaignContact> findByCustomerId(UUID customerId);
    
    List<CampaignContact> findByStatus(CampaignContactStatus status);
    
    List<CampaignContact> findByCampaignIdAndStatus(UUID campaignId, CampaignContactStatus status);
    
    List<CampaignContact> findByCustomerIdAndStatus(UUID customerId, CampaignContactStatus status);
    
    List<CampaignContact> findByCustomer_PhoneAndStatus(String customerPhone, CampaignContactStatus status);
    
    long countByCampaignId(UUID campaignId);
    
    long countByCustomerId(UUID customerId);
    
    long countByCampaignIdAndStatus(UUID campaignId, CampaignContactStatus status);
    
    boolean existsByCampaignIdAndCustomerId(UUID campaignId, UUID customerId);
    
    @Query("SELECT cc FROM CampaignContact cc JOIN FETCH cc.customer JOIN FETCH cc.campaign c LEFT JOIN FETCH c.initialMessageTemplate WHERE cc.id = :id")
    Optional<CampaignContact> findByIdWithRelations(@Param("id") UUID id);
}