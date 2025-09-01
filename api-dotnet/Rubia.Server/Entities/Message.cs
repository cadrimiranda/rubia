using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("messages")]
public class Message : BaseEntity
{
    [Column("content", TypeName = "TEXT")]
    public string? Content { get; set; }

    [Column("sender_type")]
    [Required]
    public SenderType SenderType { get; set; }

    [Column("sender_id")]
    public Guid? SenderId { get; set; }

    [Column("status")]
    [Required]
    public MessageStatus Status { get; set; } = MessageStatus.Sent;

    [Column("delivered_at")]
    public DateTime? DeliveredAt { get; set; }

    [Column("read_at")]
    public DateTime? ReadAt { get; set; }

    [Column("external_message_id")]
    public string? ExternalMessageId { get; set; }

    [Column("is_ai_generated")]
    public bool? IsAiGenerated { get; set; }

    [Column("ai_confidence")]
    public double? AiConfidence { get; set; }

    [Column("sentiment")]
    [MaxLength(20)]
    public string? Sentiment { get; set; }

    [Column("keywords")]
    public string? Keywords { get; set; }

    // Navigation properties
    [Column("conversation_id")]
    [Required]
    public Guid ConversationId { get; set; }
    
    [ForeignKey("ConversationId")]
    public virtual Conversation Conversation { get; set; } = null!;

    [Column("ai_agent_id")]
    public Guid? AiAgentId { get; set; }
    
    [ForeignKey("AiAgentId")]
    public virtual AIAgent? AiAgent { get; set; }

    [Column("message_template_id")]
    public Guid? MessageTemplateId { get; set; }
    
    [ForeignKey("MessageTemplateId")]
    public virtual MessageTemplate? MessageTemplate { get; set; }

    [Column("campaign_contact_id")]
    public Guid? CampaignContactId { get; set; }
    
    [ForeignKey("CampaignContactId")]
    public virtual CampaignContact? CampaignContact { get; set; }

    [Column("conversation_media_id")]
    public Guid? ConversationMediaId { get; set; }
    
    [ForeignKey("ConversationMediaId")]
    public virtual ConversationMedia? Media { get; set; }
}