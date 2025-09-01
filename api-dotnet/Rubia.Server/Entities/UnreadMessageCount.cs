using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Microsoft.EntityFrameworkCore;

namespace Rubia.Server.Entities;

[Table("unread_message_counts")]
[Index(nameof(UserId), nameof(ConversationId), IsUnique = true)]
public class UnreadMessageCount : BaseEntity
{
    [Column("count")]
    [Required]
    public int Count { get; set; } = 0;

    [Column("last_read_at")]
    public DateTime? LastReadAt { get; set; }

    // Navigation properties
    [Column("user_id")]
    [Required]
    public Guid UserId { get; set; }
    
    [ForeignKey("UserId")]
    public virtual User User { get; set; } = null!;

    [Column("conversation_id")]
    [Required]
    public Guid ConversationId { get; set; }
    
    [ForeignKey("ConversationId")]
    public virtual Conversation Conversation { get; set; } = null!;
}