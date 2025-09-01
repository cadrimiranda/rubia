using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class AIAgentService : IAIAgentService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<AIAgentService> _logger;

    public AIAgentService(RubiaDbContext context, ILogger<AIAgentService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<AIAgentDto> CreateAsync(CreateAIAgentDto createDto)
    {
        _logger.LogInformation("Creating AI agent with name: {Name} for company: {CompanyId}", createDto.Name, createDto.CompanyId);

        // Validate company exists
        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == createDto.CompanyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        // Check agent limit for company
        var currentAgentCount = await CountByCompanyAsync(createDto.CompanyId);
        if (currentAgentCount >= company.MaxAIAgents)
        {
            throw new ArgumentException($"Limite de agentes IA atingido. Plano atual permite {company.MaxAIAgents} agente(s), empresa já possui {currentAgentCount}.");
        }

        // Validate AI model exists
        var aiModel = await _context.AIModels.FirstOrDefaultAsync(m => m.Id == createDto.AIModelId);
        if (aiModel == null)
        {
            throw new ArgumentException("Modelo de IA não encontrado");
        }

        // Check if agent name already exists for this company
        if (await ExistsByNameAndCompanyAsync(createDto.Name, createDto.CompanyId))
        {
            throw new ArgumentException($"Agente com nome '{createDto.Name}' já existe nesta empresa");
        }

        var agent = new AIAgent
        {
            CompanyId = createDto.CompanyId,
            Name = createDto.Name,
            Description = createDto.Description,
            AvatarBase64 = createDto.AvatarBase64,
            AIModelId = createDto.AIModelId,
            Temperament = createDto.Temperament.ToUpper(),
            MaxResponseLength = createDto.MaxResponseLength,
            Temperature = createDto.Temperature,
            AIMessageLimit = createDto.AIMessageLimit,
            IsActive = createDto.IsActive
        };

        _context.AIAgents.Add(agent);
        await _context.SaveChangesAsync();

        _logger.LogInformation("AI agent created successfully with id: {Id}", agent.Id);
        return await GetByIdAsync(agent.Id);
    }

    public async Task<AIAgentDto> GetByIdAsync(Guid id)
    {
        _logger.LogDebug("Fetching AI agent with id: {Id}", id);

        var agent = await _context.AIAgents
            .Include(a => a.AIModel)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (agent == null)
        {
            throw new ArgumentException("Agente de IA não encontrado");
        }

        return ToDto(agent);
    }

    public async Task<List<AIAgentDto>> GetAllByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching AI agents for company: {CompanyId}", companyId);

        var agents = await _context.AIAgents
            .Include(a => a.AIModel)
            .Where(a => a.CompanyId == companyId)
            .OrderBy(a => a.Name)
            .ToListAsync();

        return agents.Select(ToDto).ToList();
    }

    public async Task<List<AIAgentDto>> GetActiveByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching active AI agents for company: {CompanyId}", companyId);

        var agents = await _context.AIAgents
            .Include(a => a.AIModel)
            .Where(a => a.CompanyId == companyId && a.IsActive)
            .OrderBy(a => a.Name)
            .ToListAsync();

        return agents.Select(ToDto).ToList();
    }

    public async Task<List<AIAgentDto>> GetAllByCompanyOrderByNameAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching AI agents for company ordered by name: {CompanyId}", companyId);

        var agents = await _context.AIAgents
            .Include(a => a.AIModel)
            .Where(a => a.CompanyId == companyId)
            .OrderBy(a => a.Name)
            .ToListAsync();

        return agents.Select(ToDto).ToList();
    }

    public async Task<AIAgentDto> UpdateAsync(Guid id, UpdateAIAgentDto updateDto)
    {
        _logger.LogInformation("Updating AI agent with id: {Id}", id);

        var agent = await _context.AIAgents
            .Include(a => a.AIModel)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (agent == null)
        {
            throw new ArgumentException("Agente de IA não encontrado");
        }

        if (updateDto.Name != null && updateDto.Name != agent.Name)
        {
            if (await ExistsByNameAndCompanyAsync(updateDto.Name, agent.CompanyId))
            {
                throw new ArgumentException($"Agente com nome '{updateDto.Name}' já existe nesta empresa");
            }
            agent.Name = updateDto.Name;
        }

        if (updateDto.Description != null) agent.Description = updateDto.Description;
        if (updateDto.AvatarBase64 != null) agent.AvatarBase64 = updateDto.AvatarBase64;

        if (updateDto.AIModelId.HasValue)
        {
            var aiModel = await _context.AIModels.FirstOrDefaultAsync(m => m.Id == updateDto.AIModelId.Value);
            if (aiModel == null)
            {
                throw new ArgumentException("Modelo de IA não encontrado");
            }
            agent.AIModelId = updateDto.AIModelId.Value;
        }

        if (updateDto.Temperament != null) agent.Temperament = updateDto.Temperament.ToUpper();
        if (updateDto.MaxResponseLength.HasValue) agent.MaxResponseLength = updateDto.MaxResponseLength.Value;
        if (updateDto.Temperature.HasValue) agent.Temperature = updateDto.Temperature.Value;
        if (updateDto.AIMessageLimit.HasValue) agent.AIMessageLimit = updateDto.AIMessageLimit.Value;
        if (updateDto.IsActive.HasValue) agent.IsActive = updateDto.IsActive.Value;

        await _context.SaveChangesAsync();
        _logger.LogInformation("AI agent updated successfully with id: {Id}", agent.Id);

        // Reload to get updated AIModel info
        await _context.Entry(agent).ReloadAsync();
        await _context.Entry(agent).Reference(a => a.AIModel).LoadAsync();

        return ToDto(agent);
    }

    public async Task DeleteAsync(Guid id)
    {
        _logger.LogInformation("Deleting AI agent with id: {Id}", id);

        var agent = await _context.AIAgents.FirstOrDefaultAsync(a => a.Id == id);

        if (agent == null)
        {
            throw new ArgumentException("Agente de IA não encontrado");
        }

        _context.AIAgents.Remove(agent);
        await _context.SaveChangesAsync();

        _logger.LogInformation("AI agent deleted successfully");
    }

    public async Task<long> CountByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Counting AI agents for company: {CompanyId}", companyId);
        return await _context.AIAgents.Where(a => a.CompanyId == companyId).CountAsync();
    }

    public async Task<long> CountActiveByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Counting active AI agents for company: {CompanyId}", companyId);
        return await _context.AIAgents.Where(a => a.CompanyId == companyId && a.IsActive).CountAsync();
    }

    public async Task<bool> ExistsByNameAndCompanyAsync(string name, Guid companyId)
    {
        _logger.LogDebug("Checking if AI agent exists with name: {Name} for company: {CompanyId}", name, companyId);
        return await _context.AIAgents.AnyAsync(a => a.Name == name && a.CompanyId == companyId);
    }

    public async Task<bool> CanCreateAgentAsync(Guid companyId)
    {
        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == companyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        var currentCount = await CountByCompanyAsync(companyId);
        return currentCount < company.MaxAIAgents;
    }

    public async Task<int> GetRemainingAgentSlotsAsync(Guid companyId)
    {
        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == companyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        var currentCount = await CountByCompanyAsync(companyId);
        return Math.Max(0, company.MaxAIAgents - (int)currentCount);
    }

    public async Task<List<AIAgentDto>> GetByModelIdAsync(Guid modelId)
    {
        _logger.LogDebug("Fetching AI agents by model id: {ModelId}", modelId);

        var agents = await _context.AIAgents
            .Include(a => a.AIModel)
            .Where(a => a.AIModelId == modelId)
            .OrderBy(a => a.Name)
            .ToListAsync();

        return agents.Select(ToDto).ToList();
    }

    public async Task<List<AIAgentDto>> GetByTemperamentAsync(string temperament)
    {
        _logger.LogDebug("Fetching AI agents by temperament: {Temperament}", temperament);

        var agents = await _context.AIAgents
            .Include(a => a.AIModel)
            .Where(a => a.Temperament == temperament.ToUpper())
            .OrderBy(a => a.Name)
            .ToListAsync();

        return agents.Select(ToDto).ToList();
    }

    public async Task<int?> GetAIMessageLimitForCompanyAsync(Guid companyId)
    {
        try
        {
            var activeAgents = await GetActiveByCompanyAsync(companyId);
            if (activeAgents.Count > 0)
            {
                return activeAgents.First().AIMessageLimit;
            }
        }
        catch (Exception e)
        {
            _logger.LogWarning("Failed to get AI agent limit for company {CompanyId}, using default: {Message}", companyId, e.Message);
        }

        return 10; // Default fallback
    }

    private static AIAgentDto ToDto(AIAgent agent)
    {
        return new AIAgentDto
        {
            Id = agent.Id,
            CompanyId = agent.CompanyId,
            Name = agent.Name,
            Description = agent.Description,
            AvatarBase64 = agent.AvatarBase64,
            AIModelId = agent.AIModelId,
            AIModelName = agent.AIModel?.Name ?? string.Empty,
            Temperament = agent.Temperament,
            MaxResponseLength = agent.MaxResponseLength,
            Temperature = agent.Temperature,
            AIMessageLimit = agent.AIMessageLimit,
            IsActive = agent.IsActive,
            CreatedAt = agent.CreatedAt,
            UpdatedAt = agent.UpdatedAt
        };
    }
}