using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("companies")]
public class Company : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Required]
    [Column("slug")]
    public string Slug { get; set; } = string.Empty;

    [Column("description", TypeName = "TEXT")]
    public string? Description { get; set; }

    [Column("contact_email")]
    public string? ContactEmail { get; set; }

    [Column("contact_phone")]
    public string? ContactPhone { get; set; }

    [Column("logo_url")]
    public string? LogoUrl { get; set; }

    [Column("is_active")]
    public bool IsActive { get; set; } = true;

    [Column("plan_type")]
    public CompanyPlanType PlanType { get; set; } = CompanyPlanType.Basic;

    [Column("max_users")]
    public int MaxUsers { get; set; } = 10;

    [Column("max_whatsapp_numbers")]
    public int MaxWhatsappNumbers { get; set; } = 1;

    [Column("max_ai_agents")]
    public int MaxAiAgents { get; set; } = 1;

    // Navigation properties
    [Column("company_group_id")]
    [Required]
    public Guid CompanyGroupId { get; set; }
    
    [ForeignKey("CompanyGroupId")]
    public virtual CompanyGroup CompanyGroup { get; set; } = null!;

    public virtual ICollection<Department> Departments { get; set; } = new List<Department>();
    public virtual ICollection<WhatsAppInstance> WhatsappInstances { get; set; } = new List<WhatsAppInstance>();
    public virtual ICollection<User> Users { get; set; } = new List<User>();
    public virtual ICollection<Conversation> Conversations { get; set; } = new List<Conversation>();
}