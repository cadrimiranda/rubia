using System.ComponentModel.DataAnnotations;
using System.Text.RegularExpressions;
using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class CreateCompanyDto : IValidatableObject
{
    [Required(ErrorMessage = "Nome é obrigatório")]
    [StringLength(255, MinimumLength = 2, ErrorMessage = "Nome deve ter entre 2 e 255 caracteres")]
    public string Name { get; set; } = string.Empty;

    [Required(ErrorMessage = "Slug é obrigatório")]
    [StringLength(255, MinimumLength = 2, ErrorMessage = "Slug deve ter entre 2 e 255 caracteres")]
    public string Slug { get; set; } = string.Empty;

    [StringLength(1000, ErrorMessage = "Descrição não pode exceder 1000 caracteres")]
    public string? Description { get; set; }

    [EmailAddress(ErrorMessage = "Email de contato deve ter formato válido")]
    public string? ContactEmail { get; set; }

    public string? ContactPhone { get; set; }

    public string? LogoUrl { get; set; }

    public bool IsActive { get; set; } = true;

    public CompanyPlanType PlanType { get; set; } = CompanyPlanType.Basic;

    public int MaxUsers { get; set; } = 10;

    public int MaxWhatsappNumbers { get; set; } = 1;

    public int MaxAiAgents { get; set; } = 1;

    [Required(ErrorMessage = "ID do grupo da empresa é obrigatório")]
    public Guid CompanyGroupId { get; set; }

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        var results = new List<ValidationResult>();

        // Validar slug
        if (!string.IsNullOrEmpty(Slug))
        {
            var slugRegex = new Regex(@"^[a-z0-9-]+$");
            if (!slugRegex.IsMatch(Slug))
            {
                results.Add(new ValidationResult(
                    "Slug deve conter apenas letras minúsculas, números e hífens",
                    new[] { nameof(Slug) }));
            }
        }

        // Validar telefone
        if (!string.IsNullOrEmpty(ContactPhone))
        {
            var phoneRegex = new Regex(@"^\+?[0-9.()-\s]*$");
            if (!phoneRegex.IsMatch(ContactPhone))
            {
                results.Add(new ValidationResult(
                    "Telefone de contato inválido",
                    new[] { nameof(ContactPhone) }));
            }
        }

        return results;
    }
}