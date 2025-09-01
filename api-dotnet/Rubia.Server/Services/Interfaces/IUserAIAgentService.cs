using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IUserAIAgentService
{
    Task<UserAIAgentDto> CreateUserAIAgentAsync(CreateUserAIAgentDto createDto, CancellationToken cancellationToken = default);
    Task<UserAIAgentDto> UpdateUserAIAgentAsync(Guid userAIAgentId, UpdateUserAIAgentDto updateDto, CancellationToken cancellationToken = default);
    Task<bool> DeleteUserAIAgentAsync(Guid userAIAgentId, CancellationToken cancellationToken = default);
    Task<UserAIAgentDto?> GetUserAIAgentByIdAsync(Guid userAIAgentId, CancellationToken cancellationToken = default);
    Task<IEnumerable<UserAIAgentDto>> GetUserAIAgentsByUserAsync(Guid userId, CancellationToken cancellationToken = default);
    Task<IEnumerable<UserAIAgentDto>> GetUserAIAgentsByCompanyAsync(Guid companyId, CancellationToken cancellationToken = default);
    
    Task<bool> SetActiveAIAgentAsync(Guid userId, Guid aiAgentId, CancellationToken cancellationToken = default);
    Task<UserAIAgentDto?> GetActiveAIAgentAsync(Guid userId, CancellationToken cancellationToken = default);
    Task<bool> DeactivateAIAgentAsync(Guid userId, Guid aiAgentId, CancellationToken cancellationToken = default);
    
    Task<IEnumerable<AIAgentDto>> GetAvailableAIAgentsAsync(Guid userId, CancellationToken cancellationToken = default);
    Task<UserAIAgentStatsDto> GetUserAIAgentStatsAsync(Guid userId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default);
    
    Task<bool> UpdateAIAgentPreferencesAsync(Guid userAIAgentId, UpdateAIAgentPreferencesDto preferencesDto, CancellationToken cancellationToken = default);
    Task<AIAgentPreferencesDto> GetAIAgentPreferencesAsync(Guid userAIAgentId, CancellationToken cancellationToken = default);
}