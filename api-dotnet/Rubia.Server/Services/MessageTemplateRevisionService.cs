using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class MessageTemplateRevisionService : IMessageTemplateRevisionService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<MessageTemplateRevisionService> _logger;

    public MessageTemplateRevisionService(RubiaDbContext context, ILogger<MessageTemplateRevisionService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<MessageTemplateRevisionDto?> GetByIdAsync(Guid id)
    {
        var revision = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .FirstOrDefaultAsync(r => r.Id == id);

        return revision != null ? MapToDto(revision) : null;
    }

    public async Task<IEnumerable<MessageTemplateRevisionDto>> GetAllAsync()
    {
        var revisions = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return revisions.Select(MapToDto);
    }

    public async Task<MessageTemplateRevisionDto> CreateAsync(CreateMessageTemplateRevisionDto createDto)
    {
        var template = await _context.MessageTemplates.FindAsync(createDto.TemplateId);
        if (template == null)
        {
            throw new ArgumentException("Template not found");
        }

        var nextRevisionNumber = await GetNextRevisionNumberAsync(createDto.TemplateId);

        var revision = new MessageTemplateRevision
        {
            Id = Guid.NewGuid(),
            TemplateId = createDto.TemplateId,
            RevisionNumber = nextRevisionNumber,
            Content = createDto.Content,
            RevisionType = createDto.RevisionType,
            EditedByUserId = createDto.EditedByUserId,
            RevisionTimestamp = DateTime.UtcNow,
            AiAgentId = createDto.AiAgentId,
            AiEnhancementType = createDto.AiEnhancementType,
            AiTokensUsed = createDto.AiTokensUsed,
            AiCreditsConsumed = createDto.AiCreditsConsumed,
            AiModelUsed = createDto.AiModelUsed,
            AiExplanation = createDto.AiExplanation,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.MessageTemplateRevisions.Add(revision);
        await _context.SaveChangesAsync();

        return await GetByIdAsync(revision.Id) ?? throw new InvalidOperationException("Failed to retrieve created revision");
    }

    public async Task<MessageTemplateRevisionDto?> UpdateAsync(Guid id, UpdateMessageTemplateRevisionDto updateDto)
    {
        var revision = await _context.MessageTemplateRevisions.FindAsync(id);
        if (revision == null)
            return null;

        if (!string.IsNullOrEmpty(updateDto.Content))
            revision.Content = updateDto.Content;

        if (updateDto.RevisionType.HasValue)
            revision.RevisionType = updateDto.RevisionType.Value;

        if (!string.IsNullOrEmpty(updateDto.AiEnhancementType))
            revision.AiEnhancementType = updateDto.AiEnhancementType;

        if (updateDto.AiTokensUsed.HasValue)
            revision.AiTokensUsed = updateDto.AiTokensUsed;

        if (updateDto.AiCreditsConsumed.HasValue)
            revision.AiCreditsConsumed = updateDto.AiCreditsConsumed;

        if (!string.IsNullOrEmpty(updateDto.AiModelUsed))
            revision.AiModelUsed = updateDto.AiModelUsed;

        if (!string.IsNullOrEmpty(updateDto.AiExplanation))
            revision.AiExplanation = updateDto.AiExplanation;

        revision.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return await GetByIdAsync(id);
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var revision = await _context.MessageTemplateRevisions.FindAsync(id);
        if (revision == null)
            return false;

        _context.MessageTemplateRevisions.Remove(revision);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<IEnumerable<MessageTemplateRevisionDto>> GetByTemplateIdAsync(Guid templateId)
    {
        var revisions = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .Where(r => r.TemplateId == templateId)
            .OrderByDescending(r => r.RevisionNumber)
            .ToListAsync();

        return revisions.Select(MapToDto);
    }

    public async Task<long> CountByTemplateIdAsync(Guid templateId)
    {
        return await _context.MessageTemplateRevisions
            .Where(r => r.TemplateId == templateId)
            .CountAsync();
    }

    public async Task<IEnumerable<MessageTemplateRevisionDto>> GetByEditedByUserIdAsync(Guid userId)
    {
        var revisions = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .Where(r => r.EditedByUserId == userId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        return revisions.Select(MapToDto);
    }

    public async Task<MessageTemplateRevisionDto?> GetByTemplateIdAndRevisionNumberAsync(Guid templateId, int revisionNumber)
    {
        var revision = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .FirstOrDefaultAsync(r => r.TemplateId == templateId && r.RevisionNumber == revisionNumber);

        return revision != null ? MapToDto(revision) : null;
    }

    public async Task<IEnumerable<MessageTemplateRevisionDto>> GetByTemplateIdOrderByRevisionNumberDescAsync(Guid templateId)
    {
        return await GetByTemplateIdAsync(templateId); // Already ordered by revision number desc
    }

    public async Task<MessageTemplateRevisionDto?> GetLatestRevisionAsync(Guid templateId)
    {
        var revision = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .Where(r => r.TemplateId == templateId)
            .OrderByDescending(r => r.RevisionNumber)
            .FirstOrDefaultAsync();

        return revision != null ? MapToDto(revision) : null;
    }

    public async Task<MessageTemplateRevisionDto?> GetOriginalRevisionAsync(Guid templateId)
    {
        var revision = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .Where(r => r.TemplateId == templateId)
            .OrderBy(r => r.RevisionNumber)
            .FirstOrDefaultAsync();

        return revision != null ? MapToDto(revision) : null;
    }

    public async Task<bool> ExistsByTemplateIdAndRevisionNumberAsync(Guid templateId, int revisionNumber)
    {
        return await _context.MessageTemplateRevisions
            .AnyAsync(r => r.TemplateId == templateId && r.RevisionNumber == revisionNumber);
    }

    public async Task<IEnumerable<MessageTemplateRevisionDto>> GetRevisionsBetweenNumbersAsync(Guid templateId, int minRevision, int maxRevision)
    {
        var revisions = await _context.MessageTemplateRevisions
            .Include(r => r.Template)
            .Include(r => r.EditedBy)
            .Include(r => r.AiAgent)
            .Where(r => r.TemplateId == templateId && r.RevisionNumber >= minRevision && r.RevisionNumber <= maxRevision)
            .OrderBy(r => r.RevisionNumber)
            .ToListAsync();

        return revisions.Select(MapToDto);
    }

    public async Task<int> GetNextRevisionNumberAsync(Guid templateId)
    {
        var lastRevision = await _context.MessageTemplateRevisions
            .Where(r => r.TemplateId == templateId)
            .OrderByDescending(r => r.RevisionNumber)
            .FirstOrDefaultAsync();

        return (lastRevision?.RevisionNumber ?? 0) + 1;
    }

    public async Task<MessageTemplateRevisionDto> CreateRevisionFromTemplateAsync(Guid templateId, string content, Guid editedByUserId)
    {
        var createDto = new CreateMessageTemplateRevisionDto
        {
            TemplateId = templateId,
            Content = content,
            RevisionType = RevisionType.MANUAL,
            EditedByUserId = editedByUserId
        };

        return await CreateAsync(createDto);
    }

    private static MessageTemplateRevisionDto MapToDto(MessageTemplateRevision revision)
    {
        return new MessageTemplateRevisionDto
        {
            Id = revision.Id,
            TemplateId = revision.TemplateId,
            TemplateName = revision.Template?.Name,
            RevisionNumber = revision.RevisionNumber,
            Content = revision.Content,
            EditedByUserId = revision.EditedByUserId,
            EditedByUserName = revision.EditedBy?.Name,
            RevisionType = revision.RevisionType,
            RevisionTimestamp = revision.RevisionTimestamp,
            CreatedAt = revision.CreatedAt,
            UpdatedAt = revision.UpdatedAt,
            AiAgentId = revision.AiAgentId,
            AiAgentName = revision.AiAgent?.Name,
            AiEnhancementType = revision.AiEnhancementType,
            AiTokensUsed = revision.AiTokensUsed,
            AiCreditsConsumed = revision.AiCreditsConsumed,
            AiModelUsed = revision.AiModelUsed,
            AiExplanation = revision.AiExplanation
        };
    }
}