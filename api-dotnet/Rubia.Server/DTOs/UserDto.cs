using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class UserDto
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public Guid CompanyId { get; set; }
    public Guid DepartmentId { get; set; }
    public string? DepartmentName { get; set; }
    public UserRole Role { get; set; }
    public string? AvatarUrl { get; set; }
    public bool IsOnline { get; set; }
    public DateTime? LastSeen { get; set; }
    public DateOnly? BirthDate { get; set; }
    public double? Weight { get; set; }
    public double? Height { get; set; }
    public string? Address { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}