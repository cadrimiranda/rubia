using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("message_templates")]
public class MessageTemplate : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Required]
    [Column("content", TypeName = "TEXT")]
    public string Content { get; set; } = string.Empty;

    [Column("is_ai_generated")]
    [Required]
    public bool IsAiGenerated { get; set; } = false;

    [Column("tone")]
    public string? Tone { get; set; } // Ex: "FORMAL", "INFORMAL", "DESCONTRAIDO", "EMPATICO"

    [Column("edit_count")]
    public int EditCount { get; set; } = 0;

    [Column("deleted_at")]
    public DateTime? DeletedAt { get; set; }

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    [Column("created_by_user_id")]
    public Guid? CreatedByUserId { get; set; }
    
    [ForeignKey("CreatedByUserId")]
    public virtual User? CreatedBy { get; set; }

    [Column("ai_agent_id")]
    public Guid? AiAgentId { get; set; }
    
    [ForeignKey("AiAgentId")]
    public virtual AIAgent? AiAgent { get; set; } // Qual agente de IA gerou este template (se aplicável)

    [Column("last_edited_by_user_id")]
    public Guid? LastEditedByUserId { get; set; }
    
    [ForeignKey("LastEditedByUserId")]
    public virtual User? LastEditedBy { get; set; }

    public virtual ICollection<MessageTemplateRevision> Revisions { get; set; } = new List<MessageTemplateRevision>();
    public virtual ICollection<Message> Messages { get; set; } = new List<Message>();
    public virtual ICollection<Campaign> CampaignsAsInitialTemplate { get; set; } = new List<Campaign>();
}