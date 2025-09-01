using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class CampaignProcessingService : ICampaignProcessingService
{
    private readonly RubiaDbContext _context;
    private readonly ICampaignMessagingService _campaignMessagingService;
    private readonly ILogger<CampaignProcessingService> _logger;

    public CampaignProcessingService(
        RubiaDbContext context,
        ICampaignMessagingService campaignMessagingService,
        ILogger<CampaignProcessingService> logger)
    {
        _context = context;
        _campaignMessagingService = campaignMessagingService;
        _logger = logger;
    }

    public async Task<CampaignDto> CreateCampaignAsync(CreateCampaignDto createCampaignDto, CancellationToken cancellationToken = default)
    {
        var campaign = new Campaign
        {
            Id = Guid.NewGuid(),
            CompanyId = createCampaignDto.CompanyId,
            MessageTemplateId = createCampaignDto.MessageTemplateId,
            Name = createCampaignDto.Name,
            Description = createCampaignDto.Description,
            Status = CampaignStatus.Draft.ToString(),
            ScheduledDate = createCampaignDto.ScheduledDate,
            TargetAudienceCriteria = createCampaignDto.TargetAudienceCriteria,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        _context.Campaigns.Add(campaign);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} created for company {CompanyId}", campaign.Id, campaign.CompanyId);

        return MapToDto(campaign);
    }

    public async Task<CampaignDto> UpdateCampaignAsync(Guid campaignId, UpdateCampaignDto updateCampaignDto, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            throw new ArgumentException($"Campaign {campaignId} not found");

        if (campaign.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Only draft campaigns can be updated");

        campaign.Name = updateCampaignDto.Name ?? campaign.Name;
        campaign.Description = updateCampaignDto.Description ?? campaign.Description;
        campaign.MessageTemplateId = updateCampaignDto.MessageTemplateId ?? campaign.MessageTemplateId;
        campaign.ScheduledDate = updateCampaignDto.ScheduledDate ?? campaign.ScheduledDate;
        campaign.TargetAudienceCriteria = updateCampaignDto.TargetAudienceCriteria ?? campaign.TargetAudienceCriteria;
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} updated", campaignId);

        return MapToDto(campaign);
    }

    public async Task<bool> DeleteCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            return false;

        if (campaign.Status == CampaignStatus.Running.ToString())
            throw new InvalidOperationException("Cannot delete a running campaign");

        campaign.IsActive = false;
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} marked as deleted", campaignId);

        return true;
    }

    public async Task<CampaignDto?> GetCampaignByIdAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns
            .Include(c => c.MessageTemplate)
            .Include(c => c.Company)
            .FirstOrDefaultAsync(c => c.Id == campaignId && c.IsActive, cancellationToken);

        return campaign != null ? MapToDto(campaign) : null;
    }

    public async Task<IEnumerable<CampaignDto>> GetCampaignsByCompanyAsync(Guid companyId, CancellationToken cancellationToken = default)
    {
        var campaigns = await _context.Campaigns
            .Include(c => c.MessageTemplate)
            .Where(c => c.CompanyId == companyId && c.IsActive)
            .OrderByDescending(c => c.CreatedAt)
            .ToListAsync(cancellationToken);

        return campaigns.Select(MapToDto);
    }

    public async Task<bool> StartCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns
            .Include(c => c.CampaignContacts)
            .FirstOrDefaultAsync(c => c.Id == campaignId, cancellationToken);

        if (campaign == null)
            return false;

        if (campaign.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Only draft campaigns can be started");

        if (!campaign.CampaignContacts.Any())
            throw new InvalidOperationException("Campaign must have contacts to start");

        campaign.Status = CampaignStatus.Running.ToString();
        campaign.StartDate = DateOnly.FromDateTime(DateTime.UtcNow);
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Start processing campaign queue
        _ = Task.Run(() => ProcessCampaignQueueAsync(campaignId, cancellationToken), cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} started", campaignId);

        return true;
    }

    public async Task<bool> PauseCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            return false;

        if (campaign.Status != CampaignStatus.Running.ToString())
            return false;

        campaign.Status = CampaignStatus.Paused.ToString();
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} paused", campaignId);

        return true;
    }

    public async Task<bool> ResumeCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            return false;

        if (campaign.Status != CampaignStatus.Paused.ToString())
            return false;

        campaign.Status = CampaignStatus.Running.ToString();
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Resume processing campaign queue
        _ = Task.Run(() => ProcessCampaignQueueAsync(campaignId, cancellationToken), cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} resumed", campaignId);

        return true;
    }

    public async Task<bool> StopCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            return false;

        campaign.Status = CampaignStatus.Completed.ToString();
        campaign.EndDate = DateOnly.FromDateTime(DateTime.UtcNow);
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign {CampaignId} stopped", campaignId);

        return true;
    }

    public async Task<CampaignStatsDto> GetCampaignStatsAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns
            .Include(c => c.CampaignContacts)
            .FirstOrDefaultAsync(c => c.Id == campaignId, cancellationToken);

        if (campaign == null)
            throw new ArgumentException($"Campaign {campaignId} not found");

        var contacts = campaign.CampaignContacts;
        
        return new CampaignStatsDto
        {
            CampaignId = campaignId,
            TotalContacts = contacts.Count,
            PendingCount = contacts.Count(c => c.Status == CampaignContactStatus.Pending.ToString()),
            SentCount = contacts.Count(c => c.Status == CampaignContactStatus.Sent.ToString()),
            DeliveredCount = contacts.Count(c => c.Status == CampaignContactStatus.Delivered.ToString()),
            ReadCount = contacts.Count(c => c.Status == CampaignContactStatus.Read.ToString()),
            RespondedCount = contacts.Count(c => c.Status == CampaignContactStatus.Responded.ToString()),
            FailedCount = contacts.Count(c => c.Status == CampaignContactStatus.Failed.ToString()),
            DeliveryRate = contacts.Any() ? (double)contacts.Count(c => c.Status == CampaignContactStatus.Delivered.ToString()) / contacts.Count : 0,
            ReadRate = contacts.Any() ? (double)contacts.Count(c => c.Status == CampaignContactStatus.Read.ToString()) / contacts.Count : 0,
            ResponseRate = contacts.Any() ? (double)contacts.Count(c => c.Status == CampaignContactStatus.Responded.ToString()) / contacts.Count : 0
        };
    }

    public async Task ProcessScheduledCampaignsAsync(CancellationToken cancellationToken = default)
    {
        var today = DateOnly.FromDateTime(DateTime.UtcNow);
        var scheduledCampaigns = await _context.Campaigns
            .Where(c => c.Status == CampaignStatus.Draft.ToString() 
                       && c.ScheduledDate.HasValue 
                       && c.ScheduledDate <= today
                       && c.IsActive)
            .ToListAsync(cancellationToken);

        foreach (var campaign in scheduledCampaigns)
        {
            try
            {
                await StartCampaignAsync(campaign.Id, cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error starting scheduled campaign {CampaignId}", campaign.Id);
            }
        }
    }

    public async Task ProcessCampaignQueueAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        const int batchSize = 50;
        const int delayBetweenMessages = 1000; // 1 second delay between messages

        var campaign = await _context.Campaigns
            .FirstOrDefaultAsync(c => c.Id == campaignId, cancellationToken);

        if (campaign == null || campaign.Status != CampaignStatus.Running.ToString())
            return;

        var pendingContacts = await _context.CampaignContacts
            .Include(cc => cc.Customer)
            .Where(cc => cc.CampaignId == campaignId && cc.Status == CampaignContactStatus.Pending.ToString())
            .Take(batchSize)
            .ToListAsync(cancellationToken);

        foreach (var contact in pendingContacts)
        {
            if (cancellationToken.IsCancellationRequested)
                break;

            // Check if campaign is still running
            campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
            if (campaign?.Status != CampaignStatus.Running.ToString())
                break;

            try
            {
                await _campaignMessagingService.SendCampaignMessageAsync(contact.Id, cancellationToken);
                await Task.Delay(delayBetweenMessages, cancellationToken);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error sending message to campaign contact {ContactId}", contact.Id);
            }
        }

        // Check if there are more pending contacts to process
        var remainingCount = await _context.CampaignContacts
            .CountAsync(cc => cc.CampaignId == campaignId && cc.Status == CampaignContactStatus.Pending.ToString(), cancellationToken);

        if (remainingCount > 0 && campaign?.Status == CampaignStatus.Running.ToString())
        {
            // Schedule next batch processing
            _ = Task.Delay(TimeSpan.FromMinutes(1), cancellationToken)
                .ContinueWith(_ => ProcessCampaignQueueAsync(campaignId, cancellationToken), cancellationToken);
        }
        else if (remainingCount == 0)
        {
            // Campaign completed
            await StopCampaignAsync(campaignId, cancellationToken);
        }
    }

    public async Task<bool> AddContactsToCampaignAsync(Guid campaignId, IEnumerable<Guid> customerIds, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null || campaign.Status != CampaignStatus.Draft.ToString())
            return false;

        var existingContactIds = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .Select(cc => cc.CustomerId)
            .ToHashSetAsync(cancellationToken);

        var newContacts = customerIds
            .Where(customerId => !existingContactIds.Contains(customerId))
            .Select(customerId => new CampaignContact
            {
                Id = Guid.NewGuid(),
                CampaignId = campaignId,
                CustomerId = customerId,
                Status = CampaignContactStatus.Pending.ToString(),
                CreatedAt = DateTime.UtcNow
            });

        _context.CampaignContacts.AddRange(newContacts);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Added {Count} contacts to campaign {CampaignId}", newContacts.Count(), campaignId);

        return true;
    }

    public async Task<bool> RemoveContactsFromCampaignAsync(Guid campaignId, IEnumerable<Guid> campaignContactIds, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null || campaign.Status != CampaignStatus.Draft.ToString())
            return false;

        var contactsToRemove = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId && campaignContactIds.Contains(cc.Id))
            .ToListAsync(cancellationToken);

        _context.CampaignContacts.RemoveRange(contactsToRemove);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Removed {Count} contacts from campaign {CampaignId}", contactsToRemove.Count, campaignId);

        return true;
    }

    public async Task<bool> ValidateCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns
            .Include(c => c.MessageTemplate)
            .Include(c => c.CampaignContacts)
            .FirstOrDefaultAsync(c => c.Id == campaignId, cancellationToken);

        if (campaign == null)
            return false;

        // Validate campaign has message template
        if (campaign.MessageTemplate == null)
            throw new InvalidOperationException("Campaign must have a message template");

        // Validate campaign has contacts
        if (!campaign.CampaignContacts.Any())
            throw new InvalidOperationException("Campaign must have at least one contact");

        // Validate company has active WhatsApp instance
        var hasActiveInstance = await _context.WhatsAppInstances
            .AnyAsync(w => w.CompanyId == campaign.CompanyId 
                          && w.IsActive 
                          && w.Status == WhatsAppInstanceStatus.Connected.ToString(), cancellationToken);

        if (!hasActiveInstance)
            throw new InvalidOperationException("Company must have an active WhatsApp instance");

        return true;
    }

    public async Task<CampaignPreviewDto> PreviewCampaignAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns
            .Include(c => c.MessageTemplate)
            .Include(c => c.CampaignContacts)
                .ThenInclude(cc => cc.Customer)
            .FirstOrDefaultAsync(c => c.Id == campaignId, cancellationToken);

        if (campaign == null)
            throw new ArgumentException($"Campaign {campaignId} not found");

        var sampleContacts = campaign.CampaignContacts
            .Take(5)
            .Select(cc => new CampaignContactPreviewDto
            {
                CustomerName = cc.Customer.Name,
                CustomerPhone = cc.Customer.Phone,
                MessageContent = campaign.MessageTemplate?.Content ?? "No template selected"
            })
            .ToList();

        return new CampaignPreviewDto
        {
            CampaignId = campaignId,
            CampaignName = campaign.Name,
            TotalContacts = campaign.CampaignContacts.Count,
            MessageTemplate = campaign.MessageTemplate?.Content ?? "No template selected",
            SampleContacts = sampleContacts
        };
    }

    public async Task UpdateCampaignProgressAsync(Guid campaignId, CampaignContactStatus status, int count, CancellationToken cancellationToken = default)
    {
        // This method can be used to update campaign progress in real-time
        // For now, we'll just log the progress
        _logger.LogInformation("Campaign {CampaignId} progress: {Status} = {Count}", campaignId, status, count);
    }

    private static CampaignDto MapToDto(Campaign campaign)
    {
        return new CampaignDto
        {
            Id = campaign.Id,
            CompanyId = campaign.CompanyId,
            MessageTemplateId = campaign.MessageTemplateId,
            Name = campaign.Name,
            Description = campaign.Description,
            Status = Enum.Parse<CampaignStatus>(campaign.Status),
            ScheduledDate = campaign.ScheduledDate,
            StartDate = campaign.StartDate,
            EndDate = campaign.EndDate,
            TargetAudienceCriteria = campaign.TargetAudienceCriteria,
            IsActive = campaign.IsActive,
            MessageTemplateName = campaign.MessageTemplate?.Name,
            CreatedAt = campaign.CreatedAt,
            UpdatedAt = campaign.UpdatedAt
        };
    }
}