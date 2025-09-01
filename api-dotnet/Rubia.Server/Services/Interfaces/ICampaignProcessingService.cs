using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface ICampaignProcessingService
{
    Task<CampaignDto> CreateCampaignAsync(CreateCampaignDto createCampaignDto, CancellationToken cancellationToken = default);
    Task<CampaignDto> UpdateCampaignAsync(Guid campaignId, UpdateCampaignDto updateCampaignDto, CancellationToken cancellationToken = default);
    Task<bool> DeleteCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<CampaignDto?> GetCampaignByIdAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<IEnumerable<CampaignDto>> GetCampaignsByCompanyAsync(Guid companyId, CancellationToken cancellationToken = default);
    
    Task<bool> StartCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<bool> PauseCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<bool> ResumeCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<bool> StopCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    
    Task<CampaignStatsDto> GetCampaignStatsAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task ProcessScheduledCampaignsAsync(CancellationToken cancellationToken = default);
    Task ProcessCampaignQueueAsync(Guid campaignId, CancellationToken cancellationToken = default);
    
    Task<bool> AddContactsToCampaignAsync(Guid campaignId, IEnumerable<Guid> customerIds, CancellationToken cancellationToken = default);
    Task<bool> RemoveContactsFromCampaignAsync(Guid campaignId, IEnumerable<Guid> campaignContactIds, CancellationToken cancellationToken = default);
    
    Task<bool> ValidateCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<CampaignPreviewDto> PreviewCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default);
    
    Task UpdateCampaignProgressAsync(Guid campaignId, CampaignContactStatus status, int count, CancellationToken cancellationToken = default);
}