using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("message_template_revisions")]
public class MessageTemplateRevision : BaseEntity
{
    [Required]
    [Column("revision_number")]
    public int RevisionNumber { get; set; } // Número da revisão (1 para a original, 2 para a primeira edição, etc.)

    [Required]
    [Column("content", TypeName = "TEXT")]
    public string Content { get; set; } = string.Empty; // Conteúdo desta revisão específica do template

    [Column("revision_type")]
    [Required]
    public RevisionType RevisionType { get; set; } = RevisionType.Edit; // Tipo da revisão

    // Novos campos para metadados de IA
    [Column("ai_enhancement_type")]
    public string? AiEnhancementType { get; set; } // Tipo de melhoria aplicada pela IA

    [Column("ai_tokens_used")]
    public int? AiTokensUsed { get; set; } // Tokens consumidos pela IA

    [Column("ai_credits_consumed")]
    public int? AiCreditsConsumed { get; set; } // Créditos consumidos pela IA

    [Column("ai_model_used")]
    public string? AiModelUsed { get; set; } // Nome do modelo de IA usado

    [Column("ai_explanation", TypeName = "TEXT")]
    public string? AiExplanation { get; set; } // Explicação das melhorias aplicadas pela IA

    [Column("revision_timestamp")]
    [Required]
    public DateTime RevisionTimestamp { get; set; } = DateTime.UtcNow; // Quando esta revisão foi criada

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    [Column("template_id")]
    [Required]
    public Guid TemplateId { get; set; }
    
    [ForeignKey("TemplateId")]
    public virtual MessageTemplate Template { get; set; } = null!; // Template ao qual esta revisão pertence

    [Column("edited_by_user_id")]
    public Guid? EditedByUserId { get; set; }
    
    [ForeignKey("EditedByUserId")]
    public virtual User? EditedBy { get; set; } // Quem criou/editou esta revisão

    [Column("ai_agent_id")]
    public Guid? AiAgentId { get; set; }
    
    [ForeignKey("AiAgentId")]
    public virtual AIAgent? AiAgent { get; set; } // Agente de IA usado para esta melhoria (se aplicável)
}