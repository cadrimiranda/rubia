namespace Rubia.Server.DTOs;

public class AuthResponse
{
    public string Token { get; set; } = string.Empty;
    public long ExpiresIn { get; set; }
    public string TokenType { get; set; } = "Bearer";
    public bool RequiresWhatsAppSetup { get; set; }
    public UserInfo User { get; set; } = new();
}

public class UserInfo
{
    public Guid Id { get; set; }
    public string Name { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string Role { get; set; } = string.Empty;
    public Guid CompanyId { get; set; }
    public string CompanySlug { get; set; } = string.Empty;
    public Guid CompanyGroupId { get; set; }
    public Guid DepartmentId { get; set; }
}