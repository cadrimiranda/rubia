using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("ai_logs")]
public class AILog
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public Company Company { get; set; } = null!;
    public Guid AiAgentId { get; set; }
    public AIAgent AiAgent { get; set; } = null!;
    public Guid? UserId { get; set; }
    public User? User { get; set; }
    public Guid? ConversationId { get; set; }
    public Conversation? Conversation { get; set; }
    public Guid? MessageId { get; set; }
    public Message? Message { get; set; }
    public Guid? MessageTemplateId { get; set; }
    public MessageTemplate? MessageTemplate { get; set; }
    public string RequestPrompt { get; set; } = string.Empty;
    public string? RawResponse { get; set; }
    public string? ProcessedResponse { get; set; }
    public int? TokensUsedInput { get; set; }
    public int? TokensUsedOutput { get; set; }
    public decimal? EstimatedCost { get; set; }
    public AILogStatus Status { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}