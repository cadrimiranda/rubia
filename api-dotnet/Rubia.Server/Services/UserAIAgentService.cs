using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class UserAIAgentService : IUserAIAgentService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<UserAIAgentService> _logger;

    public UserAIAgentService(
        RubiaDbContext context,
        ILogger<UserAIAgentService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<UserAIAgentDto> CreateUserAIAgentAsync(CreateUserAIAgentDto createDto, CancellationToken cancellationToken = default)
    {
        // Validate user exists
        var user = await _context.Users.FindAsync(createDto.UserId, cancellationToken);
        if (user == null)
            throw new ArgumentException($"User {createDto.UserId} not found");

        // Validate AI agent exists and belongs to same company
        var aiAgent = await _context.AIAgents.FindAsync(createDto.AiAgentId, cancellationToken);
        if (aiAgent == null)
            throw new ArgumentException($"AI Agent {createDto.AiAgentId} not found");

        if (aiAgent.CompanyId != user.CompanyId)
            throw new InvalidOperationException("AI Agent must belong to the same company as the user");

        // Check if association already exists
        var existingAssociation = await _context.UserAIAgents
            .FirstOrDefaultAsync(ua => ua.UserId == createDto.UserId && ua.AiAgentId == createDto.AiAgentId, cancellationToken);

        if (existingAssociation != null)
            throw new InvalidOperationException("User is already associated with this AI Agent");

        var userAIAgent = new UserAIAgent
        {
            Id = Guid.NewGuid(),
            UserId = createDto.UserId,
            AiAgentId = createDto.AiAgentId,
            IsActive = createDto.IsActive,
            IsPrimary = createDto.IsPrimary,
            CustomPrompt = createDto.CustomPrompt,
            CustomTemperature = createDto.CustomTemperature,
            CustomMaxTokens = createDto.CustomMaxTokens,
            AutoResponseEnabled = createDto.AutoResponseEnabled ?? false,
            DailyMessageLimit = createDto.DailyMessageLimit,
            HourlyMessageLimit = createDto.HourlyMessageLimit,
            CreatedAt = DateTime.UtcNow
        };

        // If this is set as primary, deactivate other primary agents for this user
        if (userAIAgent.IsPrimary)
        {
            var existingPrimary = await _context.UserAIAgents
                .Where(ua => ua.UserId == createDto.UserId && ua.IsPrimary)
                .ToListAsync(cancellationToken);

            foreach (var existing in existingPrimary)
            {
                existing.IsPrimary = false;
                existing.UpdatedAt = DateTime.UtcNow;
            }
        }

        _context.UserAIAgents.Add(userAIAgent);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("User {UserId} associated with AI Agent {AiAgentId}", createDto.UserId, createDto.AiAgentId);

        return await MapToDtoAsync(userAIAgent, cancellationToken);
    }

    public async Task<UserAIAgentDto> UpdateUserAIAgentAsync(Guid userAIAgentId, UpdateUserAIAgentDto updateDto, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents.FindAsync(userAIAgentId, cancellationToken);
        if (userAIAgent == null)
            throw new ArgumentException($"UserAIAgent {userAIAgentId} not found");

        userAIAgent.IsActive = updateDto.IsActive ?? userAIAgent.IsActive;
        userAIAgent.CustomPrompt = updateDto.CustomPrompt ?? userAIAgent.CustomPrompt;
        userAIAgent.CustomTemperature = updateDto.CustomTemperature ?? userAIAgent.CustomTemperature;
        userAIAgent.CustomMaxTokens = updateDto.CustomMaxTokens ?? userAIAgent.CustomMaxTokens;
        userAIAgent.AutoResponseEnabled = updateDto.AutoResponseEnabled ?? userAIAgent.AutoResponseEnabled;
        userAIAgent.DailyMessageLimit = updateDto.DailyMessageLimit ?? userAIAgent.DailyMessageLimit;
        userAIAgent.HourlyMessageLimit = updateDto.HourlyMessageLimit ?? userAIAgent.HourlyMessageLimit;
        userAIAgent.UpdatedAt = DateTime.UtcNow;

        // Handle primary flag change
        if (updateDto.IsPrimary.HasValue && updateDto.IsPrimary.Value != userAIAgent.IsPrimary)
        {
            if (updateDto.IsPrimary.Value)
            {
                // Set as primary, deactivate others
                var existingPrimary = await _context.UserAIAgents
                    .Where(ua => ua.UserId == userAIAgent.UserId && ua.IsPrimary && ua.Id != userAIAgentId)
                    .ToListAsync(cancellationToken);

                foreach (var existing in existingPrimary)
                {
                    existing.IsPrimary = false;
                    existing.UpdatedAt = DateTime.UtcNow;
                }
            }
            userAIAgent.IsPrimary = updateDto.IsPrimary.Value;
        }

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("UserAIAgent {UserAIAgentId} updated", userAIAgentId);

        return await MapToDtoAsync(userAIAgent, cancellationToken);
    }

    public async Task<bool> DeleteUserAIAgentAsync(Guid userAIAgentId, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents.FindAsync(userAIAgentId, cancellationToken);
        if (userAIAgent == null)
            return false;

        _context.UserAIAgents.Remove(userAIAgent);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("UserAIAgent {UserAIAgentId} deleted", userAIAgentId);

        return true;
    }

    public async Task<UserAIAgentDto?> GetUserAIAgentByIdAsync(Guid userAIAgentId, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents
            .Include(ua => ua.User)
            .Include(ua => ua.AiAgent)
            .FirstOrDefaultAsync(ua => ua.Id == userAIAgentId, cancellationToken);

        return userAIAgent != null ? await MapToDtoAsync(userAIAgent, cancellationToken) : null;
    }

    public async Task<IEnumerable<UserAIAgentDto>> GetUserAIAgentsByUserAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        var userAIAgents = await _context.UserAIAgents
            .Include(ua => ua.User)
            .Include(ua => ua.AiAgent)
            .Where(ua => ua.UserId == userId)
            .OrderByDescending(ua => ua.IsPrimary)
            .ThenBy(ua => ua.CreatedAt)
            .ToListAsync(cancellationToken);

        var tasks = userAIAgents.Select(ua => MapToDtoAsync(ua, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<IEnumerable<UserAIAgentDto>> GetUserAIAgentsByCompanyAsync(Guid companyId, CancellationToken cancellationToken = default)
    {
        var userAIAgents = await _context.UserAIAgents
            .Include(ua => ua.User)
            .Include(ua => ua.AiAgent)
            .Where(ua => ua.User!.CompanyId == companyId)
            .OrderBy(ua => ua.User!.Name)
            .ThenByDescending(ua => ua.IsPrimary)
            .ToListAsync(cancellationToken);

        var tasks = userAIAgents.Select(ua => MapToDtoAsync(ua, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<bool> SetActiveAIAgentAsync(Guid userId, Guid aiAgentId, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents
            .FirstOrDefaultAsync(ua => ua.UserId == userId && ua.AiAgentId == aiAgentId, cancellationToken);

        if (userAIAgent == null)
            return false;

        userAIAgent.IsActive = true;
        userAIAgent.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("AI Agent {AiAgentId} activated for user {UserId}", aiAgentId, userId);

        return true;
    }

    public async Task<UserAIAgentDto?> GetActiveAIAgentAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents
            .Include(ua => ua.User)
            .Include(ua => ua.AiAgent)
            .Where(ua => ua.UserId == userId && ua.IsActive)
            .OrderByDescending(ua => ua.IsPrimary)
            .ThenByDescending(ua => ua.UpdatedAt)
            .FirstOrDefaultAsync(cancellationToken);

        return userAIAgent != null ? await MapToDtoAsync(userAIAgent, cancellationToken) : null;
    }

    public async Task<bool> DeactivateAIAgentAsync(Guid userId, Guid aiAgentId, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents
            .FirstOrDefaultAsync(ua => ua.UserId == userId && ua.AiAgentId == aiAgentId, cancellationToken);

        if (userAIAgent == null)
            return false;

        userAIAgent.IsActive = false;
        userAIAgent.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("AI Agent {AiAgentId} deactivated for user {UserId}", aiAgentId, userId);

        return true;
    }

    public async Task<IEnumerable<AIAgentDto>> GetAvailableAIAgentsAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        var user = await _context.Users.FindAsync(userId, cancellationToken);
        if (user == null)
            return new List<AIAgentDto>();

        var aiAgents = await _context.AIAgents
            .Include(aa => aa.AIModel)
            .Where(aa => aa.CompanyId == user.CompanyId && aa.IsActive)
            .OrderBy(aa => aa.Name)
            .ToListAsync(cancellationToken);

        return aiAgents.Select(MapAIAgentToDto);
    }

    public async Task<UserAIAgentStatsDto> GetUserAIAgentStatsAsync(Guid userId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default)
    {
        fromDate ??= DateTime.UtcNow.AddMonths(-1);
        toDate ??= DateTime.UtcNow;

        var userAIAgents = await _context.UserAIAgents
            .Where(ua => ua.UserId == userId)
            .ToListAsync(cancellationToken);

        var aiGeneratedMessages = await _context.Messages
            .Where(m => m.IsAiGenerated == true
                       && userAIAgents.Select(ua => ua.AiAgentId).Contains(m.AiAgentId!.Value)
                       && m.CreatedAt >= fromDate
                       && m.CreatedAt <= toDate)
            .CountAsync(cancellationToken);

        var averageConfidence = await _context.Messages
            .Where(m => m.IsAiGenerated == true
                       && userAIAgents.Select(ua => ua.AiAgentId).Contains(m.AiAgentId!.Value)
                       && m.AiConfidence.HasValue
                       && m.CreatedAt >= fromDate
                       && m.CreatedAt <= toDate)
            .Select(m => m.AiConfidence!.Value)
            .DefaultIfEmpty(0)
            .AverageAsync(cancellationToken);

        return new UserAIAgentStatsDto
        {
            UserId = userId,
            PeriodStart = fromDate.Value,
            PeriodEnd = toDate.Value,
            ActiveAIAgents = userAIAgents.Count(ua => ua.IsActive),
            TotalAIAgents = userAIAgents.Count,
            AIGeneratedMessages = aiGeneratedMessages,
            AverageAIConfidence = averageConfidence,
            TotalTokensUsed = 0, // Would need additional tracking
            TotalCost = 0 // Would need additional tracking
        };
    }

    public async Task<bool> UpdateAIAgentPreferencesAsync(Guid userAIAgentId, UpdateAIAgentPreferencesDto preferencesDto, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents.FindAsync(userAIAgentId, cancellationToken);
        if (userAIAgent == null)
            return false;

        userAIAgent.CustomPrompt = preferencesDto.CustomPrompt ?? userAIAgent.CustomPrompt;
        userAIAgent.CustomTemperature = preferencesDto.CustomTemperature ?? userAIAgent.CustomTemperature;
        userAIAgent.CustomMaxTokens = preferencesDto.CustomMaxTokens ?? userAIAgent.CustomMaxTokens;
        userAIAgent.AutoResponseEnabled = preferencesDto.AutoResponseEnabled ?? userAIAgent.AutoResponseEnabled;
        userAIAgent.DailyMessageLimit = preferencesDto.DailyMessageLimit ?? userAIAgent.DailyMessageLimit;
        userAIAgent.HourlyMessageLimit = preferencesDto.HourlyMessageLimit ?? userAIAgent.HourlyMessageLimit;
        userAIAgent.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("AI Agent preferences updated for UserAIAgent {UserAIAgentId}", userAIAgentId);

        return true;
    }

    public async Task<AIAgentPreferencesDto> GetAIAgentPreferencesAsync(Guid userAIAgentId, CancellationToken cancellationToken = default)
    {
        var userAIAgent = await _context.UserAIAgents
            .Include(ua => ua.AiAgent)
            .FirstOrDefaultAsync(ua => ua.Id == userAIAgentId, cancellationToken);

        if (userAIAgent == null)
            throw new ArgumentException($"UserAIAgent {userAIAgentId} not found");

        return new AIAgentPreferencesDto
        {
            UserAIAgentId = userAIAgentId,
            CustomPrompt = userAIAgent.CustomPrompt,
            CustomTemperature = userAIAgent.CustomTemperature,
            CustomMaxTokens = userAIAgent.CustomMaxTokens,
            AutoResponseEnabled = userAIAgent.AutoResponseEnabled,
            DailyMessageLimit = userAIAgent.DailyMessageLimit,
            HourlyMessageLimit = userAIAgent.HourlyMessageLimit,
            DefaultPrompt = userAIAgent.AiAgent?.Prompt,
            DefaultTemperature = userAIAgent.AiAgent?.Temperature ?? 0.7,
            DefaultMaxTokens = userAIAgent.AiAgent?.MaxTokens ?? 150
        };
    }

    private async Task<UserAIAgentDto> MapToDtoAsync(UserAIAgent userAIAgent, CancellationToken cancellationToken)
    {
        if (userAIAgent.User == null)
        {
            await _context.Entry(userAIAgent)
                .Reference(ua => ua.User)
                .LoadAsync(cancellationToken);
        }

        if (userAIAgent.AiAgent == null)
        {
            await _context.Entry(userAIAgent)
                .Reference(ua => ua.AiAgent)
                .LoadAsync(cancellationToken);
        }

        return new UserAIAgentDto
        {
            Id = userAIAgent.Id,
            UserId = userAIAgent.UserId,
            AiAgentId = userAIAgent.AiAgentId,
            IsActive = userAIAgent.IsActive,
            IsPrimary = userAIAgent.IsPrimary,
            CustomPrompt = userAIAgent.CustomPrompt,
            CustomTemperature = userAIAgent.CustomTemperature,
            CustomMaxTokens = userAIAgent.CustomMaxTokens,
            AutoResponseEnabled = userAIAgent.AutoResponseEnabled,
            DailyMessageLimit = userAIAgent.DailyMessageLimit,
            HourlyMessageLimit = userAIAgent.HourlyMessageLimit,
            MessagesToday = userAIAgent.MessagesToday,
            MessagesThisHour = userAIAgent.MessagesThisHour,
            UserName = userAIAgent.User?.Name ?? "Unknown",
            UserEmail = userAIAgent.User?.Email ?? "Unknown",
            AiAgentName = userAIAgent.AiAgent?.Name ?? "Unknown",
            AiAgentDescription = userAIAgent.AiAgent?.Description,
            CreatedAt = userAIAgent.CreatedAt,
            UpdatedAt = userAIAgent.UpdatedAt
        };
    }

    private static AIAgentDto MapAIAgentToDto(AIAgent aiAgent)
    {
        return new AIAgentDto
        {
            Id = aiAgent.Id,
            CompanyId = aiAgent.CompanyId,
            AIModelId = aiAgent.AIModelId,
            Name = aiAgent.Name,
            Description = aiAgent.Description,
            Prompt = aiAgent.Prompt,
            Temperature = aiAgent.Temperature,
            MaxTokens = aiAgent.MaxTokens,
            AvatarBase64 = aiAgent.AvatarBase64,
            IsActive = aiAgent.IsActive,
            DailyMessageLimit = aiAgent.DailyMessageLimit,
            MonthlyMessageLimit = aiAgent.MonthlyMessageLimit,
            AIModelName = aiAgent.AIModel?.Name,
            AIModelProvider = aiAgent.AIModel?.Provider,
            CreatedAt = aiAgent.CreatedAt,
            UpdatedAt = aiAgent.UpdatedAt
        };
    }
}