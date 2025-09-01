using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IAIModelService
{
    Task<List<AIModelDto>> GetActiveModelsAsync();
    Task<List<AIModelDto>> GetAllModelsAsync();
    Task<List<AIModelDto>> GetModelsByProviderAsync(string provider);
    Task<AIModelDto> GetModelByIdAsync(Guid id);
    Task<AIModelDto?> GetModelByNameAsync(string name);
    Task<bool> ExistsByNameAsync(string name);
    Task<long> CountActiveModelsAsync();
    Task<AIModelDto> CreateAsync(CreateAIModelDto createDto);
    Task<AIModelDto> UpdateAsync(Guid id, UpdateAIModelDto updateDto);
    Task DeleteAsync(Guid id);
}