using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class FAQDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public string Question { get; set; } = string.Empty;
    public string Answer { get; set; } = string.Empty;
    public string? Keywords { get; set; }
    public int UsageCount { get; set; }
    public decimal? SuccessRate { get; set; }
    public bool IsActive { get; set; }
    public int? Priority { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateFAQDto
{
    [Required]
    public Guid CompanyId { get; set; }

    [Required]
    [MaxLength(500)]
    public string Question { get; set; } = string.Empty;

    [Required]
    public string Answer { get; set; } = string.Empty;

    public string? Keywords { get; set; }
    public int? Priority { get; set; }
}

public class UpdateFAQDto
{
    [MaxLength(500)]
    public string? Question { get; set; }

    public string? Answer { get; set; }
    public string? Keywords { get; set; }
    public bool? IsActive { get; set; }
    public int? Priority { get; set; }
}

public class FAQMatchDto
{
    public Guid Id { get; set; }
    public string Question { get; set; } = string.Empty;
    public string Answer { get; set; } = string.Empty;
    public double MatchScore { get; set; }
    public int UsageCount { get; set; }
}

public class FAQSearchDto
{
    [Required]
    [MinLength(3)]
    public string Query { get; set; } = string.Empty;
}

public class FAQStatsDto
{
    public int TotalFAQs { get; set; }
    public int TotalUsage { get; set; }
    public List<string> TopFAQs { get; set; } = new();
}