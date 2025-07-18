package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.entity.CampaignContact;
import com.ruby.rubia_server.core.enums.CampaignContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignContactRepository extends JpaRepository<CampaignContact, UUID> {
    
    List<CampaignContact> findByCampaignId(UUID campaignId);
    
    List<CampaignContact> findByCustomerId(UUID customerId);
    
    List<CampaignContact> findByStatus(CampaignContactStatus status);
    
    List<CampaignContact> findByCampaignIdAndStatus(UUID campaignId, CampaignContactStatus status);
    
    List<CampaignContact> findByCustomerIdAndStatus(UUID customerId, CampaignContactStatus status);
    
    long countByCampaignId(UUID campaignId);
    
    long countByCustomerId(UUID customerId);
    
    long countByCampaignIdAndStatus(UUID campaignId, CampaignContactStatus status);
    
    boolean existsByCampaignIdAndCustomerId(UUID campaignId, UUID customerId);
}