package com.ruby.rubia_server.core.service;

import com.ruby.rubia_server.core.base.BaseCompanyEntityService;
import com.ruby.rubia_server.core.base.EntityRelationshipValidator;
import com.ruby.rubia_server.core.dto.*;
import com.ruby.rubia_server.core.entity.Company;
import com.ruby.rubia_server.core.entity.FAQ;
import com.ruby.rubia_server.core.entity.User;
import com.ruby.rubia_server.core.repository.CompanyRepository;
import com.ruby.rubia_server.core.repository.FAQRepository;
import com.ruby.rubia_server.core.repository.UserRepository;
import com.ruby.rubia_server.core.util.CompanyContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class FAQService extends BaseCompanyEntityService<FAQ, CreateFAQDTO, UpdateFAQDTO> {

    private final FAQRepository faqRepository;
    private final UserRepository userRepository;
    private final CompanyContextUtil companyContextUtil;

    // Stop words for better search matching
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is", "it",
        "its", "of", "on", "that", "the", "to", "was", "will", "with", "o", "e", "os", "do", 
        "da", "de", "para", "com", "em", "um", "uma", "que", "não", "é", "eu", "você", "ele", "ela"
    );

    public FAQService(FAQRepository faqRepository,
                      CompanyRepository companyRepository,
                      UserRepository userRepository,
                      EntityRelationshipValidator relationshipValidator,
                      CompanyContextUtil companyContextUtil) {
        super(faqRepository, companyRepository, relationshipValidator);
        this.faqRepository = faqRepository;
        this.userRepository = userRepository;
        this.companyContextUtil = companyContextUtil;
    }

    @Override
    protected String getEntityName() {
        return "FAQ";
    }

    @Override
    protected FAQ buildEntityFromDTO(CreateFAQDTO createDTO) {
        User createdBy = companyContextUtil.getAuthenticatedUser();

        return FAQ.builder()
                .question(createDTO.getQuestion().trim())
                .answer(createDTO.getAnswer().trim())
                .keywords(normalizeKeywords(createDTO.getKeywords()))
                .triggers(normalizeTriggers(createDTO.getTriggers()))
                .isActive(createDTO.getIsActive())
                .usageCount(0)
                .successRate(0.0)
                .createdBy(createdBy)
                .build();
    }

    @Override
    protected void updateEntityFromDTO(FAQ existingEntity, UpdateFAQDTO updateDTO) {
        User lastEditedBy = companyContextUtil.getAuthenticatedUser();

        if (updateDTO.getQuestion() != null) {
            existingEntity.setQuestion(updateDTO.getQuestion().trim());
        }
        if (updateDTO.getAnswer() != null) {
            existingEntity.setAnswer(updateDTO.getAnswer().trim());
        }
        if (updateDTO.getKeywords() != null) {
            existingEntity.setKeywords(normalizeKeywords(updateDTO.getKeywords()));
        }
        if (updateDTO.getTriggers() != null) {
            existingEntity.setTriggers(normalizeTriggers(updateDTO.getTriggers()));
        }
        if (updateDTO.getIsActive() != null) {
            existingEntity.setIsActive(updateDTO.getIsActive());
        }
        
        existingEntity.setLastEditedBy(lastEditedBy);
    }

    @Override
    protected Company getCompanyFromDTO(CreateFAQDTO createDTO) {
        return validateAndGetCompany(createDTO.getCompanyId());
    }

    public FAQDTO mapToDTO(FAQ entity) {
        return FAQDTO.builder()
                .id(entity.getId())
                .companyId(entity.getCompany().getId())
                .question(entity.getQuestion())
                .answer(entity.getAnswer())
                .keywords(entity.getKeywords())
                .triggers(entity.getTriggers())
                .usageCount(entity.getUsageCount())
                .successRate(entity.getSuccessRate())
                .isActive(entity.getIsActive())
                .createdById(entity.getCreatedBy().getId())
                .createdByName(entity.getCreatedBy().getName())
                .lastEditedById(entity.getLastEditedBy() != null ? entity.getLastEditedBy().getId() : null)
                .lastEditedByName(entity.getLastEditedBy() != null ? entity.getLastEditedBy().getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // FAQ-specific business methods

    /**
     * Search for FAQs relevant to a user message (AI functionality)
     */
    public List<FAQMatchDTO> searchRelevantFAQs(FAQSearchDTO searchDTO) {
        if (searchDTO.getUserMessage() == null || searchDTO.getUserMessage().trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedMessage = normalizeText(searchDTO.getUserMessage());
        List<FAQMatchDTO> results = new ArrayList<>();

        // 1. Exact trigger matches (highest priority)
        List<FAQ> exactTriggerMatches = findExactTriggerMatches(searchDTO.getCompanyId(), normalizedMessage);
        for (FAQ faq : exactTriggerMatches) {
            results.add(createFAQMatch(faq, 0.95, "exact_trigger", findMatchingTrigger(faq, normalizedMessage)));
        }

        // 2. Exact keyword matches
        List<FAQ> exactKeywordMatches = findExactKeywordMatches(searchDTO.getCompanyId(), normalizedMessage);
        for (FAQ faq : exactKeywordMatches) {
            if (results.stream().noneMatch(match -> match.getFaq().getId().equals(faq.getId()))) {
                results.add(createFAQMatch(faq, 0.85, "exact_keyword", findMatchingKeyword(faq, normalizedMessage)));
            }
        }

        // 3. Partial matches in question/answer
        List<FAQ> partialMatches = faqRepository.findRelevantFAQs(searchDTO.getCompanyId(), normalizedMessage);
        for (FAQ faq : partialMatches) {
            if (results.stream().noneMatch(match -> match.getFaq().getId().equals(faq.getId()))) {
                double score = calculatePartialMatchScore(faq, normalizedMessage);
                if (score >= (searchDTO.getMinConfidenceScore() != null ? searchDTO.getMinConfidenceScore() : 0.3)) {
                    results.add(createFAQMatch(faq, score, "partial_match", normalizedMessage));
                }
            }
        }

        // Sort by confidence score and apply limit
        results.sort((a, b) -> Double.compare(b.getConfidenceScore(), a.getConfidenceScore()));
        
        if (searchDTO.getLimit() != null && searchDTO.getLimit() > 0) {
            results = results.subList(0, Math.min(results.size(), searchDTO.getLimit()));
        }

        return results;
    }

    /**
     * Get FAQ statistics for a company
     */
    @Transactional(readOnly = true)
    public FAQStatsDTO getCompanyFAQStats(UUID companyId) {
        long totalFAQs = faqRepository.countByCompanyIdAndNotDeleted(companyId);
        long activeFAQs = faqRepository.countByCompanyIdAndIsActiveAndNotDeleted(companyId, true);
        long inactiveFAQs = faqRepository.countByCompanyIdAndIsActiveAndNotDeleted(companyId, false);
        
        Double avgSuccessRate = faqRepository.getAverageSuccessRateByCompanyId(companyId);
        Long totalUsage = faqRepository.getTotalUsageCountByCompanyId(companyId);

        List<FAQ> topPerforming = faqRepository.findTopPerformingFAQs(companyId);
        List<FAQ> mostUsed = faqRepository.findMostUsedFAQs(companyId);

        return FAQStatsDTO.builder()
                .companyId(companyId)
                .totalFAQs(totalFAQs)
                .activeFAQs(activeFAQs)
                .inactiveFAQs(inactiveFAQs)
                .averageSuccessRate(avgSuccessRate != null ? avgSuccessRate : 0.0)
                .totalUsageCount(totalUsage != null ? totalUsage : 0L)
                .topPerformingFAQ(!topPerforming.isEmpty() ? mapToDTO(topPerforming.get(0)) : null)
                .mostUsedFAQ(!mostUsed.isEmpty() ? mapToDTO(mostUsed.get(0)) : null)
                .build();
    }

    /**
     * Record FAQ usage for AI learning
     */
    public void recordFAQUsage(UUID faqId, String userMessage, boolean wasApproved) {
        FAQ faq = faqRepository.findByIdAndNotDeleted(faqId)
            .orElseThrow(() -> new RuntimeException("FAQ not found: " + faqId));

        faq.incrementUsageCount();
        // Simple success rate calculation - could be enhanced with more sophisticated tracking
        int totalUsages = faq.getUsageCount();
        faq.updateSuccessRate(wasApproved, totalUsages);
        
        faqRepository.save(faq);
        
        log.info("Recorded FAQ usage - ID: {}, approved: {}, new usage count: {}, success rate: {}%", 
            faqId, wasApproved, totalUsages, faq.getSuccessRate());
    }

    /**
     * Get paginated FAQs with search
     */
    @Transactional(readOnly = true)
    public Page<FAQDTO> searchFAQs(UUID companyId, String searchTerm, Boolean isActive, Pageable pageable) {
        // For now, we'll implement a simple version that doesn't do complex search with pagination
        // This can be enhanced later with Spring Data specifications or custom queries
        
        List<FAQ> allFAQs;
        
        if (isActive != null) {
            allFAQs = faqRepository.findByCompanyIdAndIsActiveAndNotDeleted(companyId, isActive);
        } else {
            allFAQs = faqRepository.findByCompanyIdAndNotDeleted(companyId);
        }
        
        // Apply search filter if provided
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String normalizedSearch = searchTerm.toLowerCase().trim();
            allFAQs = allFAQs.stream()
                    .filter(faq -> 
                        faq.getQuestion().toLowerCase().contains(normalizedSearch) ||
                        faq.getAnswer().toLowerCase().contains(normalizedSearch) ||
                        faq.getKeywords().stream().anyMatch(keyword -> keyword.toLowerCase().contains(normalizedSearch)) ||
                        faq.getTriggers().stream().anyMatch(trigger -> trigger.toLowerCase().contains(normalizedSearch))
                    )
                    .collect(Collectors.toList());
        }
        
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            allFAQs.sort((faq1, faq2) -> {
                for (Sort.Order order : pageable.getSort()) {
                    int comparison = 0;
                    switch (order.getProperty()) {
                        case "createdAt":
                            comparison = faq1.getCreatedAt().compareTo(faq2.getCreatedAt());
                            break;
                        case "question":
                            comparison = faq1.getQuestion().compareToIgnoreCase(faq2.getQuestion());
                            break;
                        case "usageCount":
                            comparison = Integer.compare(faq1.getUsageCount(), faq2.getUsageCount());
                            break;
                        case "successRate":
                            comparison = Double.compare(faq1.getSuccessRate(), faq2.getSuccessRate());
                            break;
                        default:
                            comparison = 0;
                    }
                    
                    if (comparison != 0) {
                        return order.isAscending() ? comparison : -comparison;
                    }
                }
                return 0;
            });
        }
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allFAQs.size());
        List<FAQ> pageContent = allFAQs.subList(start, end);
        
        List<FAQDTO> dtoContent = pageContent.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtoContent, pageable, allFAQs.size());
    }

    /**
     * Soft delete FAQ
     */
    public void softDelete(UUID id) {
        FAQ faq = faqRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("FAQ not found: " + id));
        faq.markAsDeleted();
        faqRepository.save(faq);
        log.info("Soft deleted FAQ: {}", id);
    }

    /**
     * Restore soft deleted FAQ
     */
    public FAQDTO restore(UUID id) {
        FAQ faq = faqRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("FAQ not found: " + id));
        
        faq.restore();
        FAQ saved = faqRepository.save(faq);
        log.info("Restored FAQ: {}", id);
        
        return mapToDTO(saved);
    }

    /**
     * Get deleted FAQs for a company
     */
    @Transactional(readOnly = true)
    public List<FAQDTO> getDeletedFAQs(UUID companyId) {
        List<FAQ> deletedFAQs = faqRepository.findDeletedByCompanyId(companyId);
        return deletedFAQs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Helper methods

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) return new ArrayList<>();
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(keyword -> !keyword.isEmpty() && !STOP_WORDS.contains(keyword))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> normalizeTriggers(List<String> triggers) {
        if (triggers == null) return new ArrayList<>();
        return triggers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(trigger -> !trigger.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeText(String text) {
        return text.toLowerCase().trim();
    }

    private List<FAQ> findExactTriggerMatches(UUID companyId, String message) {
        List<FAQ> allFAQs = faqRepository.findByCompanyIdAndIsActiveAndNotDeleted(companyId, true);
        return allFAQs.stream()
                .filter(faq -> faq.getTriggers().stream()
                        .anyMatch(trigger -> message.contains(trigger.toLowerCase())))
                .collect(Collectors.toList());
    }

    private List<FAQ> findExactKeywordMatches(UUID companyId, String message) {
        Set<String> messageWords = Arrays.stream(message.split("\\s+"))
                .map(String::toLowerCase)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toSet());

        List<FAQ> allFAQs = faqRepository.findByCompanyIdAndIsActiveAndNotDeleted(companyId, true);
        return allFAQs.stream()
                .filter(faq -> faq.getKeywords().stream()
                        .anyMatch(messageWords::contains))
                .collect(Collectors.toList());
    }

    private FAQMatchDTO createFAQMatch(FAQ faq, double score, String reason, String matchedText) {
        return FAQMatchDTO.builder()
                .faq(mapToDTO(faq))
                .confidenceScore(score)
                .matchReason(reason)
                .matchedText(matchedText)
                .build();
    }

    private String findMatchingTrigger(FAQ faq, String message) {
        return faq.getTriggers().stream()
                .filter(trigger -> message.contains(trigger.toLowerCase()))
                .findFirst()
                .orElse("");
    }

    private String findMatchingKeyword(FAQ faq, String message) {
        Set<String> messageWords = Arrays.stream(message.split("\\s+"))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        return faq.getKeywords().stream()
                .filter(messageWords::contains)
                .findFirst()
                .orElse("");
    }

    private double calculatePartialMatchScore(FAQ faq, String message) {
        int matches = 0;
        int totalWords = 0;
        
        // Check question similarity
        String[] questionWords = faq.getQuestion().toLowerCase().split("\\s+");
        String[] messageWords = message.split("\\s+");
        
        for (String qWord : questionWords) {
            if (!STOP_WORDS.contains(qWord)) {
                totalWords++;
                for (String mWord : messageWords) {
                    if (qWord.contains(mWord) || mWord.contains(qWord)) {
                        matches++;
                        break;
                    }
                }
            }
        }
        
        return totalWords > 0 ? (double) matches / totalWords : 0.0;
    }
}