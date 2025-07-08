package com.ruby.rubia_server.core.repository;

import com.ruby.rubia_server.core.base.BaseCompanyEntityRepository;
import com.ruby.rubia_server.core.entity.Campaign;
import com.ruby.rubia_server.core.enums.CampaignStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CampaignRepository extends BaseCompanyEntityRepository<Campaign> {
    
    List<Campaign> findByStatus(CampaignStatus status);
    
    List<Campaign> findByCompanyIdAndStatus(UUID companyId, CampaignStatus status);
    
    long countByCompanyIdAndStatus(UUID companyId, CampaignStatus status);
}