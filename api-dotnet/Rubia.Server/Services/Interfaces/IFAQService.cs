using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IFAQService
{
    Task<IEnumerable<FAQDto>> GetAllAsync(Guid companyId);
    Task<FAQDto?> GetByIdAsync(Guid faqId);
    Task<FAQDto> CreateAsync(CreateFAQDto dto);
    Task<FAQDto?> UpdateAsync(Guid faqId, UpdateFAQDto dto);
    Task<bool> DeleteAsync(Guid faqId);
    Task<IEnumerable<FAQMatchDto>> SearchAsync(Guid companyId, string query);
    Task IncrementUsageAsync(Guid faqId);
    Task<FAQStatsDto> GetStatsAsync(Guid companyId);
}