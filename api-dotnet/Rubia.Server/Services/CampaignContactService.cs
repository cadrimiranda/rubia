using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;
using System.Globalization;
using System.Text;

namespace Rubia.Server.Services;

public class CampaignContactService : ICampaignContactService
{
    private readonly RubiaDbContext _context;
    private readonly ICampaignMessagingService _campaignMessagingService;
    private readonly ILogger<CampaignContactService> _logger;

    public CampaignContactService(
        RubiaDbContext context,
        ICampaignMessagingService campaignMessagingService,
        ILogger<CampaignContactService> logger)
    {
        _context = context;
        _campaignMessagingService = campaignMessagingService;
        _logger = logger;
    }

    public async Task<CampaignContactDto> CreateCampaignContactAsync(CreateCampaignContactDto createDto, CancellationToken cancellationToken = default)
    {
        // Validate campaign exists and is in draft status
        var campaign = await _context.Campaigns.FindAsync(createDto.CampaignId, cancellationToken);
        if (campaign == null)
            throw new ArgumentException($"Campaign {createDto.CampaignId} not found");

        if (campaign.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Can only add contacts to draft campaigns");

        // Validate customer exists
        var customer = await _context.Customers.FindAsync(createDto.CustomerId, cancellationToken);
        if (customer == null)
            throw new ArgumentException($"Customer {createDto.CustomerId} not found");

        // Check if contact already exists in campaign
        var existingContact = await _context.CampaignContacts
            .FirstOrDefaultAsync(cc => cc.CampaignId == createDto.CampaignId && cc.CustomerId == createDto.CustomerId, cancellationToken);

        if (existingContact != null)
            throw new InvalidOperationException("Customer is already in this campaign");

        var campaignContact = new CampaignContact
        {
            Id = Guid.NewGuid(),
            CampaignId = createDto.CampaignId,
            CustomerId = createDto.CustomerId,
            Status = CampaignContactStatus.Pending.ToString(),
            CreatedAt = DateTime.UtcNow
        };

        _context.CampaignContacts.Add(campaignContact);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign contact created: {ContactId} for campaign {CampaignId}", 
            campaignContact.Id, createDto.CampaignId);

        return await MapToDtoAsync(campaignContact, cancellationToken);
    }

    public async Task<CampaignContactDto?> GetCampaignContactByIdAsync(Guid campaignContactId, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts
            .Include(cc => cc.Customer)
            .Include(cc => cc.Campaign)
            .FirstOrDefaultAsync(cc => cc.Id == campaignContactId, cancellationToken);

        return campaignContact != null ? await MapToDtoAsync(campaignContact, cancellationToken) : null;
    }

    public async Task<IEnumerable<CampaignContactDto>> GetCampaignContactsAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var campaignContacts = await _context.CampaignContacts
            .Include(cc => cc.Customer)
            .Include(cc => cc.Campaign)
            .Where(cc => cc.CampaignId == campaignId)
            .OrderBy(cc => cc.CreatedAt)
            .ToListAsync(cancellationToken);

