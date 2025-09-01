using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Microsoft.EntityFrameworkCore;

namespace Rubia.Server.Entities;

[Table("user_ai_agents")]
[Index(nameof(UserId), nameof(AiAgentId), IsUnique = true)]
public class UserAIAgent : BaseEntity
{
    [Key]
    public Guid Id { get; set; } = Guid.NewGuid();

    [Required]
    [Column("company_id")]
    public Guid CompanyId { get; set; }

    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    [Required]
    [Column("user_id")]
    public Guid UserId { get; set; }

    [ForeignKey("UserId")]
    public virtual User User { get; set; } = null!;

    [Required]
    [Column("ai_agent_id")]
    public Guid AiAgentId { get; set; }

    [ForeignKey("AiAgentId")]
    public virtual AIAgent AiAgent { get; set; } = null!;

    [Column("assigned_at")]
    public DateTime AssignedAt { get; set; } = DateTime.UtcNow;

    [Column("is_default")]
    public bool IsDefault { get; set; } = false;
}