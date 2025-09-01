using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class CreateCompanyGroupDto
{
    [Required(ErrorMessage = "Nome é obrigatório")]
    [StringLength(255, MinimumLength = 2, ErrorMessage = "Nome deve ter entre 2 e 255 caracteres")]
    public string Name { get; set; } = string.Empty;

    [StringLength(1000, ErrorMessage = "Descrição não pode exceder 1000 caracteres")]
    public string? Description { get; set; }

    public bool IsActive { get; set; } = true;
}