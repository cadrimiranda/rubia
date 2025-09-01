using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface ICampaignContactService
{
    Task<CampaignContactDto> CreateCampaignContactAsync(CreateCampaignContactDto createDto, CancellationToken cancellationToken = default);
    Task<CampaignContactDto?> GetCampaignContactByIdAsync(Guid campaignContactId, CancellationToken cancellationToken = default);
    Task<IEnumerable<CampaignContactDto>> GetCampaignContactsAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<PagedResult<CampaignContactDto>> GetCampaignContactsPaginatedAsync(Guid campaignId, int page, int pageSize, string? status = null, CancellationToken cancellationToken = default);
    
    Task<bool> UpdateCampaignContactStatusAsync(Guid campaignContactId, string status, CancellationToken cancellationToken = default);
    Task<bool> DeleteCampaignContactAsync(Guid campaignContactId, CancellationToken cancellationToken = default);
    
    Task<IEnumerable<CampaignContactDto>> ImportContactsFromCsvAsync(Guid campaignId, Stream csvStream, CancellationToken cancellationToken = default);
    Task<IEnumerable<CampaignContactDto>> ImportContactsFromCustomersAsync(Guid campaignId, IEnumerable<Guid> customerIds, CancellationToken cancellationToken = default);
    Task<IEnumerable<CampaignContactDto>> ImportContactsFromCriteriaAsync(Guid campaignId, CustomerSearchCriteriaDto criteria, CancellationToken cancellationToken = default);
    
    Task<CampaignContactStatsDto> GetCampaignContactStatsAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task<IEnumerable<CampaignContactDto>> GetFailedCampaignContactsAsync(Guid campaignId, CancellationToken cancellationToken = default);
    
    Task<bool> RetryFailedContactAsync(Guid campaignContactId, CancellationToken cancellationToken = default);
    Task<int> RetryAllFailedContactsAsync(Guid campaignId, CancellationToken cancellationToken = default);
    
    Task<bool> ExcludeContactFromCampaignAsync(Guid campaignContactId, string reason, CancellationToken cancellationToken = default);
    Task<bool> ReincludeContactInCampaignAsync(Guid campaignContactId, CancellationToken cancellationToken = default);
}