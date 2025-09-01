using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IMessageDraftService
{
    Task<IEnumerable<MessageDraftDto>> GetUserDraftsAsync(Guid userId, Guid? conversationId = null);
    Task<MessageDraftDto?> GetByIdAsync(Guid draftId);
    Task<MessageDraftDto> CreateOrUpdateAsync(CreateMessageDraftDto dto);
    Task<MessageDraftDto?> UpdateAsync(Guid draftId, UpdateMessageDraftDto dto);
    Task<bool> DeleteAsync(Guid draftId);
    Task<int> DeleteUserDraftsAsync(Guid userId, Guid? conversationId = null);
    Task<IEnumerable<MessageDraftDto>> GetTemplatesAsync(Guid userId);
    Task<MessageDraftDto?> SaveAsTemplateAsync(Guid draftId, string templateName);
    Task CleanupOldDraftsAsync(TimeSpan maxAge);
}