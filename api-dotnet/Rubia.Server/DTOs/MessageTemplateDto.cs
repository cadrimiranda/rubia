using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class MessageTemplateDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public string CompanyName { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public bool IsAIGenerated { get; set; }
    public Guid? CreatedByUserId { get; set; }
    public string? CreatedByUserName { get; set; }
    public Guid? AIAgentId { get; set; }
    public string? AIAgentName { get; set; }
    public string? Tone { get; set; }
    public Guid? LastEditedByUserId { get; set; }
    public string? LastEditedByUserName { get; set; }
    public int EditCount { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public DateTime? DeletedAt { get; set; }
}

public class CreateMessageTemplateDto : IValidatableObject
{
    [Required]
    public Guid CompanyId { get; set; }

    [Required]
    [StringLength(255)]
    public string Name { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsAIGenerated { get; set; } = false;

    public Guid? AIAgentId { get; set; }

    [StringLength(50)]
    public string? Tone { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        if (!string.IsNullOrWhiteSpace(Tone))
        {
            var validTones = new[] { "FORMAL", "INFORMAL", "DESCONTRAIDO", "EMPATICO" };
            if (!validTones.Contains(Tone.ToUpper()))
            {
                results.Add(new ValidationResult("O tom deve ser: FORMAL, INFORMAL, DESCONTRAIDO ou EMPATICO", new[] { nameof(Tone) }));
            }
        }

        return results;
    }
}

public class UpdateMessageTemplateDto : IValidatableObject
{
    [StringLength(255)]
    public string? Name { get; set; }

    public string? Content { get; set; }

    [StringLength(50)]
    public string? Tone { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        if (!string.IsNullOrWhiteSpace(Tone))
        {
            var validTones = new[] { "FORMAL", "INFORMAL", "DESCONTRAIDO", "EMPATICO" };
            if (!validTones.Contains(Tone.ToUpper()))
            {
                results.Add(new ValidationResult("O tom deve ser: FORMAL, INFORMAL, DESCONTRAIDO ou EMPATICO", new[] { nameof(Tone) }));
            }
        }

        return results;
    }
}