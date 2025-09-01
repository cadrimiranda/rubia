using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IMessageTemplateService
{
    Task<MessageTemplateDto> CreateAsync(CreateMessageTemplateDto createDto, Guid? currentUserId = null);
    Task<MessageTemplateDto> GetByIdAsync(Guid id, Guid companyId);
    Task<List<MessageTemplateDto>> GetAllByCompanyAsync(Guid companyId);
    Task<List<MessageTemplateDto>> GetActiveByCompanyAsync(Guid companyId);
    Task<List<MessageTemplateDto>> GetByCreatedByUserAsync(Guid userId, Guid companyId);
    Task<List<MessageTemplateDto>> GetByAIAgentAsync(Guid aiAgentId, Guid companyId);
    Task<List<MessageTemplateDto>> GetByToneAsync(string tone, Guid companyId);
    Task<List<MessageTemplateDto>> GetAIGeneratedByCompanyAsync(Guid companyId);
    Task<List<MessageTemplateDto>> GetManualByCompanyAsync(Guid companyId);
    Task<MessageTemplateDto> UpdateAsync(Guid id, UpdateMessageTemplateDto updateDto, Guid companyId, Guid? currentUserId = null);
    Task SoftDeleteAsync(Guid id, Guid companyId);
    Task DeleteAsync(Guid id, Guid companyId);
    Task<long> CountByCompanyAsync(Guid companyId);
    Task<long> CountActiveByCompanyAsync(Guid companyId);
    Task<long> CountAIGeneratedByCompanyAsync(Guid companyId);
    Task<bool> ExistsByNameAndCompanyAsync(string name, Guid companyId);
}