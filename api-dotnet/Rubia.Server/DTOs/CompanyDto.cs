using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class CompanyDto
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Slug { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string? ContactEmail { get; set; }
    public string? ContactPhone { get; set; }
    public string? LogoUrl { get; set; }
    public bool IsActive { get; set; }
    public CompanyPlanType PlanType { get; set; }
    public int MaxUsers { get; set; }
    public int MaxWhatsappNumbers { get; set; }
    public int MaxAiAgents { get; set; }
    public Guid CompanyGroupId { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}