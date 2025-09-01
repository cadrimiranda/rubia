using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class AIModelDto
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string? Capabilities { get; set; }
    public string? ImpactDescription { get; set; }
    public int? CostPer1kTokens { get; set; }
    public string? PerformanceLevel { get; set; }
    public string Provider { get; set; } = string.Empty;
    public bool IsActive { get; set; }
    public int SortOrder { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateAIModelDto : IValidatableObject
{
    [Required]
    [StringLength(255)]
    public string Name { get; set; } = string.Empty;

    [Required]
    [StringLength(255)]
    public string DisplayName { get; set; } = string.Empty;

    public string? Description { get; set; }

    public string? Capabilities { get; set; }

    public string? ImpactDescription { get; set; }

    [Range(0, int.MaxValue, ErrorMessage = "O custo por 1k tokens deve ser maior ou igual a zero")]
    public int? CostPer1kTokens { get; set; }

    [StringLength(50)]
    public string? PerformanceLevel { get; set; }

    [Required]
    [StringLength(100)]
    public string Provider { get; set; } = string.Empty;

    public bool IsActive { get; set; } = true;

    [Range(0, int.MaxValue, ErrorMessage = "A ordem de classificação deve ser maior ou igual a zero")]
    public int SortOrder { get; set; } = 0;

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        if (!string.IsNullOrWhiteSpace(PerformanceLevel))
        {
            var validLevels = new[] { "BASICO", "INTERMEDIARIO", "AVANCADO", "PREMIUM" };
            if (!validLevels.Contains(PerformanceLevel.ToUpper()))
            {
                results.Add(new ValidationResult("O nível de performance deve ser: BASICO, INTERMEDIARIO, AVANCADO ou PREMIUM", new[] { nameof(PerformanceLevel) }));
            }
        }

        return results;
    }
}

public class UpdateAIModelDto : IValidatableObject
{
    [StringLength(255)]
    public string? Name { get; set; }

    [StringLength(255)]
    public string? DisplayName { get; set; }

    public string? Description { get; set; }

    public string? Capabilities { get; set; }

    public string? ImpactDescription { get; set; }

    [Range(0, int.MaxValue, ErrorMessage = "O custo por 1k tokens deve ser maior ou igual a zero")]
    public int? CostPer1kTokens { get; set; }

    [StringLength(50)]
    public string? PerformanceLevel { get; set; }

    [StringLength(100)]
    public string? Provider { get; set; }

    public bool? IsActive { get; set; }

    [Range(0, int.MaxValue, ErrorMessage = "A ordem de classificação deve ser maior ou igual a zero")]
    public int? SortOrder { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        if (!string.IsNullOrWhiteSpace(PerformanceLevel))
        {
            var validLevels = new[] { "BASICO", "INTERMEDIARIO", "AVANCADO", "PREMIUM" };
            if (!validLevels.Contains(PerformanceLevel.ToUpper()))
            {
                results.Add(new ValidationResult("O nível de performance deve ser: BASICO, INTERMEDIARIO, AVANCADO ou PREMIUM", new[] { nameof(PerformanceLevel) }));
            }
        }

        return results;
    }
}