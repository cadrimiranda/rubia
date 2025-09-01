using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("campaigns")]
public class Campaign : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Column("description", TypeName = "TEXT")]
    public string? Description { get; set; }

    [Column("campaign_status")]
    [Required]
    public CampaignStatus Status { get; set; } = CampaignStatus.Active;

    [Column("start_date")]
    public DateOnly? StartDate { get; set; }

    [Column("end_date")]
    public DateOnly? EndDate { get; set; }

    [Column("target_audience_description", TypeName = "TEXT")]
    public string? TargetAudienceDescription { get; set; }

    [Column("total_contacts")]
    public int TotalContacts { get; set; } = 0;

    [Column("contacts_reached")]
    public int ContactsReached { get; set; } = 0;

    [Column("source_system_name")]
    public string? SourceSystemName { get; set; } // Ex: "CRM Externo", "Sistema de Doação Realblood"

    [Column("source_system_id")]
    public string? SourceSystemId { get; set; } // ID original da campanha no sistema de origem

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

    [Column("message_template_id")]
    public Guid? MessageTemplateId { get; set; }
    
    [ForeignKey("MessageTemplateId")]
    public virtual MessageTemplate? InitialMessageTemplate { get; set; }

    public virtual ICollection<CampaignContact> CampaignContacts { get; set; } = new List<CampaignContact>();
    public virtual ICollection<Conversation> Conversations { get; set; } = new List<Conversation>();
}