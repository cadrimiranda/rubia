using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface ICampaignService
{
    Task<IEnumerable<CampaignDto>> GetAllAsync(Guid companyId);
    Task<CampaignDto?> GetByIdAsync(Guid campaignId);
    Task<CampaignDto> CreateAsync(CreateCampaignDto dto);
    Task<CampaignDto?> UpdateAsync(Guid campaignId, UpdateCampaignDto dto);
    Task<bool> DeleteAsync(Guid campaignId);
    Task<CampaignDto?> UpdateStatusAsync(Guid campaignId, CampaignStatus status);
    Task<int> AddContactsAsync(Guid campaignId, List<Guid> customerIds);
    Task<CampaignStatsDto> GetStatsAsync(Guid campaignId);
}