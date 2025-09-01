using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface IAILogService
{
    Task<AILog> CreateAILogAsync(CreateAILogDto createDto);
    Task<AILog?> GetAILogByIdAsync(Guid id);
    Task<IEnumerable<AILog>> GetAllAILogsAsync(int page, int size, string sortBy, string sortDir);
    Task<IEnumerable<AILog>> GetAILogsByCompanyIdAsync(Guid companyId);
    Task<IEnumerable<AILog>> GetAILogsByStatusAsync(AILogStatus status);
    Task<IEnumerable<AILog>> GetAILogsByAIAgentIdAsync(Guid aiAgentId);
    Task<AILog?> UpdateAILogAsync(Guid id, UpdateAILogDto updateDto);
    Task<bool> DeleteAILogAsync(Guid id);
    Task<decimal> GetTotalCostByCompanyIdAsync(Guid companyId);
    Task<long> GetTotalTokensUsedByCompanyIdAsync(Guid companyId);
    Task<long> CountAILogsByCompanyIdAndStatusAsync(Guid companyId, AILogStatus status);
    Task<IEnumerable<AILog>> GetAILogsByDateRangeAsync(DateTime startDate, DateTime endDate);
}