        var tasks = campaignContacts.Select(cc => MapToDtoAsync(cc, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<PagedResult<CampaignContactDto>> GetCampaignContactsPaginatedAsync(Guid campaignId, int page, int pageSize, string? status = null, CancellationToken cancellationToken = default)
    {
        var query = _context.CampaignContacts
            .Include(cc => cc.Customer)
            .Include(cc => cc.Campaign)
            .Where(cc => cc.CampaignId == campaignId);

        if (!string.IsNullOrEmpty(status))
        {
            query = query.Where(cc => cc.Status == status);
        }

        var totalCount = await query.CountAsync(cancellationToken);
        
        var campaignContacts = await query
            .OrderBy(cc => cc.CreatedAt)
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToListAsync(cancellationToken);

        var tasks = campaignContacts.Select(cc => MapToDtoAsync(cc, cancellationToken));
        var items = await Task.WhenAll(tasks);

        return new PagedResult<CampaignContactDto>
        {
            Items = items,
            TotalCount = totalCount,
            Page = page,
            PageSize = pageSize,
            TotalPages = (int)Math.Ceiling((double)totalCount / pageSize)
        };
    }

    public async Task<bool> UpdateCampaignContactStatusAsync(Guid campaignContactId, string status, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts.FindAsync(campaignContactId, cancellationToken);
        if (campaignContact == null)
            return false;

        await _campaignMessagingService.UpdateCampaignContactStatusAsync(campaignContactId, status, cancellationToken: cancellationToken);

        _logger.LogInformation("Campaign contact {ContactId} status updated to {Status}", campaignContactId, status);

        return true;
    }

    public async Task<bool> DeleteCampaignContactAsync(Guid campaignContactId, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts
            .Include(cc => cc.Campaign)
            .FirstOrDefaultAsync(cc => cc.Id == campaignContactId, cancellationToken);

        if (campaignContact == null)
            return false;

        if (campaignContact.Campaign!.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Can only delete contacts from draft campaigns");

        _context.CampaignContacts.Remove(campaignContact);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign contact {ContactId} deleted", campaignContactId);

        return true;
    }

    public async Task<IEnumerable<CampaignContactDto>> ImportContactsFromCsvAsync(Guid campaignId, Stream csvStream, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            throw new ArgumentException($"Campaign {campaignId} not found");

        if (campaign.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Can only import contacts to draft campaigns");

        var importedContacts = new List<CampaignContactDto>();

        using var reader = new StreamReader(csvStream, Encoding.UTF8);
        var headerLine = await reader.ReadLineAsync();
        if (string.IsNullOrEmpty(headerLine))
            throw new ArgumentException("CSV file is empty");

        var headers = headerLine.Split(',').Select(h => h.Trim().ToLower()).ToArray();
        var phoneColumnIndex = Array.FindIndex(headers, h => h.Contains("phone") || h.Contains("telefone"));
        var nameColumnIndex = Array.FindIndex(headers, h => h.Contains("name") || h.Contains("nome"));
        var emailColumnIndex = Array.FindIndex(headers, h => h.Contains("email"));

        if (phoneColumnIndex == -1)
            throw new ArgumentException("CSV must contain a phone number column");

        string? line;
        var lineNumber = 1;

        while ((line = await reader.ReadLineAsync()) != null)
        {
            lineNumber++;
            if (string.IsNullOrWhiteSpace(line))
                continue;

            try
            {
                var columns = line.Split(',').Select(c => c.Trim()).ToArray();
                
                if (columns.Length <= phoneColumnIndex)
                {
                    _logger.LogWarning("Skipping line {LineNumber}: insufficient columns", lineNumber);
                    continue;
                }

                var phone = CleanPhoneNumber(columns[phoneColumnIndex]);
                if (string.IsNullOrEmpty(phone))
                {
                    _logger.LogWarning("Skipping line {LineNumber}: invalid phone number", lineNumber);
                    continue;
                }

                var name = nameColumnIndex >= 0 && nameColumnIndex < columns.Length 
                    ? columns[nameColumnIndex] 
                    : $"Contact {phone}";
                
                var email = emailColumnIndex >= 0 && emailColumnIndex < columns.Length 
                    ? columns[emailColumnIndex] 
                    : null;

                // Find or create customer
                var customer = await GetOrCreateCustomerAsync(campaign.CompanyId, name, phone, email, cancellationToken);
                
                // Create campaign contact if not exists
                var existingContact = await _context.CampaignContacts
                    .FirstOrDefaultAsync(cc => cc.CampaignId == campaignId && cc.CustomerId == customer.Id, cancellationToken);

                if (existingContact == null)
                {
                    var campaignContact = new CampaignContact
                    {
                        Id = Guid.NewGuid(),
                        CampaignId = campaignId,
                        CustomerId = customer.Id,
                        Status = CampaignContactStatus.Pending.ToString(),
                        CreatedAt = DateTime.UtcNow
                    };

                    _context.CampaignContacts.Add(campaignContact);
                    await _context.SaveChangesAsync(cancellationToken);

                    importedContacts.Add(await MapToDtoAsync(campaignContact, cancellationToken));
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error processing CSV line {LineNumber}", lineNumber);
            }
        }

        _logger.LogInformation("Imported {Count} contacts from CSV to campaign {CampaignId}", 
            importedContacts.Count, campaignId);

        return importedContacts;
    }

    public async Task<IEnumerable<CampaignContactDto>> ImportContactsFromCustomersAsync(Guid campaignId, IEnumerable<Guid> customerIds, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            throw new ArgumentException($"Campaign {campaignId} not found");

        if (campaign.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Can only import contacts to draft campaigns");

        var existingContactCustomerIds = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .Select(cc => cc.CustomerId)
            .ToHashSetAsync(cancellationToken);

        var newCustomerIds = customerIds.Where(id => !existingContactCustomerIds.Contains(id));
        
        var validCustomers = await _context.Customers
            .Where(c => newCustomerIds.Contains(c.Id) && c.CompanyId == campaign.CompanyId)
            .ToListAsync(cancellationToken);

        var campaignContacts = validCustomers.Select(customer => new CampaignContact
        {
            Id = Guid.NewGuid(),
            CampaignId = campaignId,
            CustomerId = customer.Id,
            Status = CampaignContactStatus.Pending.ToString(),
            CreatedAt = DateTime.UtcNow
        }).ToList();

        _context.CampaignContacts.AddRange(campaignContacts);
        await _context.SaveChangesAsync(cancellationToken);

        var tasks = campaignContacts.Select(cc => MapToDtoAsync(cc, cancellationToken));
        var result = await Task.WhenAll(tasks);

        _logger.LogInformation("Imported {Count} contacts from existing customers to campaign {CampaignId}", 
            result.Length, campaignId);

        return result;
    }

    public async Task<IEnumerable<CampaignContactDto>> ImportContactsFromCriteriaAsync(Guid campaignId, CustomerSearchCriteriaDto criteria, CancellationToken cancellationToken = default)
    {
        var campaign = await _context.Campaigns.FindAsync(campaignId, cancellationToken);
        if (campaign == null)
            throw new ArgumentException($"Campaign {campaignId} not found");

        if (campaign.Status != CampaignStatus.Draft.ToString())
            throw new InvalidOperationException("Can only import contacts to draft campaigns");

        var query = _context.Customers.Where(c => c.CompanyId == campaign.CompanyId);

        // Apply search criteria
        if (!string.IsNullOrEmpty(criteria.City))
            query = query.Where(c => c.AddressCity != null && c.AddressCity.ToLower().Contains(criteria.City.ToLower()));

        if (!string.IsNullOrEmpty(criteria.State))
            query = query.Where(c => c.AddressState != null && c.AddressState.ToLower().Contains(criteria.State.ToLower()));

        if (!string.IsNullOrEmpty(criteria.BloodType))
            query = query.Where(c => c.BloodType != null && c.BloodType.ToLower() == criteria.BloodType.ToLower());

        if (criteria.AgeMin.HasValue || criteria.AgeMax.HasValue)
        {
            var today = DateTime.Today;
            if (criteria.AgeMin.HasValue)
            {
                var maxBirthDate = today.AddYears(-criteria.AgeMin.Value);
                query = query.Where(c => c.CreatedAt <= maxBirthDate); // Using CreatedAt as proxy for age
            }
            if (criteria.AgeMax.HasValue)
            {
                var minBirthDate = today.AddYears(-criteria.AgeMax.Value - 1);
                query = query.Where(c => c.CreatedAt >= minBirthDate);
            }
        }

        var customers = await query.ToListAsync(cancellationToken);

        // Filter out customers already in campaign
        var existingContactCustomerIds = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .Select(cc => cc.CustomerId)
            .ToHashSetAsync(cancellationToken);

        var newCustomers = customers.Where(c => !existingContactCustomerIds.Contains(c.Id));

        var campaignContacts = newCustomers.Select(customer => new CampaignContact
        {
            Id = Guid.NewGuid(),
            CampaignId = campaignId,
            CustomerId = customer.Id,
            Status = CampaignContactStatus.Pending.ToString(),
            CreatedAt = DateTime.UtcNow
        }).ToList();

        _context.CampaignContacts.AddRange(campaignContacts);
        await _context.SaveChangesAsync(cancellationToken);

        var tasks = campaignContacts.Select(cc => MapToDtoAsync(cc, cancellationToken));
        var result = await Task.WhenAll(tasks);

        _logger.LogInformation("Imported {Count} contacts from search criteria to campaign {CampaignId}", 
            result.Length, campaignId);

        return result;
    }

    public async Task<CampaignContactStatsDto> GetCampaignContactStatsAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var contacts = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId)
            .ToListAsync(cancellationToken);

        return new CampaignContactStatsDto
        {
            CampaignId = campaignId,
            TotalContacts = contacts.Count,
            PendingCount = contacts.Count(c => c.Status == CampaignContactStatus.Pending.ToString()),
            SentCount = contacts.Count(c => c.Status == CampaignContactStatus.Sent.ToString()),
            DeliveredCount = contacts.Count(c => c.Status == CampaignContactStatus.Delivered.ToString()),
            ReadCount = contacts.Count(c => c.Status == CampaignContactStatus.Read.ToString()),
            RespondedCount = contacts.Count(c => c.Status == CampaignContactStatus.Responded.ToString()),
            FailedCount = contacts.Count(c => c.Status == CampaignContactStatus.Failed.ToString()),
            LastUpdated = contacts.Max(c => c.UpdatedAt) ?? contacts.Max(c => c.CreatedAt)
        };
    }

    public async Task<IEnumerable<CampaignContactDto>> GetFailedCampaignContactsAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var failedContacts = await _context.CampaignContacts
            .Include(cc => cc.Customer)
            .Include(cc => cc.Campaign)
            .Where(cc => cc.CampaignId == campaignId && cc.Status == CampaignContactStatus.Failed.ToString())
            .ToListAsync(cancellationToken);

        var tasks = failedContacts.Select(cc => MapToDtoAsync(cc, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<bool> RetryFailedContactAsync(Guid campaignContactId, CancellationToken cancellationToken = default)
    {
        return await _campaignMessagingService.RetryCampaignMessageAsync(campaignContactId, cancellationToken);
    }

    public async Task<int> RetryAllFailedContactsAsync(Guid campaignId, CancellationToken cancellationToken = default)
    {
        var failedContacts = await _context.CampaignContacts
            .Where(cc => cc.CampaignId == campaignId && cc.Status == CampaignContactStatus.Failed.ToString())
            .Select(cc => cc.Id)
            .ToListAsync(cancellationToken);

        var successCount = 0;
        foreach (var contactId in failedContacts)
        {
            try
            {
                var success = await _campaignMessagingService.RetryCampaignMessageAsync(contactId, cancellationToken);
                if (success) successCount++;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Error retrying campaign contact {ContactId}", contactId);
            }
        }

        _logger.LogInformation("Retried {SuccessCount}/{TotalCount} failed contacts for campaign {CampaignId}", 
            successCount, failedContacts.Count, campaignId);

        return successCount;
    }

    public async Task<bool> ExcludeContactFromCampaignAsync(Guid campaignContactId, string reason, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts.FindAsync(campaignContactId, cancellationToken);
        if (campaignContact == null)
            return false;

        campaignContact.Status = "EXCLUDED";
        campaignContact.ErrorMessage = $"Excluded: {reason}";
        campaignContact.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign contact {ContactId} excluded: {Reason}", campaignContactId, reason);

        return true;
    }

    public async Task<bool> ReincludeContactInCampaignAsync(Guid campaignContactId, CancellationToken cancellationToken = default)
    {
        var campaignContact = await _context.CampaignContacts.FindAsync(campaignContactId, cancellationToken);
        if (campaignContact == null || campaignContact.Status != "EXCLUDED")
            return false;

        campaignContact.Status = CampaignContactStatus.Pending.ToString();
        campaignContact.ErrorMessage = null;
        campaignContact.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Campaign contact {ContactId} reincluded in campaign", campaignContactId);

        return true;
    }

    private async Task<CampaignContactDto> MapToDtoAsync(CampaignContact campaignContact, CancellationToken cancellationToken)
    {
        // Load related entities if not already loaded
        if (campaignContact.Customer == null)
        {
            await _context.Entry(campaignContact)
                .Reference(cc => cc.Customer)
                .LoadAsync(cancellationToken);
        }

        if (campaignContact.Campaign == null)
        {
            await _context.Entry(campaignContact)
                .Reference(cc => cc.Campaign)
                .LoadAsync(cancellationToken);
        }

        return new CampaignContactDto
        {
            Id = campaignContact.Id,
            CampaignId = campaignContact.CampaignId,
            CustomerId = campaignContact.CustomerId,
            Status = Enum.Parse<CampaignContactStatus>(campaignContact.Status),
            SentAt = campaignContact.SentAt,
            DeliveredAt = campaignContact.DeliveredAt,
            ReadAt = campaignContact.ReadAt,
            RespondedAt = campaignContact.RespondedAt,
            ErrorMessage = campaignContact.ErrorMessage,
            CustomerName = campaignContact.Customer?.Name ?? "Unknown",
            CustomerPhone = campaignContact.Customer?.Phone ?? "Unknown",
            CampaignName = campaignContact.Campaign?.Name ?? "Unknown",
            CreatedAt = campaignContact.CreatedAt,
            UpdatedAt = campaignContact.UpdatedAt
        };
    }

    private async Task<Customer> GetOrCreateCustomerAsync(Guid companyId, string name, string phone, string? email, CancellationToken cancellationToken)
    {
        var existingCustomer = await _context.Customers
            .FirstOrDefaultAsync(c => c.CompanyId == companyId && c.Phone == phone, cancellationToken);

        if (existingCustomer != null)
            return existingCustomer;

        var customer = new Customer
        {
            Id = Guid.NewGuid(),
            CompanyId = companyId,
            Name = name,
            Phone = phone,
            Email = email,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        _context.Customers.Add(customer);
        await _context.SaveChangesAsync(cancellationToken);

        return customer;
    }

    private static string CleanPhoneNumber(string phone)
    {
        if (string.IsNullOrWhiteSpace(phone))
            return string.Empty;

        // Remove all non-digit characters
        var digits = new string(phone.Where(char.IsDigit).ToArray());
        
        // Basic validation - should have at least 10 digits
        if (digits.Length < 10)
            return string.Empty;

        return digits;
    }
}