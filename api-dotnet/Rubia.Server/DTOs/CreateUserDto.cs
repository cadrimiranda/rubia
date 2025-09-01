using System.ComponentModel.DataAnnotations;
using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class CreateUserDto
{
    [Required(ErrorMessage = "Nome é obrigatório")]
    [StringLength(255, MinimumLength = 2, ErrorMessage = "Nome deve ter entre 2 e 255 caracteres")]
    public string Name { get; set; } = string.Empty;

    [Required(ErrorMessage = "Email é obrigatório")]
    [EmailAddress(ErrorMessage = "Email deve ter formato válido")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Senha é obrigatória")]
    [StringLength(100, MinimumLength = 6, ErrorMessage = "Senha deve ter entre 6 e 100 caracteres")]
    public string Password { get; set; } = string.Empty;

    [Required(ErrorMessage = "ID da empresa é obrigatório")]
    public Guid CompanyId { get; set; }

    [Required(ErrorMessage = "ID do departamento é obrigatório")]
    public Guid DepartmentId { get; set; }

    [Required(ErrorMessage = "Role é obrigatório")]
    public UserRole Role { get; set; }

    public string? AvatarUrl { get; set; }

    public DateOnly? BirthDate { get; set; }

    public double? Weight { get; set; }

    public double? Height { get; set; }

    [StringLength(500, ErrorMessage = "Endereço deve ter no máximo 500 caracteres")]
    public string? Address { get; set; }
}