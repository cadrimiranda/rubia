using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("conversation_last_message")]
public class ConversationLastMessage : BaseEntity
{
    [Required]
    [Column("conversation_id")]
    public Guid ConversationId { get; set; }

    [Required]
    [Column("message_id")]
    public Guid MessageId { get; set; }

    [Column("content")]
    public string? Content { get; set; }

    [Required]
    [Column("sender_type")]
    [MaxLength(20)]
    public string SenderType { get; set; } = string.Empty;

    [Column("sender_id")]
    public Guid? SenderId { get; set; }

    [Required]
    [Column("message_created_at")]
    public DateTime MessageCreatedAt { get; set; }

    [Column("is_ai_generated")]
    public bool? IsAiGenerated { get; set; }

    [Column("sentiment")]
    [MaxLength(20)]
    public string? Sentiment { get; set; }

    [Column("keywords")]
    public string? Keywords { get; set; }

    // Navigation properties
    public virtual Conversation? Conversation { get; set; }
    public virtual Message? Message { get; set; }
}