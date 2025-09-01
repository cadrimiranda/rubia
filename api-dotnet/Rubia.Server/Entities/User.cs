using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

[Table("users")]
public class User : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Required]
    [Column("email")]
    public string Email { get; set; } = string.Empty;

    [Required]
    [Column("password_hash")]
    public string PasswordHash { get; set; } = string.Empty;

    [Column("role")]
    public UserRole Role { get; set; }

    [Column("avatar_url")]
    public string? AvatarUrl { get; set; }

    [Column("is_online")]
    public bool IsOnline { get; set; } = false;

    [Column("last_seen")]
    public DateTime? LastSeen { get; set; }

    [Column("whatsapp_number")]
    public string? WhatsappNumber { get; set; }

    [Column("is_whatsapp_active")]
    public bool IsWhatsappActive { get; set; } = false;

    [Column("birth_date")]
    public DateOnly? BirthDate { get; set; }

    [Column("weight")]
    public double? Weight { get; set; }

    [Column("height")]
    public double? Height { get; set; }

    [Column("address")]
    public string? Address { get; set; }

    // Navigation properties
    [Column("department_id")]
    [Required]
    public Guid DepartmentId { get; set; }
    
    [ForeignKey("DepartmentId")]
    public virtual Department Department { get; set; } = null!;

    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    public virtual ICollection<Conversation> AssignedConversations { get; set; } = new List<Conversation>();
    public virtual ICollection<Conversation> OwnedConversations { get; set; } = new List<Conversation>();
}