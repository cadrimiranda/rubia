using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class CampaignDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public CampaignStatus Status { get; set; }
    public DateTime? StartDate { get; set; }
    public DateTime? EndDate { get; set; }
    public Guid? MessageTemplateId { get; set; }
    public string? MessageTemplateName { get; set; }
    public int TotalContacts { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateCampaignDto
{
    [Required]
    public Guid CompanyId { get; set; }

    [Required]
    [MaxLength(255)]
    public string Name { get; set; } = string.Empty;

    public string? Description { get; set; }

    public DateTime? StartDate { get; set; }
    public DateTime? EndDate { get; set; }
    public Guid? MessageTemplateId { get; set; }
}

public class UpdateCampaignDto
{
    [MaxLength(255)]
    public string? Name { get; set; }

    public string? Description { get; set; }
    public DateTime? StartDate { get; set; }
    public DateTime? EndDate { get; set; }
    public Guid? MessageTemplateId { get; set; }
}

public class UpdateCampaignStatusDto
{
    [Required]
    public CampaignStatus Status { get; set; }
}

public class AddContactsDto
{
    [Required]
    public List<Guid> CustomerIds { get; set; } = new();
}

public class CampaignStatsDto
{
    public Guid CampaignId { get; set; }
    public int TotalContacts { get; set; }
    public int PendingContacts { get; set; }
    public int SentContacts { get; set; }
    public int DeliveredContacts { get; set; }
    public int FailedContacts { get; set; }
}

public class ProcessCampaignDto
{
    [Required(ErrorMessage = "Company ID is required")]
    public Guid CompanyId { get; set; }

    [Required(ErrorMessage = "User ID is required")]
    public Guid UserId { get; set; }

    [Required(ErrorMessage = "Campaign name is required")]
    [MaxLength(255, ErrorMessage = "Campaign name must not exceed 255 characters")]
    public string Name { get; set; } = string.Empty;

    [MaxLength(1000, ErrorMessage = "Description must not exceed 1000 characters")]
    public string? Description { get; set; }

    [Required(ErrorMessage = "Start date is required")]
    public DateOnly StartDate { get; set; }

    [Required(ErrorMessage = "End date is required")]
    public DateOnly EndDate { get; set; }

    [MaxLength(100, ErrorMessage = "Source system name must not exceed 100 characters")]
    public string? SourceSystem { get; set; }

    [Required(ErrorMessage = "At least one template must be selected")]
    public List<Guid> TemplateIds { get; set; } = new();
}