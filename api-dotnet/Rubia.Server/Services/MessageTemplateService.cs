using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class MessageTemplateService : IMessageTemplateService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<MessageTemplateService> _logger;

    public MessageTemplateService(RubiaDbContext context, ILogger<MessageTemplateService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<MessageTemplateDto> CreateAsync(CreateMessageTemplateDto createDto, Guid? currentUserId = null)
    {
        _logger.LogInformation("Creating message template: {Name} for company: {CompanyId}", createDto.Name, createDto.CompanyId);

        // Validate company exists
        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == createDto.CompanyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        // Check if template name already exists for this company
        if (await ExistsByNameAndCompanyAsync(createDto.Name, createDto.CompanyId))
        {
            throw new ArgumentException($"Template com nome '{createDto.Name}' já existe nesta empresa");
        }

        // Validate AI Agent if provided
        if (createDto.AIAgentId.HasValue)
        {
            var aiAgent = await _context.AIAgents.FirstOrDefaultAsync(a => a.Id == createDto.AIAgentId.Value && a.CompanyId == createDto.CompanyId);
            if (aiAgent == null)
            {
                throw new ArgumentException("Agente de IA não encontrado ou não pertence a esta empresa");
            }
        }

        var template = new MessageTemplate
        {
            CompanyId = createDto.CompanyId,
            Name = createDto.Name,
            Content = createDto.Content,
            IsAIGenerated = createDto.IsAIGenerated,
            AIAgentId = createDto.AIAgentId,
            Tone = createDto.Tone?.ToUpper(),
            CreatedByUserId = currentUserId,
            EditCount = 0
        };

        _context.MessageTemplates.Add(template);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Message template created successfully with id: {Id}", template.Id);
        return await GetByIdAsync(template.Id, createDto.CompanyId);
    }

    public async Task<MessageTemplateDto> GetByIdAsync(Guid id, Guid companyId)
    {
        _logger.LogDebug("Fetching message template with id: {Id} for company: {CompanyId}", id, companyId);

        var template = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .FirstOrDefaultAsync(t => t.Id == id && t.CompanyId == companyId && t.DeletedAt == null);

        if (template == null)
        {
            throw new ArgumentException("Template de mensagem não encontrado");
        }

        return ToDto(template);
    }

    public async Task<List<MessageTemplateDto>> GetAllByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching all message templates for company: {CompanyId}", companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<List<MessageTemplateDto>> GetActiveByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching active message templates for company: {CompanyId}", companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId && t.DeletedAt == null)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<List<MessageTemplateDto>> GetByCreatedByUserAsync(Guid userId, Guid companyId)
    {
        _logger.LogDebug("Fetching message templates created by user: {UserId} for company: {CompanyId}", userId, companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId && t.CreatedByUserId == userId && t.DeletedAt == null)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<List<MessageTemplateDto>> GetByAIAgentAsync(Guid aiAgentId, Guid companyId)
    {
        _logger.LogDebug("Fetching message templates by AI agent: {AIAgentId} for company: {CompanyId}", aiAgentId, companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId && t.AIAgentId == aiAgentId && t.DeletedAt == null)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<List<MessageTemplateDto>> GetByToneAsync(string tone, Guid companyId)
    {
        _logger.LogDebug("Fetching message templates by tone: {Tone} for company: {CompanyId}", tone, companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId && t.Tone == tone.ToUpper() && t.DeletedAt == null)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<List<MessageTemplateDto>> GetAIGeneratedByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching AI-generated message templates for company: {CompanyId}", companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId && t.IsAIGenerated && t.DeletedAt == null)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<List<MessageTemplateDto>> GetManualByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Fetching manual message templates for company: {CompanyId}", companyId);

        var templates = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .Where(t => t.CompanyId == companyId && !t.IsAIGenerated && t.DeletedAt == null)
            .OrderBy(t => t.Name)
            .ToListAsync();

        return templates.Select(ToDto).ToList();
    }

    public async Task<MessageTemplateDto> UpdateAsync(Guid id, UpdateMessageTemplateDto updateDto, Guid companyId, Guid? currentUserId = null)
    {
        _logger.LogInformation("Updating message template with id: {Id} for company: {CompanyId}", id, companyId);

        var template = await _context.MessageTemplates
            .Include(t => t.Company)
            .Include(t => t.CreatedByUser)
            .Include(t => t.AIAgent)
            .Include(t => t.LastEditedByUser)
            .FirstOrDefaultAsync(t => t.Id == id && t.CompanyId == companyId && t.DeletedAt == null);

        if (template == null)
        {
            throw new ArgumentException("Template de mensagem não encontrado");
        }

        if (updateDto.Name != null && updateDto.Name != template.Name)
        {
            if (await ExistsByNameAndCompanyAsync(updateDto.Name, companyId))
            {
                throw new ArgumentException($"Template com nome '{updateDto.Name}' já existe nesta empresa");
            }
            template.Name = updateDto.Name;
        }

        if (updateDto.Content != null) template.Content = updateDto.Content;
        if (updateDto.Tone != null) template.Tone = updateDto.Tone.ToUpper();

        template.LastEditedByUserId = currentUserId;
        template.EditCount++;

        await _context.SaveChangesAsync();
        _logger.LogInformation("Message template updated successfully");

        // Reload to get updated related entities
        await _context.Entry(template).ReloadAsync();
        await _context.Entry(template).Reference(t => t.Company).LoadAsync();
        await _context.Entry(template).Reference(t => t.CreatedByUser).LoadAsync();
        await _context.Entry(template).Reference(t => t.AIAgent).LoadAsync();
        await _context.Entry(template).Reference(t => t.LastEditedByUser).LoadAsync();

        return ToDto(template);
    }

    public async Task SoftDeleteAsync(Guid id, Guid companyId)
    {
        _logger.LogInformation("Soft deleting message template with id: {Id} for company: {CompanyId}", id, companyId);

        var template = await _context.MessageTemplates
            .FirstOrDefaultAsync(t => t.Id == id && t.CompanyId == companyId && t.DeletedAt == null);

        if (template == null)
        {
            throw new ArgumentException("Template de mensagem não encontrado");
        }

        template.DeletedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        _logger.LogInformation("Message template soft deleted successfully");
    }

    public async Task DeleteAsync(Guid id, Guid companyId)
    {
        _logger.LogInformation("Deleting message template with id: {Id} for company: {CompanyId}", id, companyId);

        var template = await _context.MessageTemplates
            .FirstOrDefaultAsync(t => t.Id == id && t.CompanyId == companyId);

        if (template == null)
        {
            throw new ArgumentException("Template de mensagem não encontrado");
        }

        _context.MessageTemplates.Remove(template);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Message template deleted successfully");
    }

    public async Task<long> CountByCompanyAsync(Guid companyId)
    {
        return await _context.MessageTemplates
            .Where(t => t.CompanyId == companyId)
            .CountAsync();
    }

    public async Task<long> CountActiveByCompanyAsync(Guid companyId)
    {
        return await _context.MessageTemplates
            .Where(t => t.CompanyId == companyId && t.DeletedAt == null)
            .CountAsync();
    }

    public async Task<long> CountAIGeneratedByCompanyAsync(Guid companyId)
    {
        return await _context.MessageTemplates
            .Where(t => t.CompanyId == companyId && t.IsAIGenerated && t.DeletedAt == null)
            .CountAsync();
    }

    public async Task<bool> ExistsByNameAndCompanyAsync(string name, Guid companyId)
    {
        return await _context.MessageTemplates
            .AnyAsync(t => t.Name == name && t.CompanyId == companyId && t.DeletedAt == null);
    }

    private static MessageTemplateDto ToDto(MessageTemplate template)
    {
        return new MessageTemplateDto
        {
            Id = template.Id,
            CompanyId = template.CompanyId,
            CompanyName = template.Company?.Name ?? string.Empty,
            Name = template.Name,
            Content = template.Content,
            IsAIGenerated = template.IsAIGenerated,
            CreatedByUserId = template.CreatedByUserId,
            CreatedByUserName = template.CreatedByUser?.Name ?? string.Empty,
            AIAgentId = template.AIAgentId,
            AIAgentName = template.AIAgent?.Name ?? string.Empty,
            Tone = template.Tone,
            LastEditedByUserId = template.LastEditedByUserId,
            LastEditedByUserName = template.LastEditedByUser?.Name ?? string.Empty,
            EditCount = template.EditCount,
            CreatedAt = template.CreatedAt,
            UpdatedAt = template.UpdatedAt,
            DeletedAt = template.DeletedAt
        };
    }
}