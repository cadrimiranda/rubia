using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class AIAgentDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public string Name { get; set; } = string.Empty;
    public string? Description { get; set; }
    public string? AvatarBase64 { get; set; }
    public Guid AIModelId { get; set; }
    public string AIModelName { get; set; } = string.Empty;
    public string Temperament { get; set; } = string.Empty;
    public int MaxResponseLength { get; set; }
    public decimal Temperature { get; set; }
    public int AIMessageLimit { get; set; }
    public bool IsActive { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateAIAgentDto : IValidatableObject
{
    [Required]
    public Guid CompanyId { get; set; }

    [Required]
    [StringLength(255)]
    public string Name { get; set; } = string.Empty;

    [StringLength(2000)]
    public string? Description { get; set; }

    public string? AvatarBase64 { get; set; }

    [Required]
    public Guid AIModelId { get; set; }

    [Required]
    [StringLength(50)]
    public string Temperament { get; set; } = string.Empty;

    [Range(1, 10000, ErrorMessage = "O comprimento máximo da resposta deve estar entre 1 e 10000")]
    public int MaxResponseLength { get; set; } = 500;

    [Range(0.0, 1.0, ErrorMessage = "A temperatura deve estar entre 0.0 e 1.0")]
    public decimal Temperature { get; set; } = 0.7m;

    [Range(1, 1000, ErrorMessage = "O limite de mensagens de IA deve estar entre 1 e 1000")]
    public int AIMessageLimit { get; set; } = 10;

    public bool IsActive { get; set; } = true;

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        if (!string.IsNullOrWhiteSpace(AvatarBase64) && 
            !System.Text.RegularExpressions.Regex.IsMatch(AvatarBase64, 
                @"^data:image\/(jpeg|jpg|png|gif);base64,[A-Za-z0-9+/]+=*$"))
        {
            results.Add(new ValidationResult("O avatar deve ser uma imagem base64 válida (data:image/jpeg;base64,...) ou vazio", new[] { nameof(AvatarBase64) }));
        }

        var validTemperaments = new[] { "FORMAL", "AMIGAVEL", "MOTIVACIONAL", "EDUCATIVO", "URGENTE", "EMOTIVO" };
        if (!validTemperaments.Contains(Temperament.ToUpper()))
        {
            results.Add(new ValidationResult("O temperamento deve ser: FORMAL, AMIGAVEL, MOTIVACIONAL, EDUCATIVO, URGENTE ou EMOTIVO", new[] { nameof(Temperament) }));
        }

        return results;
    }
}

public class UpdateAIAgentDto : IValidatableObject
{
    [StringLength(255)]
    public string? Name { get; set; }

    [StringLength(2000)]
    public string? Description { get; set; }

    public string? AvatarBase64 { get; set; }

    public Guid? AIModelId { get; set; }

    [StringLength(50)]
    public string? Temperament { get; set; }

    [Range(1, 10000, ErrorMessage = "O comprimento máximo da resposta deve estar entre 1 e 10000")]
    public int? MaxResponseLength { get; set; }

    [Range(0.0, 1.0, ErrorMessage = "A temperatura deve estar entre 0.0 e 1.0")]
    public decimal? Temperature { get; set; }

    [Range(1, 1000, ErrorMessage = "O limite de mensagens de IA deve estar entre 1 e 1000")]
    public int? AIMessageLimit { get; set; }

    public bool? IsActive { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        if (!string.IsNullOrWhiteSpace(AvatarBase64) && 
            !System.Text.RegularExpressions.Regex.IsMatch(AvatarBase64, 
                @"^data:image\/(jpeg|jpg|png|gif);base64,[A-Za-z0-9+/]+=*$"))
        {
            results.Add(new ValidationResult("O avatar deve ser uma imagem base64 válida (data:image/jpeg;base64,...) ou vazio", new[] { nameof(AvatarBase64) }));
        }

        if (!string.IsNullOrWhiteSpace(Temperament))
        {
            var validTemperaments = new[] { "FORMAL", "AMIGAVEL", "MOTIVACIONAL", "EDUCATIVO", "URGENTE", "EMOTIVO" };
            if (!validTemperaments.Contains(Temperament.ToUpper()))
            {
                results.Add(new ValidationResult("O temperamento deve ser: FORMAL, AMIGAVEL, MOTIVACIONAL, EDUCATIVO, URGENTE ou EMOTIVO", new[] { nameof(Temperament) }));
            }
        }

        return results;
    }
}