using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class AILogDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public Guid AiAgentId { get; set; }
    public Guid? UserId { get; set; }
    public Guid? ConversationId { get; set; }
    public Guid? MessageId { get; set; }
    public Guid? MessageTemplateId { get; set; }
    public string RequestPrompt { get; set; } = string.Empty;
    public string? RawResponse { get; set; }
    public string? ProcessedResponse { get; set; }
    public int? TokensUsedInput { get; set; }
    public int? TokensUsedOutput { get; set; }
    public decimal? EstimatedCost { get; set; }
    public AILogStatus Status { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime CreatedAt { get; set; }
}

public class CreateAILogDto
{
    [Required(ErrorMessage = "Company ID is required")]
    public Guid CompanyId { get; set; }

    [Required(ErrorMessage = "AI Agent ID is required")]
    public Guid AiAgentId { get; set; }

    public Guid? UserId { get; set; }
    public Guid? ConversationId { get; set; }
    public Guid? MessageId { get; set; }
    public Guid? MessageTemplateId { get; set; }

    [Required(ErrorMessage = "Request prompt is required")]
    [MaxLength(10000, ErrorMessage = "Request prompt must not exceed 10000 characters")]
    public string RequestPrompt { get; set; } = string.Empty;

    [MaxLength(10000, ErrorMessage = "Raw response must not exceed 10000 characters")]
    public string? RawResponse { get; set; }

    [MaxLength(10000, ErrorMessage = "Processed response must not exceed 10000 characters")]
    public string? ProcessedResponse { get; set; }

    public int? TokensUsedInput { get; set; }
    public int? TokensUsedOutput { get; set; }
    public decimal? EstimatedCost { get; set; }

    [Required(ErrorMessage = "Status is required")]
    public AILogStatus Status { get; set; }

    [MaxLength(1000, ErrorMessage = "Error message must not exceed 1000 characters")]
    public string? ErrorMessage { get; set; }
}

public class UpdateAILogDto
{
    [MaxLength(10000, ErrorMessage = "Raw response must not exceed 10000 characters")]
    public string? RawResponse { get; set; }

    [MaxLength(10000, ErrorMessage = "Processed response must not exceed 10000 characters")]
    public string? ProcessedResponse { get; set; }

    public int? TokensUsedInput { get; set; }
    public int? TokensUsedOutput { get; set; }
    public decimal? EstimatedCost { get; set; }
    public AILogStatus? Status { get; set; }

    [MaxLength(1000, ErrorMessage = "Error message must not exceed 1000 characters")]
    public string? ErrorMessage { get; set; }
}