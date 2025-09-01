using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface ICampaignMessagingService
{
    Task<bool> SendCampaignMessageAsync(Guid campaignContactId, CancellationToken cancellationToken = default);
    Task<bool> SendBulkCampaignMessagesAsync(IEnumerable<Guid> campaignContactIds, CancellationToken cancellationToken = default);
    Task<MessageDto> ProcessCampaignMessageTemplateAsync(Guid campaignContactId, string templateContent, CancellationToken cancellationToken = default);
    Task<bool> ScheduleCampaignMessageAsync(Guid campaignContactId, DateTime scheduledTime, CancellationToken cancellationToken = default);
    Task<CampaignMessageStatsDto> GetCampaignMessageStatsAsync(Guid campaignId, CancellationToken cancellationToken = default);
    Task UpdateCampaignContactStatusAsync(Guid campaignContactId, string status, string? errorMessage = null, CancellationToken cancellationToken = default);
    Task<bool> RetryCampaignMessageAsync(Guid campaignContactId, CancellationToken cancellationToken = default);
}