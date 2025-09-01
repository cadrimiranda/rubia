using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.Entities;

namespace Rubia.Server.Services;

public class MessageEnhancementAuditService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<MessageEnhancementAuditService> _logger;

    public MessageEnhancementAuditService(
        RubiaDbContext context,
        ILogger<MessageEnhancementAuditService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<MessageEnhancementAudit> RecordSuccessfulEnhancementAsync(
        Company company,
        User user,
        AIAgent aiAgent,
        string originalMessage,
        string enhancedMessage,
        Guid? conversationId,
        int? tokensConsumed,
        long? responseTimeMs,
        string? userAgent,
        string? ipAddress,
        string? openaiSystemMessage,
        string? openaiUserMessage,
        string? openaiFullPayload)
    {
        _logger.LogDebug("Recording successful message enhancement for user: {UserId} with agent: {AgentName}",
            user?.Id, aiAgent.Name);

        var audit = new MessageEnhancementAudit
        {
            Company = company,
            CompanyId = company.Id,
            User = user,
            UserId = user.Id,
            AiAgent = aiAgent,
            AiAgentId = aiAgent.Id,
            ConversationId = conversationId,
            OriginalMessage = originalMessage,
            EnhancedMessage = enhancedMessage,
            TemperamentUsed = aiAgent.Temperament ?? string.Empty,
            AiModelUsed = aiAgent.AiModel?.Name ?? "unknown",
            TemperatureUsed = (double?)aiAgent.Temperature ?? 0.7,
            MaxTokensUsed = aiAgent.MaxResponseLength,
            TokensConsumed = tokensConsumed,
            ResponseTimeMs = responseTimeMs,
            Success = true,
            UserAgent = userAgent,
            IpAddress = ipAddress,
            OpenaiSystemMessage = openaiSystemMessage,
            OpenaiUserMessage = openaiUserMessage,
            OpenaiFullPayload = openaiFullPayload
        };

        _context.MessageEnhancementAudits.Add(audit);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Message enhancement audit recorded successfully with ID: {AuditId}", audit.Id);
        return audit;
    }

    public async Task<MessageEnhancementAudit> RecordFailedEnhancementAsync(
        Company company,
        User user,
        AIAgent aiAgent,
        string originalMessage,
        string errorMessage,
        Guid? conversationId,
        long? responseTimeMs,
        string? userAgent,
        string? ipAddress,
        string? openaiSystemMessage,
        string? openaiUserMessage,
        string? openaiFullPayload)
    {
        _logger.LogDebug("Recording failed message enhancement for user: {UserId} with agent: {AgentName}",
            user?.Id, aiAgent.Name);

        var audit = new MessageEnhancementAudit
        {
            Company = company,
            CompanyId = company.Id,
            User = user,
            UserId = user.Id,
            AiAgent = aiAgent,
            AiAgentId = aiAgent.Id,
            ConversationId = conversationId,
            OriginalMessage = originalMessage,
            EnhancedMessage = null,
            TemperamentUsed = aiAgent.Temperament ?? string.Empty,
            AiModelUsed = aiAgent.AiModel?.Name ?? "unknown",
            TemperatureUsed = (double?)aiAgent.Temperature ?? 0.7,
            MaxTokensUsed = aiAgent.MaxResponseLength,
            TokensConsumed = null,
            ResponseTimeMs = responseTimeMs,
            Success = false,
            ErrorMessage = errorMessage,
            UserAgent = userAgent,
            IpAddress = ipAddress,
            OpenaiSystemMessage = openaiSystemMessage,
            OpenaiUserMessage = openaiUserMessage,
            OpenaiFullPayload = openaiFullPayload
        };

        _context.MessageEnhancementAudits.Add(audit);
        await _context.SaveChangesAsync();

        _logger.LogWarning("Message enhancement failure recorded with ID: {AuditId} - Error: {Error}",
            audit.Id, errorMessage);
        return audit;
    }

    public async Task<(List<MessageEnhancementAudit> Items, int TotalCount)> GetAuditsByCompanyAsync(
        Guid companyId, int page, int pageSize)
    {
        var query = _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.CompanyId == companyId)
            .OrderByDescending(a => a.CreatedAt);

        var totalCount = await query.CountAsync();
        var items = await query
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<(List<MessageEnhancementAudit> Items, int TotalCount)> GetAuditsByUserAsync(
        Guid userId, int page, int pageSize)
    {
        var query = _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.UserId == userId)
            .OrderByDescending(a => a.CreatedAt);

        var totalCount = await query.CountAsync();
        var items = await query
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<(List<MessageEnhancementAudit> Items, int TotalCount)> GetAuditsByAiAgentAsync(
        Guid aiAgentId, int page, int pageSize)
    {
        var query = _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.AiAgentId == aiAgentId)
            .OrderByDescending(a => a.CreatedAt);

        var totalCount = await query.CountAsync();
        var items = await query
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<List<MessageEnhancementAudit>> GetAuditsByConversationAsync(Guid conversationId)
    {
        return await _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.ConversationId == conversationId)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }

    public async Task<EnhancementStats> GetCompanyStatsAsync(Guid companyId)
    {
        var audits = await _context.MessageEnhancementAudits
            .Where(a => a.CompanyId == companyId)
            .ToListAsync();

        var successful = audits.Count(a => a.Success);
        var failed = audits.Count(a => !a.Success);
        var totalTokens = audits.Where(a => a.TokensConsumed.HasValue).Sum(a => a.TokensConsumed.Value);
        var avgResponseTime = audits.Where(a => a.ResponseTimeMs.HasValue)
            .Average(a => (double?)a.ResponseTimeMs) ?? 0.0;

        return new EnhancementStats
        {
            SuccessfulEnhancements = successful,
            FailedEnhancements = failed,
            TotalEnhancements = successful + failed,
            TotalTokensConsumed = totalTokens,
            AverageResponseTimeMs = avgResponseTime,
            SuccessRate = successful + failed > 0 ? (double)successful / (successful + failed) * 100 : 0.0
        };
    }

    public async Task<(List<MessageEnhancementAudit> Items, int TotalCount)> GetAuditsByDateRangeAsync(
        Guid companyId, DateTime startDate, DateTime endDate, int page, int pageSize)
    {
        var query = _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.CompanyId == companyId &&
                       a.CreatedAt >= startDate &&
                       a.CreatedAt <= endDate)
            .OrderByDescending(a => a.CreatedAt);

        var totalCount = await query.CountAsync();
        var items = await query
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<(List<MessageEnhancementAudit> Items, int TotalCount)> GetAuditsByTemperamentAsync(
        Guid companyId, string temperament, int page, int pageSize)
    {
        var query = _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.CompanyId == companyId &&
                       a.TemperamentUsed == temperament)
            .OrderByDescending(a => a.CreatedAt);

        var totalCount = await query.CountAsync();
        var items = await query
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }

    public async Task<(List<MessageEnhancementAudit> Items, int TotalCount)> GetAuditsByAiModelAsync(
        Guid companyId, string model, int page, int pageSize)
    {
        var query = _context.MessageEnhancementAudits
            .Include(a => a.User)
            .Include(a => a.AiAgent)
            .Where(a => a.CompanyId == companyId &&
                       a.AiModelUsed == model)
            .OrderByDescending(a => a.CreatedAt);

        var totalCount = await query.CountAsync();
        var items = await query
            .Skip(page * pageSize)
            .Take(pageSize)
            .ToListAsync();

        return (items, totalCount);
    }
}

public class EnhancementStats
{
    public long SuccessfulEnhancements { get; set; }
    public long FailedEnhancements { get; set; }
    public long TotalEnhancements { get; set; }
    public long TotalTokensConsumed { get; set; }
    public double AverageResponseTimeMs { get; set; }
    public double SuccessRate { get; set; }
}