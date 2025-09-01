using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class AILogService : IAILogService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<AILogService> _logger;

    public AILogService(RubiaDbContext context, ILogger<AILogService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<AILog> CreateAILogAsync(CreateAILogDto createDto)
    {
        var aiLog = new AILog
        {
            Id = Guid.NewGuid(),
            CompanyId = createDto.CompanyId,
            AiAgentId = createDto.AiAgentId,
            UserId = createDto.UserId,
            ConversationId = createDto.ConversationId,
            MessageId = createDto.MessageId,
            MessageTemplateId = createDto.MessageTemplateId,
            RequestPrompt = createDto.RequestPrompt,
            RawResponse = createDto.RawResponse,
            ProcessedResponse = createDto.ProcessedResponse,
            TokensUsedInput = createDto.TokensUsedInput,
            TokensUsedOutput = createDto.TokensUsedOutput,
            EstimatedCost = createDto.EstimatedCost,
            Status = createDto.Status,
            ErrorMessage = createDto.ErrorMessage,
            CreatedAt = DateTime.UtcNow
        };

        _context.AILogs.Add(aiLog);
        await _context.SaveChangesAsync();

        _logger.LogInformation("AI log created: {Id} for company {CompanyId}", aiLog.Id, aiLog.CompanyId);
        return aiLog;
    }

    public async Task<AILog?> GetAILogByIdAsync(Guid id)
    {
        return await _context.AILogs
            .Include(a => a.Company)
            .Include(a => a.AiAgent)
            .Include(a => a.User)
            .Include(a => a.Conversation)
            .Include(a => a.Message)
            .Include(a => a.MessageTemplate)
            .FirstOrDefaultAsync(a => a.Id == id);
    }

    public async Task<IEnumerable<AILog>> GetAllAILogsAsync(int page, int size, string sortBy, string sortDir)
    {
        var query = _context.AILogs
            .Include(a => a.Company)
            .Include(a => a.AiAgent)
            .Include(a => a.User)
            .Include(a => a.Conversation)
            .Include(a => a.Message)
            .Include(a => a.MessageTemplate)
            .AsQueryable();

        // Apply sorting
        query = sortBy.ToLower() switch
        {
            "createdat" => sortDir.ToLower() == "desc" 
                ? query.OrderByDescending(a => a.CreatedAt)
                : query.OrderBy(a => a.CreatedAt),
            "status" => sortDir.ToLower() == "desc"
                ? query.OrderByDescending(a => a.Status)
                : query.OrderBy(a => a.Status),
            "estimatedcost" => sortDir.ToLower() == "desc"
                ? query.OrderByDescending(a => a.EstimatedCost)
                : query.OrderBy(a => a.EstimatedCost),
            _ => query.OrderByDescending(a => a.CreatedAt)
        };

        return await query
            .Skip(page * size)
            .Take(size)
            .ToListAsync();
    }

    public async Task<IEnumerable<AILog>> GetAILogsByCompanyIdAsync(Guid companyId)
    {
        return await _context.AILogs
            .Include(a => a.Company)
            .Include(a => a.AiAgent)
            .Include(a => a.User)
            .Include(a => a.Conversation)
            .Include(a => a.Message)
            .Include(a => a.MessageTemplate)
            .Where(a => a.CompanyId == companyId)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }

    public async Task<IEnumerable<AILog>> GetAILogsByStatusAsync(AILogStatus status)
    {
        return await _context.AILogs
            .Include(a => a.Company)
            .Include(a => a.AiAgent)
            .Include(a => a.User)
            .Include(a => a.Conversation)
            .Include(a => a.Message)
            .Include(a => a.MessageTemplate)
            .Where(a => a.Status == status)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }

    public async Task<IEnumerable<AILog>> GetAILogsByAIAgentIdAsync(Guid aiAgentId)
    {
        return await _context.AILogs
            .Include(a => a.Company)
            .Include(a => a.AiAgent)
            .Include(a => a.User)
            .Include(a => a.Conversation)
            .Include(a => a.Message)
            .Include(a => a.MessageTemplate)
            .Where(a => a.AiAgentId == aiAgentId)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }

    public async Task<AILog?> UpdateAILogAsync(Guid id, UpdateAILogDto updateDto)
    {
        var aiLog = await _context.AILogs.FindAsync(id);
        if (aiLog == null)
            return null;

        if (!string.IsNullOrEmpty(updateDto.RawResponse))
            aiLog.RawResponse = updateDto.RawResponse;

        if (!string.IsNullOrEmpty(updateDto.ProcessedResponse))
            aiLog.ProcessedResponse = updateDto.ProcessedResponse;

        if (updateDto.TokensUsedInput.HasValue)
            aiLog.TokensUsedInput = updateDto.TokensUsedInput.Value;

        if (updateDto.TokensUsedOutput.HasValue)
            aiLog.TokensUsedOutput = updateDto.TokensUsedOutput.Value;

        if (updateDto.EstimatedCost.HasValue)
            aiLog.EstimatedCost = updateDto.EstimatedCost.Value;

        if (updateDto.Status.HasValue)
            aiLog.Status = updateDto.Status.Value;

        if (!string.IsNullOrEmpty(updateDto.ErrorMessage))
            aiLog.ErrorMessage = updateDto.ErrorMessage;

        await _context.SaveChangesAsync();

        _logger.LogInformation("AI log updated: {Id}", id);
        return aiLog;
    }

    public async Task<bool> DeleteAILogAsync(Guid id)
    {
        var aiLog = await _context.AILogs.FindAsync(id);
        if (aiLog == null)
            return false;

        _context.AILogs.Remove(aiLog);
        await _context.SaveChangesAsync();

        _logger.LogInformation("AI log deleted: {Id}", id);
        return true;
    }

    public async Task<decimal> GetTotalCostByCompanyIdAsync(Guid companyId)
    {
        return await _context.AILogs
            .Where(a => a.CompanyId == companyId && a.EstimatedCost.HasValue)
            .SumAsync(a => a.EstimatedCost.Value);
    }

    public async Task<long> GetTotalTokensUsedByCompanyIdAsync(Guid companyId)
    {
        var inputTokens = await _context.AILogs
            .Where(a => a.CompanyId == companyId && a.TokensUsedInput.HasValue)
            .SumAsync(a => a.TokensUsedInput.Value);

        var outputTokens = await _context.AILogs
            .Where(a => a.CompanyId == companyId && a.TokensUsedOutput.HasValue)
            .SumAsync(a => a.TokensUsedOutput.Value);

        return inputTokens + outputTokens;
    }

    public async Task<long> CountAILogsByCompanyIdAndStatusAsync(Guid companyId, AILogStatus status)
    {
        return await _context.AILogs
            .Where(a => a.CompanyId == companyId && a.Status == status)
            .CountAsync();
    }

    public async Task<IEnumerable<AILog>> GetAILogsByDateRangeAsync(DateTime startDate, DateTime endDate)
    {
        return await _context.AILogs
            .Include(a => a.Company)
            .Include(a => a.AiAgent)
            .Include(a => a.User)
            .Include(a => a.Conversation)
            .Include(a => a.Message)
            .Include(a => a.MessageTemplate)
            .Where(a => a.CreatedAt >= startDate && a.CreatedAt <= endDate)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }
}