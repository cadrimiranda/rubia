using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class EnhanceTemplateDto
{
    [Required(ErrorMessage = "Company ID is required")]
    public Guid CompanyId { get; set; }

    [Required(ErrorMessage = "Original template content is required")]
    public string OriginalContent { get; set; } = string.Empty;

    [Required(ErrorMessage = "Enhancement type is required")]
    public string EnhancementType { get; set; } = string.Empty; // "friendly", "professional", "empathetic", "urgent", "motivational"

    [Required(ErrorMessage = "Template category is required")]
    public string Category { get; set; } = string.Empty; // Template category for context

    public string? Title { get; set; } // Template title for additional context
}

public class EnhancedTemplateResponseDto
{
    public string OriginalContent { get; set; } = string.Empty;
    public string EnhancedContent { get; set; } = string.Empty;
    public string EnhancementType { get; set; } = string.Empty;
    public string? AiExplanation { get; set; }
    public int TokensUsed { get; set; }
    public int CreditsConsumed { get; set; }
    public string ModelUsed { get; set; } = string.Empty;
    public DateTime EnhancedAt { get; set; } = DateTime.UtcNow;
}

public class SaveTemplateWithAiMetadataDto
{
    [Required]
    public Guid TemplateId { get; set; }

    [Required]
    [MaxLength(10000)]
    public string EnhancedContent { get; set; } = string.Empty;

    [Required]
    public string EnhancementType { get; set; } = string.Empty;

    public Guid? AiAgentId { get; set; }
    public string? AiExplanation { get; set; }
    public int? TokensUsed { get; set; }
    public int? CreditsConsumed { get; set; }
    public string? ModelUsed { get; set; }
}