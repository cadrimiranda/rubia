-- Ativa todas as campanhas que estão em status DRAFT
UPDATE campaigns 
SET campaign_status = 'ACTIVE' 
WHERE campaign_status = 'DRAFT';