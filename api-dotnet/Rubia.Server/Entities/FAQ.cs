using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("faqs")]
public class FAQ : BaseEntity
{
    [Column("question", TypeName = "TEXT")]
    [Required]
    public string Question { get; set; } = string.Empty;

    [Column("answer", TypeName = "TEXT")]
    [Required]
    public string Answer { get; set; } = string.Empty;

    [Column("keywords", TypeName = "TEXT")]
    public string? Keywords { get; set; }

    [Column("usage_count")]
    [Required]
    public int UsageCount { get; set; } = 0;

    [Column("success_rate")]
    public decimal? SuccessRate { get; set; }

    [Column("is_active")]
    [Required]
    public bool IsActive { get; set; } = true;

    [Column("priority")]
    public int? Priority { get; set; }

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;
}