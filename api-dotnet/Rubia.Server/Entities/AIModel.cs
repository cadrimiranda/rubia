using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace Rubia.Server.Entities;

[Table("ai_models")]
public class AIModel : BaseEntity
{
    [Required]
    [Column("name")]
    public string Name { get; set; } = string.Empty; // Nome técnico do modelo (ex: "gpt-4.1", "gpt-4o-mini", "o3")

    [Required]
    [Column("display_name")]
    public string DisplayName { get; set; } = string.Empty; // Nome amigável para exibição

    [Column("description", TypeName = "TEXT")]
    public string? Description { get; set; } // Descrição básica do modelo

    [Column("capabilities", TypeName = "TEXT")]
    public string? Capabilities { get; set; } // Descrição detalhada das capacidades do modelo

    [Column("impact_description", TypeName = "TEXT")]
    public string? ImpactDescription { get; set; } // Descrição do impacto e casos de uso recomendados

    [Column("cost_per_1k_tokens")]
    public int? CostPer1kTokens { get; set; } // Custo em créditos por 1000 tokens

    [Column("performance_level")]
    public string? PerformanceLevel { get; set; } // Nível de performance: "BASICO", "INTERMEDIARIO", "AVANCADO", "PREMIUM"

    [Required]
    [Column("provider")]
    public string Provider { get; set; } = string.Empty; // Provedor do modelo (ex: "OpenAI", "Anthropic", "Google")

    [Column("is_active")]
    public bool IsActive { get; set; } = true; // Se o modelo está ativo e disponível para uso

    [Column("sort_order")]
    public int SortOrder { get; set; } = 0; // Ordem de exibição na lista

    public virtual ICollection<AIAgent> Agents { get; set; } = new List<AIAgent>();
}