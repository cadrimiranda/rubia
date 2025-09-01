using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class CampaignStatsDto
{
    public Guid CampaignId { get; set; }
    public int TotalContacts { get; set; }
    public int PendingCount { get; set; }
    public int SentCount { get; set; }
    public int DeliveredCount { get; set; }
    public int ReadCount { get; set; }
    public int RespondedCount { get; set; }
    public int FailedCount { get; set; }
    public double DeliveryRate { get; set; }
    public double ReadRate { get; set; }
    public double ResponseRate { get; set; }
}

public class CampaignContactDto
{
    public Guid Id { get; set; }
    public Guid CampaignId { get; set; }
    public Guid CustomerId { get; set; }
    public CampaignContactStatus Status { get; set; }
    public DateTime? SentAt { get; set; }
    public DateTime? DeliveredAt { get; set; }
    public DateTime? ReadAt { get; set; }
    public DateTime? RespondedAt { get; set; }
    public string? ErrorMessage { get; set; }
    public string CustomerName { get; set; } = string.Empty;
    public string CustomerPhone { get; set; } = string.Empty;
    public string CampaignName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
}

public class CreateCampaignContactDto
{
    [Required]
    public Guid CampaignId { get; set; }
    
    [Required]
    public Guid CustomerId { get; set; }
}

public class UpdateCampaignContactStatusDto
{
    [Required]
    public string Status { get; set; } = string.Empty;
}

public class CampaignMessageStatsDto
{
    public Guid CampaignId { get; set; }
    public int TotalContacts { get; set; }
    public int SentCount { get; set; }
    public int DeliveredCount { get; set; }
    public int ReadCount { get; set; }
    public int RespondedCount { get; set; }
    public int FailedCount { get; set; }
    public int PendingCount { get; set; }
    public double DeliveryRate { get; set; }
    public double ReadRate { get; set; }
    public double ResponseRate { get; set; }
    public double FailureRate { get; set; }
}

public class PagedResult<T>
{
    public IEnumerable<T> Items { get; set; } = new List<T>();
    public int TotalCount { get; set; }
    public int Page { get; set; }
    public int PageSize { get; set; }
    public int TotalPages { get; set; }
    public bool HasPreviousPage => Page > 1;
    public bool HasNextPage => Page < TotalPages;
}

public class CustomerSearchCriteriaDto
{
    public string? City { get; set; }
    public string? State { get; set; }
    public string? BloodType { get; set; }
    public int? AgeMin { get; set; }
    public int? AgeMax { get; set; }
}

public class ImportCustomersDto
{
    [Required]
    public List<Guid> CustomerIds { get; set; } = new();
}

public class ExcludeContactDto
{
    [Required]
    public string Reason { get; set; } = string.Empty;
}

public class CampaignPreviewDto
{
    public Guid CampaignId { get; set; }
    public string CampaignName { get; set; } = string.Empty;
    public int TotalContacts { get; set; }
    public string MessageTemplate { get; set; } = string.Empty;
    public List<CampaignContactPreviewDto> SampleContacts { get; set; } = new();
}

public class CampaignContactPreviewDto
{
    public string CustomerName { get; set; } = string.Empty;
    public string CustomerPhone { get; set; } = string.Empty;
    public string MessageContent { get; set; } = string.Empty;
}

public class CampaignContactStatsDto
{
    public Guid CampaignId { get; set; }
    public int TotalContacts { get; set; }
    public int PendingCount { get; set; }
    public int SentCount { get; set; }
    public int DeliveredCount { get; set; }
    public int ReadCount { get; set; }
    public int RespondedCount { get; set; }
    public int FailedCount { get; set; }
    public DateTime LastUpdated { get; set; }
}