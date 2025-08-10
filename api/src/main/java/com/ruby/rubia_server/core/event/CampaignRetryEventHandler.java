package com.ruby.rubia_server.core.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.service.CampaignQueueProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Event handler for campaign retry events
 * Adds items back to the queue for retry processing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignRetryEventHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String QUEUE_KEY = "rubia:campaign:queue";

    @EventListener
    public void handleCampaignRetryEvent(CampaignRetryEvent event) {
        log.debug("🔄 Processing campaign retry event: campaignId={}, contactId={}, companyId={}",
                event.getCampaignId(), event.getContactId(), event.getCompanyId());
        
        try {
            // Create queue item for retry
            CampaignQueueProcessor.CampaignQueueItem retryItem = new CampaignQueueProcessor.CampaignQueueItem(
                event.getCampaignId(),
                event.getContactId(),
                event.getCompanyId()
            );
            
            // Add to Redis queue with immediate processing timestamp
            String itemJson = objectMapper.writeValueAsString(retryItem);
            long currentTimestamp = System.currentTimeMillis();
            redisTemplate.opsForZSet().add(QUEUE_KEY, itemJson, currentTimestamp);
            
            log.info("✅ Retry item added to queue: contactId={}", event.getContactId());
            
        } catch (Exception e) {
            log.error("❌ Failed to handle campaign retry event: {}", e.getMessage(), e);
        }
    }
}