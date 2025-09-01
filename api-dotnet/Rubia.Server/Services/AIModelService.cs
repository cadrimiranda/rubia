using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Caching.Memory;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class AIModelService : IAIModelService
{
    private readonly RubiaDbContext _context;
    private readonly IMemoryCache _cache;
    private readonly ILogger<AIModelService> _logger;
    private const string ActiveModelsKey = "activeAIModels";
    private readonly TimeSpan CacheExpiration = TimeSpan.FromMinutes(30);

    public AIModelService(RubiaDbContext context, IMemoryCache cache, ILogger<AIModelService> logger)
    {
        _context = context;
        _cache = cache;
        _logger = logger;
    }

    public async Task<List<AIModelDto>> GetActiveModelsAsync()
    {
        _logger.LogDebug("Fetching active AI models");

        if (_cache.TryGetValue(ActiveModelsKey, out List<AIModelDto>? cachedModels) && cachedModels != null)
        {
            _logger.LogDebug("Returning cached active AI models");
            return cachedModels;
        }

        var models = await _context.AIModels
            .Where(m => m.IsActive)
            .OrderBy(m => m.SortOrder)
            .ThenBy(m => m.Name)
            .ToListAsync();

        var modelDtos = models.Select(ToDto).ToList();

        if (modelDtos.Count > 0)
        {
            _cache.Set(ActiveModelsKey, modelDtos, CacheExpiration);
        }

        return modelDtos;
    }

    public async Task<List<AIModelDto>> GetAllModelsAsync()
    {
        _logger.LogDebug("Fetching all AI models");

        var models = await _context.AIModels
            .OrderBy(m => m.SortOrder)
            .ThenBy(m => m.Name)
            .ToListAsync();

        return models.Select(ToDto).ToList();
    }

    public async Task<List<AIModelDto>> GetModelsByProviderAsync(string provider)
    {
        _logger.LogDebug("Fetching AI models by provider: {Provider}", provider);

        var models = await _context.AIModels
            .Where(m => m.Provider == provider)
            .OrderBy(m => m.SortOrder)
            .ThenBy(m => m.Name)
            .ToListAsync();

        return models.Select(ToDto).ToList();
    }

    public async Task<AIModelDto> GetModelByIdAsync(Guid id)
    {
        _logger.LogDebug("Fetching AI model with id: {Id}", id);

        var model = await _context.AIModels.FirstOrDefaultAsync(m => m.Id == id);

        if (model == null)
        {
            throw new ArgumentException("Modelo de IA não encontrado");
        }

        return ToDto(model);
    }

    public async Task<AIModelDto?> GetModelByNameAsync(string name)
    {
        _logger.LogDebug("Fetching AI model with name: {Name}", name);

        var model = await _context.AIModels.FirstOrDefaultAsync(m => m.Name == name);

        return model != null ? ToDto(model) : null;
    }

    public async Task<bool> ExistsByNameAsync(string name)
    {
        return await _context.AIModels.AnyAsync(m => m.Name == name);
    }

    public async Task<long> CountActiveModelsAsync()
    {
        return await _context.AIModels.Where(m => m.IsActive).CountAsync();
    }

    public async Task<AIModelDto> CreateAsync(CreateAIModelDto createDto)
    {
        _logger.LogInformation("Creating AI model: {Name}", createDto.Name);

        if (await ExistsByNameAsync(createDto.Name))
        {
            throw new ArgumentException($"Modelo de IA com o nome '{createDto.Name}' já existe");
        }

        var model = new AIModel
        {
            Name = createDto.Name,
            DisplayName = createDto.DisplayName,
            Description = createDto.Description,
            Capabilities = createDto.Capabilities,
            ImpactDescription = createDto.ImpactDescription,
            CostPer1kTokens = createDto.CostPer1kTokens,
            PerformanceLevel = createDto.PerformanceLevel?.ToUpper(),
            Provider = createDto.Provider,
            IsActive = createDto.IsActive,
            SortOrder = createDto.SortOrder
        };

        _context.AIModels.Add(model);
        await _context.SaveChangesAsync();

        ClearCache();
        _logger.LogInformation("AI model created successfully with id: {Id}", model.Id);
        
        return ToDto(model);
    }

    public async Task<AIModelDto> UpdateAsync(Guid id, UpdateAIModelDto updateDto)
    {
        _logger.LogInformation("Updating AI model with id: {Id}", id);

        var model = await _context.AIModels.FirstOrDefaultAsync(m => m.Id == id);

        if (model == null)
        {
            throw new ArgumentException("Modelo de IA não encontrado");
        }

        if (updateDto.Name != null && updateDto.Name != model.Name)
        {
            if (await ExistsByNameAsync(updateDto.Name))
            {
                throw new ArgumentException($"Modelo de IA com o nome '{updateDto.Name}' já existe");
            }
            model.Name = updateDto.Name;
        }

        if (updateDto.DisplayName != null) model.DisplayName = updateDto.DisplayName;
        if (updateDto.Description != null) model.Description = updateDto.Description;
        if (updateDto.Capabilities != null) model.Capabilities = updateDto.Capabilities;
        if (updateDto.ImpactDescription != null) model.ImpactDescription = updateDto.ImpactDescription;
        if (updateDto.CostPer1kTokens.HasValue) model.CostPer1kTokens = updateDto.CostPer1kTokens;
        if (updateDto.PerformanceLevel != null) model.PerformanceLevel = updateDto.PerformanceLevel.ToUpper();
        if (updateDto.Provider != null) model.Provider = updateDto.Provider;
        if (updateDto.IsActive.HasValue) model.IsActive = updateDto.IsActive.Value;
        if (updateDto.SortOrder.HasValue) model.SortOrder = updateDto.SortOrder.Value;

        await _context.SaveChangesAsync();

        ClearCache();
        _logger.LogInformation("AI model updated successfully");

        return ToDto(model);
    }

    public async Task DeleteAsync(Guid id)
    {
        _logger.LogInformation("Deleting AI model with id: {Id}", id);

        var model = await _context.AIModels.FirstOrDefaultAsync(m => m.Id == id);

        if (model == null)
        {
            throw new ArgumentException("Modelo de IA não encontrado");
        }

        _context.AIModels.Remove(model);
        await _context.SaveChangesAsync();

        ClearCache();
        _logger.LogInformation("AI model deleted successfully");
    }

    private void ClearCache()
    {
        _cache.Remove(ActiveModelsKey);
    }

    private static AIModelDto ToDto(AIModel model)
    {
        return new AIModelDto
        {
            Id = model.Id,
            Name = model.Name,
            DisplayName = model.DisplayName,
            Description = model.Description,
            Capabilities = model.Capabilities,
            ImpactDescription = model.ImpactDescription,
            CostPer1kTokens = model.CostPer1kTokens,
            PerformanceLevel = model.PerformanceLevel,
            Provider = model.Provider,
            IsActive = model.IsActive,
            SortOrder = model.SortOrder,
            CreatedAt = model.CreatedAt,
            UpdatedAt = model.UpdatedAt
        };
    }
}