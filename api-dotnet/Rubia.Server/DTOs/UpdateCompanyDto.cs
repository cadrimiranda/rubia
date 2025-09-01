using System.ComponentModel.DataAnnotations;
using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class UpdateCompanyDto
{
    [StringLength(255, MinimumLength = 2, ErrorMessage = "Nome deve ter entre 2 e 255 caracteres")]
    public string? Name { get; set; }

    [StringLength(1000, ErrorMessage = "Descrição não pode exceder 1000 caracteres")]
    public string? Description { get; set; }

    [EmailAddress(ErrorMessage = "Email de contato deve ter formato válido")]
    public string? ContactEmail { get; set; }

    public string? ContactPhone { get; set; }

    public string? LogoUrl { get; set; }

    public bool? IsActive { get; set; }

    public CompanyPlanType? PlanType { get; set; }

    public int? MaxUsers { get; set; }

    public int? MaxWhatsappNumbers { get; set; }

    public int? MaxAiAgents { get; set; }
}