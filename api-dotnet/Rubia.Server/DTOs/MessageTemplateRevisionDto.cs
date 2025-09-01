using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class MessageTemplateRevisionDto
{
    public Guid Id { get; set; }
    public Guid TemplateId { get; set; }
    public string? TemplateName { get; set; }
    public int RevisionNumber { get; set; }
    public string Content { get; set; } = string.Empty;
    public Guid? EditedByUserId { get; set; }
    public string? EditedByUserName { get; set; }
    public RevisionType RevisionType { get; set; }
    public DateTime RevisionTimestamp { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    // AI metadata fields
    public Guid? AiAgentId { get; set; }
    public string? AiAgentName { get; set; }
    public string? AiEnhancementType { get; set; }
    public int? AiTokensUsed { get; set; }
    public int? AiCreditsConsumed { get; set; }
    public string? AiModelUsed { get; set; }
    public string? AiExplanation { get; set; }
}

public class CreateMessageTemplateRevisionDto
{
    [Required]
    public Guid TemplateId { get; set; }

    [Required]
    [MaxLength(10000)]
    public string Content { get; set; } = string.Empty;

    [Required]
    public RevisionType RevisionType { get; set; }

    public Guid? EditedByUserId { get; set; }

    // AI metadata
    public Guid? AiAgentId { get; set; }
    public string? AiEnhancementType { get; set; }
    public int? AiTokensUsed { get; set; }
    public int? AiCreditsConsumed { get; set; }
    public string? AiModelUsed { get; set; }
    public string? AiExplanation { get; set; }
}

public class UpdateMessageTemplateRevisionDto
{
    [MaxLength(10000)]
    public string? Content { get; set; }

    public RevisionType? RevisionType { get; set; }
    public string? AiEnhancementType { get; set; }
    public int? AiTokensUsed { get; set; }
    public int? AiCreditsConsumed { get; set; }
    public string? AiModelUsed { get; set; }
    public string? AiExplanation { get; set; }
}