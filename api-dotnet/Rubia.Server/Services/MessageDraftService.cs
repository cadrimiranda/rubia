using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class MessageDraftService : IMessageDraftService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<MessageDraftService> _logger;

    public MessageDraftService(RubiaDbContext context, ILogger<MessageDraftService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<IEnumerable<MessageDraftDto>> GetUserDraftsAsync(Guid userId, Guid? conversationId = null)
    {
        var query = _context.MessageDrafts
            .Where(md => md.UserId == userId);

        if (conversationId.HasValue)
        {
            query = query.Where(md => md.ConversationId == conversationId);
        }

        var drafts = await query
            .OrderByDescending(md => md.UpdatedAt)
            .ToListAsync();

        return drafts.Select(MapToDto);
    }

    public async Task<MessageDraftDto?> GetByIdAsync(Guid draftId)
    {
        var draft = await _context.MessageDrafts
            .Include(md => md.Conversation)
            .FirstOrDefaultAsync(md => md.Id == draftId);

        return draft != null ? MapToDto(draft) : null;
    }

    public async Task<MessageDraftDto> CreateOrUpdateAsync(CreateMessageDraftDto dto)
    {
        // Check if there's already an auto-save draft for this conversation
        var existingDraft = await _context.MessageDrafts
            .FirstOrDefaultAsync(md => md.UserId == dto.UserId 
                                     && md.ConversationId == dto.ConversationId 
                                     && md.AutoSave 
                                     && !md.IsTemplate);

        if (existingDraft != null)
        {
            // Update existing draft
            existingDraft.Content = dto.Content;
            existingDraft.DraftType = dto.DraftType;
            existingDraft.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();
            return MapToDto(existingDraft);
        }

        // Create new draft
        var draft = new MessageDraft
        {
            Id = Guid.NewGuid(),
            UserId = dto.UserId,
            ConversationId = dto.ConversationId,
            Content = dto.Content,
            DraftType = dto.DraftType,
            IsTemplate = dto.IsTemplate,
            TemplateName = dto.TemplateName,
            AutoSave = dto.AutoSave,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.MessageDrafts.Add(draft);
        await _context.SaveChangesAsync();

        return MapToDto(draft);
    }

    public async Task<MessageDraftDto?> UpdateAsync(Guid draftId, UpdateMessageDraftDto dto)
    {
        var draft = await _context.MessageDrafts.FindAsync(draftId);
        if (draft == null)
            return null;

        draft.Content = dto.Content ?? draft.Content;
        draft.DraftType = dto.DraftType ?? draft.DraftType;
        draft.IsTemplate = dto.IsTemplate ?? draft.IsTemplate;
        draft.TemplateName = dto.TemplateName ?? draft.TemplateName;
        draft.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return MapToDto(draft);
    }

    public async Task<bool> DeleteAsync(Guid draftId)
    {
        var draft = await _context.MessageDrafts.FindAsync(draftId);
        if (draft == null)
            return false;

        _context.MessageDrafts.Remove(draft);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<int> DeleteUserDraftsAsync(Guid userId, Guid? conversationId = null)
    {
        var query = _context.MessageDrafts.Where(md => md.UserId == userId);

        if (conversationId.HasValue)
        {
            query = query.Where(md => md.ConversationId == conversationId);
        }

        var drafts = await query.ToListAsync();
        _context.MessageDrafts.RemoveRange(drafts);
        await _context.SaveChangesAsync();

        return drafts.Count;
    }

    public async Task<IEnumerable<MessageDraftDto>> GetTemplatesAsync(Guid userId)
    {
        var templates = await _context.MessageDrafts
            .Where(md => md.UserId == userId && md.IsTemplate)
            .OrderBy(md => md.TemplateName)
            .ToListAsync();

        return templates.Select(MapToDto);
    }

    public async Task<MessageDraftDto?> SaveAsTemplateAsync(Guid draftId, string templateName)
    {
        var draft = await _context.MessageDrafts.FindAsync(draftId);
        if (draft == null)
            return null;

        // Create a new template from the draft
        var template = new MessageDraft
        {
            Id = Guid.NewGuid(),
            UserId = draft.UserId,
            Content = draft.Content,
            DraftType = draft.DraftType,
            IsTemplate = true,
            TemplateName = templateName,
            AutoSave = false,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.MessageDrafts.Add(template);
        await _context.SaveChangesAsync();

        return MapToDto(template);
    }

    public async Task CleanupOldDraftsAsync(TimeSpan maxAge)
    {
        var cutoffDate = DateTime.UtcNow - maxAge;
        
        var oldDrafts = await _context.MessageDrafts
            .Where(md => md.UpdatedAt < cutoffDate && md.AutoSave && !md.IsTemplate)
            .ToListAsync();

        if (oldDrafts.Any())
        {
            _context.MessageDrafts.RemoveRange(oldDrafts);
            await _context.SaveChangesAsync();
            
            _logger.LogInformation("Cleaned up {Count} old auto-save drafts", oldDrafts.Count);
        }
    }

    private static MessageDraftDto MapToDto(MessageDraft draft)
    {
        return new MessageDraftDto
        {
            Id = draft.Id,
            UserId = draft.UserId,
            ConversationId = draft.ConversationId,
            Content = draft.Content,
            DraftType = draft.DraftType,
            IsTemplate = draft.IsTemplate,
            TemplateName = draft.TemplateName,
            AutoSave = draft.AutoSave,
            CreatedAt = draft.CreatedAt,
            UpdatedAt = draft.UpdatedAt
        };
    }
}