using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class CampaignService : ICampaignService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<CampaignService> _logger;

    public CampaignService(RubiaDbContext context, ILogger<CampaignService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<IEnumerable<CampaignDto>> GetAllAsync(Guid companyId)
    {
        var campaigns = await _context.Campaigns
            .Include(c => c.CampaignContacts)
            .Include(c => c.InitialMessageTemplate)
            .Where(c => c.CompanyId == companyId && c.IsActive)
            .OrderByDescending(c => c.CreatedAt)
            .ToListAsync();

        return campaigns.Select(MapToDto);
    }

    public async Task<CampaignDto?> GetByIdAsync(Guid campaignId)
    {
        var campaign = await _context.Campaigns
            .Include(c => c.CampaignContacts)
                .ThenInclude(cc => cc.Customer)
            .Include(c => c.InitialMessageTemplate)
            .FirstOrDefaultAsync(c => c.Id == campaignId);

        return campaign != null ? MapToDto(campaign) : null;
    }

    public async Task<CampaignDto> CreateAsync(CreateCampaignDto dto)
    {
        var campaign = new Campaign
        {
            Id = Guid.NewGuid(),
            CompanyId = dto.CompanyId,
            Name = dto.Name,
            Description = dto.Description,
            Status = CampaignStatus.DRAFT,
            StartDate = dto.StartDate,
            EndDate = dto.EndDate,
            MessageTemplateId = dto.MessageTemplateId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Campaigns.Add(campaign);
        await _context.SaveChangesAsync();

        return await GetByIdAsync(campaign.Id) ?? throw new InvalidOperationException("Failed to create campaign");
    }

    public async Task<CampaignDto?> UpdateAsync(Guid campaignId, UpdateCampaignDto dto)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId);
        if (campaign == null)
            return null;

        campaign.Name = dto.Name ?? campaign.Name;
        campaign.Description = dto.Description ?? campaign.Description;
        campaign.StartDate = dto.StartDate ?? campaign.StartDate;
        campaign.EndDate = dto.EndDate ?? campaign.EndDate;
        campaign.MessageTemplateId = dto.MessageTemplateId ?? campaign.MessageTemplateId;
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();

        return await GetByIdAsync(campaignId);
    }

    public async Task<bool> DeleteAsync(Guid campaignId)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId);
        if (campaign == null)
            return false;

        campaign.IsActive = false;
        campaign.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<CampaignDto?> UpdateStatusAsync(Guid campaignId, CampaignStatus status)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId);
        if (campaign == null)
            return null;

        campaign.Status = status;
        campaign.UpdatedAt = DateTime.UtcNow;

        if (status == CampaignStatus.ACTIVE)
        {
            campaign.StartDate ??= DateTime.UtcNow;
        }
        else if (status == CampaignStatus.COMPLETED)
        {
            campaign.EndDate ??= DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();

        return await GetByIdAsync(campaignId);
    }

    public async Task<int> AddContactsAsync(Guid campaignId, List<Guid> customerIds)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId);
        if (campaign == null)
            throw new ArgumentException("Campaign not found");

        var existingContactIds = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .Select(cc => cc.CustomerId)
            .ToListAsync();

        var newCustomerIds = customerIds.Except(existingContactIds).ToList();
        
        var contacts = newCustomerIds.Select(customerId => new CampaignContact
        {
            Id = Guid.NewGuid(),
            CampaignId = campaignId,
            CustomerId = customerId,
            Status = CampaignContactStatus.PENDING,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        }).ToList();

        _context.CampaignContacts.AddRange(contacts);
        await _context.SaveChangesAsync();

        return contacts.Count;
    }

    public async Task<CampaignStatsDto> GetStatsAsync(Guid campaignId)
    {
        var stats = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .GroupBy(cc => cc.Status)
            .Select(g => new { Status = g.Key, Count = g.Count() })
            .ToListAsync();

        var statsDict = stats.ToDictionary(s => s.Status, s => s.Count);

        return new CampaignStatsDto
        {
            CampaignId = campaignId,
            TotalContacts = statsDict.Values.Sum(),
            PendingContacts = statsDict.GetValueOrDefault(CampaignContactStatus.PENDING, 0),
            SentContacts = statsDict.GetValueOrDefault(CampaignContactStatus.SENT, 0),
            DeliveredContacts = statsDict.GetValueOrDefault(CampaignContactStatus.DELIVERED, 0),
            FailedContacts = statsDict.GetValueOrDefault(CampaignContactStatus.FAILED, 0)
        };
    }

    private static CampaignDto MapToDto(Campaign campaign)
    {
        return new CampaignDto
        {
            Id = campaign.Id,
            CompanyId = campaign.CompanyId,
            Name = campaign.Name,
            Description = campaign.Description,
            Status = campaign.Status,
            StartDate = campaign.StartDate,
            EndDate = campaign.EndDate,
            MessageTemplateId = campaign.MessageTemplateId,
            MessageTemplateName = campaign.InitialMessageTemplate?.Name,
            TotalContacts = campaign.CampaignContacts?.Count ?? 0,
            CreatedAt = campaign.CreatedAt,
            UpdatedAt = campaign.UpdatedAt
        };
    }
}