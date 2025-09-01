using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("departments")]
public class Department : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty;

    [Column("description", TypeName = "TEXT")]
    public string? Description { get; set; }

    [Column("auto_assign")]
    public bool AutoAssign { get; set; } = true;

    // Navigation properties
    [Column("company_id")]
    [Required]
    public Guid CompanyId { get; set; }
    
    [ForeignKey("CompanyId")]
    public virtual Company Company { get; set; } = null!;

    public virtual ICollection<User> Users { get; set; } = new List<User>();
}