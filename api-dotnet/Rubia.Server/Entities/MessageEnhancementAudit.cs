using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("message_enhancement_audit")]
public class MessageEnhancementAudit : BaseEntity
{
    [Required]
    [Column("company_id")]
    public Guid CompanyId { get; set; }

    [Required]
    [Column("user_id")]
    public Guid UserId { get; set; }

    [Column("ai_agent_id")]
    public Guid? AiAgentId { get; set; }

    [Column("ai_model_id")]
    public Guid? AiModelId { get; set; }

    [Column("message_template_id")]
    public Guid? MessageTemplateId { get; set; }

    [Required]
    [Column("original_content")]
    public string OriginalContent { get; set; } = string.Empty;

    [Required]
    [Column("enhanced_content")]
    public string EnhancedContent { get; set; } = string.Empty;

    [Column("system_message")]
    public string? SystemMessage { get; set; }

    [Column("model_name")]
    [MaxLength(50)]
    public string? ModelName { get; set; }

    [Column("temperature")]
    public double? Temperature { get; set; }

    [Column("max_tokens")]
    public int? MaxTokens { get; set; }

    [Column("tokens_used")]
    public int? TokensUsed { get; set; }

    [Column("response_time_ms")]
    public long? ResponseTimeMs { get; set; }

    [Column("openai_payload_json")]
    public string? OpenaiPayloadJson { get; set; }

    [Column("cost_estimate")]
    public decimal? CostEstimate { get; set; }

    [Column("success")]
    public bool Success { get; set; } = true;

    [Column("error_message")]
    public string? ErrorMessage { get; set; }

    // Navigation properties
    public virtual Company? Company { get; set; }
    public virtual User? User { get; set; }
    public virtual AIAgent? AiAgent { get; set; }
    public virtual AIModel? AiModel { get; set; }
    public virtual MessageTemplate? MessageTemplate { get; set; }
}