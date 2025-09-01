using Rubia.Server.DTOs;
using Rubia.Server.Entities;

namespace Rubia.Server.Services.Interfaces;

public interface IMessageTemplateRevisionService
{
    Task<MessageTemplateRevisionDto?> GetByIdAsync(Guid id);
    Task<IEnumerable<MessageTemplateRevisionDto>> GetAllAsync();
    Task<MessageTemplateRevisionDto> CreateAsync(CreateMessageTemplateRevisionDto createDto);
    Task<MessageTemplateRevisionDto?> UpdateAsync(Guid id, UpdateMessageTemplateRevisionDto updateDto);
    Task<bool> DeleteAsync(Guid id);
    
    // Template-specific methods
    Task<IEnumerable<MessageTemplateRevisionDto>> GetByTemplateIdAsync(Guid templateId);
    Task<long> CountByTemplateIdAsync(Guid templateId);
    Task<IEnumerable<MessageTemplateRevisionDto>> GetByEditedByUserIdAsync(Guid userId);
    Task<MessageTemplateRevisionDto?> GetByTemplateIdAndRevisionNumberAsync(Guid templateId, int revisionNumber);
    Task<IEnumerable<MessageTemplateRevisionDto>> GetByTemplateIdOrderByRevisionNumberDescAsync(Guid templateId);
    Task<MessageTemplateRevisionDto?> GetLatestRevisionAsync(Guid templateId);
    Task<MessageTemplateRevisionDto?> GetOriginalRevisionAsync(Guid templateId);
    Task<bool> ExistsByTemplateIdAndRevisionNumberAsync(Guid templateId, int revisionNumber);
    Task<IEnumerable<MessageTemplateRevisionDto>> GetRevisionsBetweenNumbersAsync(Guid templateId, int minRevision, int maxRevision);
    Task<int> GetNextRevisionNumberAsync(Guid templateId);
    Task<MessageTemplateRevisionDto> CreateRevisionFromTemplateAsync(Guid templateId, string content, Guid editedByUserId);
}