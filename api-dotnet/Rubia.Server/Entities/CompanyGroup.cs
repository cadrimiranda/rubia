using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("company_groups")]
public class CompanyGroup : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty; // Nome do grupo (ex: "Grupo GSH")

    [Column("description", TypeName = "TEXT")]
    public string? Description { get; set; }

    [Column("is_active")]
    public bool IsActive { get; set; } = true;

    // Relação com as empresas que fazem parte deste grupo
    public virtual ICollection<Company> Companies { get; set; } = new List<Company>();
}