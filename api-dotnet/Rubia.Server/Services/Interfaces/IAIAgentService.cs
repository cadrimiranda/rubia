using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IAIAgentService
{
    Task<AIAgentDto> CreateAsync(CreateAIAgentDto createDto);
    Task<AIAgentDto> GetByIdAsync(Guid id);
    Task<List<AIAgentDto>> GetAllByCompanyAsync(Guid companyId);
    Task<List<AIAgentDto>> GetActiveByCompanyAsync(Guid companyId);
    Task<List<AIAgentDto>> GetAllByCompanyOrderByNameAsync(Guid companyId);
    Task<AIAgentDto> UpdateAsync(Guid id, UpdateAIAgentDto updateDto);
    Task DeleteAsync(Guid id);
    Task<long> CountByCompanyAsync(Guid companyId);
    Task<long> CountActiveByCompanyAsync(Guid companyId);
    Task<bool> ExistsByNameAndCompanyAsync(string name, Guid companyId);
    Task<bool> CanCreateAgentAsync(Guid companyId);
    Task<int> GetRemainingAgentSlotsAsync(Guid companyId);
    Task<List<AIAgentDto>> GetByModelIdAsync(Guid modelId);
    Task<List<AIAgentDto>> GetByTemperamentAsync(string temperament);
    Task<int?> GetAIMessageLimitForCompanyAsync(Guid companyId);
}