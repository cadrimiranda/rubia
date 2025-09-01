using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("campaign_contacts")]
public class CampaignContact : BaseEntity
{
    [Column("contact_status")]
    [Required]
    public CampaignContactStatus Status { get; set; } = CampaignContactStatus.Pending;

    [Column("message_sent_at")]
    public DateTime? MessageSentAt { get; set; }

    [Column("response_received_at")]
    public DateTime? ResponseReceivedAt { get; set; }

    [Column("notes", TypeName = "TEXT")]
    public string? Notes { get; set; }

    // Navigation properties
    [Column("campaign_id")]
    [Required]
    public Guid CampaignId { get; set; }
    
    [ForeignKey("CampaignId")]
    public virtual Campaign Campaign { get; set; } = null!;

    [Column("customer_id")]
    [Required]
    public Guid CustomerId { get; set; }
    
    [ForeignKey("CustomerId")]
    public virtual Customer Customer { get; set; } = null!; // O cliente associado a esta campanha

    public virtual ICollection<Message> Messages { get; set; } = new List<Message>();
}