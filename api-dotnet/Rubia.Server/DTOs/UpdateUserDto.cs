using System.ComponentModel.DataAnnotations;
using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class UpdateUserDto
{
    [StringLength(255, MinimumLength = 2, ErrorMessage = "Nome deve ter entre 2 e 255 caracteres")]
    public string? Name { get; set; }

    [EmailAddress(ErrorMessage = "Email deve ter formato válido")]
    public string? Email { get; set; }

    [StringLength(100, MinimumLength = 6, ErrorMessage = "Senha deve ter entre 6 e 100 caracteres")]
    public string? Password { get; set; }

    public Guid? DepartmentId { get; set; }

    public UserRole? Role { get; set; }

    public string? AvatarUrl { get; set; }

    public DateOnly? BirthDate { get; set; }

    public double? Weight { get; set; }

    public double? Height { get; set; }

    [StringLength(500, ErrorMessage = "Endereço deve ter no máximo 500 caracteres")]
    public string? Address { get; set; }
}