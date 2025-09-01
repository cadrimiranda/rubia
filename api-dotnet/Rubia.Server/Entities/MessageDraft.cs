using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("message_drafts")]
public class MessageDraft : BaseEntity
{
    [Column("content", TypeName = "TEXT")]
    [Required]
    public string Content { get; set; } = string.Empty;

    [Column("draft_type")]
    [Required]
    public MessageType DraftType { get; set; } = MessageType.Text;

    [Column("is_template")]
    [Required]
    public bool IsTemplate { get; set; } = false;

    [Column("template_name")]
    public string? TemplateName { get; set; }

    [Column("auto_save")]
    [Required]
    public bool AutoSave { get; set; } = true;

    // Navigation properties
    [Column("user_id")]
    [Required]
    public Guid UserId { get; set; }
    
    [ForeignKey("UserId")]
    public virtual User User { get; set; } = null!;

    [Column("conversation_id")]
    public Guid? ConversationId { get; set; }
    
    [ForeignKey("ConversationId")]
    public virtual Conversation? Conversation { get; set; }
}