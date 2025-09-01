using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("chat_lid_mappings")]
public class ChatLidMapping : BaseEntity
{
    [Column("chat_lid")]
    [MaxLength(100)]
    public string? ChatLid { get; set; }

    [Required]
    [Column("conversation_id")]
    public Guid ConversationId { get; set; }

    [Required]
    [Column("phone")]
    [MaxLength(20)]
    public string Phone { get; set; } = string.Empty;

    [Required]
    [Column("company_id")]
    public Guid CompanyId { get; set; }

    [Column("whatsapp_instance_id")]
    public Guid? WhatsAppInstanceId { get; set; }

    [Column("from_campaign")]
    public bool FromCampaign { get; set; } = false;

    // Navigation properties
    public virtual Conversation? Conversation { get; set; }
    public virtual Company? Company { get; set; }
    public virtual WhatsAppInstance? WhatsAppInstance { get; set; }
}