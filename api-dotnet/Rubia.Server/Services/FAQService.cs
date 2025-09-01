using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class FAQService : IFAQService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<FAQService> _logger;

    public FAQService(RubiaDbContext context, ILogger<FAQService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<IEnumerable<FAQDto>> GetAllAsync(Guid companyId)
    {
        var faqs = await _context.FAQs
            .Where(f => f.CompanyId == companyId && f.IsActive)
            .OrderByDescending(f => f.CreatedAt)
            .ToListAsync();

        return faqs.Select(MapToDto);
    }

    public async Task<FAQDto?> GetByIdAsync(Guid faqId)
    {
        var faq = await _context.FAQs
            .FirstOrDefaultAsync(f => f.Id == faqId);

        return faq != null ? MapToDto(faq) : null;
    }

    public async Task<FAQDto> CreateAsync(CreateFAQDto dto)
    {
        var faq = new FAQ
        {
            Id = Guid.NewGuid(),
            CompanyId = dto.CompanyId,
            Question = dto.Question,
            Answer = dto.Answer,
            Keywords = dto.Keywords,
            IsActive = true,
            UsageCount = 0,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.FAQs.Add(faq);
        await _context.SaveChangesAsync();

        return MapToDto(faq);
    }

    public async Task<FAQDto?> UpdateAsync(Guid faqId, UpdateFAQDto dto)
    {
        var faq = await _context.FAQs.FindAsync(faqId);
        if (faq == null)
            return null;

        faq.Question = dto.Question ?? faq.Question;
        faq.Answer = dto.Answer ?? faq.Answer;
        faq.Keywords = dto.Keywords ?? faq.Keywords;
        faq.IsActive = dto.IsActive ?? faq.IsActive;
        faq.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return MapToDto(faq);
    }

    public async Task<bool> DeleteAsync(Guid faqId)
    {
        var faq = await _context.FAQs.FindAsync(faqId);
        if (faq == null)
            return false;

        faq.IsActive = false;
        faq.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<IEnumerable<FAQMatchDto>> SearchAsync(Guid companyId, string query)
    {
        var faqs = await _context.FAQs
            .Where(f => f.CompanyId == companyId && f.IsActive)
            .Where(f => EF.Functions.ILike(f.Question, $"%{query}%") || 
                       EF.Functions.ILike(f.Keywords, $"%{query}%") ||
                       EF.Functions.ILike(f.Answer, $"%{query}%"))
            .OrderByDescending(f => f.UsageCount)
            .Take(10)
            .ToListAsync();

        return faqs.Select(f => new FAQMatchDto
        {
            Id = f.Id,
            Question = f.Question,
            Answer = f.Answer,
            MatchScore = CalculateMatchScore(f, query),
            UsageCount = f.UsageCount
        });
    }

    public async Task IncrementUsageAsync(Guid faqId)
    {
        var faq = await _context.FAQs.FindAsync(faqId);
        if (faq != null)
        {
            faq.UsageCount++;
            faq.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }
    }

    public async Task<FAQStatsDto> GetStatsAsync(Guid companyId)
    {
        var totalFAQs = await _context.FAQs.CountAsync(f => f.CompanyId == companyId && f.IsActive);
        var totalUsage = await _context.FAQs
            .Where(f => f.CompanyId == companyId && f.IsActive)
            .SumAsync(f => f.UsageCount);

        var topFAQs = await _context.FAQs
            .Where(f => f.CompanyId == companyId && f.IsActive)
            .OrderByDescending(f => f.UsageCount)
            .Take(5)
            .Select(f => new { f.Question, f.UsageCount })
            .ToListAsync();

        return new FAQStatsDto
        {
            TotalFAQs = totalFAQs,
            TotalUsage = totalUsage,
            TopFAQs = topFAQs.Select(t => $"{t.Question} ({t.UsageCount} usos)").ToList()
        };
    }

    private static double CalculateMatchScore(FAQ faq, string query)
    {
        var score = 0.0;
        var queryLower = query.ToLowerInvariant();
        
        if (faq.Question.ToLowerInvariant().Contains(queryLower))
            score += 0.5;
        
        if (faq.Keywords?.ToLowerInvariant().Contains(queryLower) == true)
            score += 0.3;
        
        if (faq.Answer.ToLowerInvariant().Contains(queryLower))
            score += 0.2;

        // Bonus for usage frequency
        score += Math.Min(faq.UsageCount * 0.01, 0.2);

        return score;
    }

    private static FAQDto MapToDto(FAQ faq)
    {
        return new FAQDto
        {
            Id = faq.Id,
            CompanyId = faq.CompanyId,
            Question = faq.Question,
            Answer = faq.Answer,
            Keywords = faq.Keywords,
            UsageCount = faq.UsageCount,
            SuccessRate = faq.SuccessRate,
            IsActive = faq.IsActive,
            Priority = faq.Priority,
            CreatedAt = faq.CreatedAt,
            UpdatedAt = faq.UpdatedAt
        };
    }
}