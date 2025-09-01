using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class UserAIAgentDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public string? CompanyName { get; set; }
    public Guid UserId { get; set; }
    public string? UserName { get; set; }
    public Guid AiAgentId { get; set; }
    public string? AiAgentName { get; set; }
    public bool IsDefault { get; set; }
    public DateTime AssignedAt { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateUserAIAgentDto
{
    [Required(ErrorMessage = "Company ID is required")]
    public Guid CompanyId { get; set; }

    [Required(ErrorMessage = "User ID is required")]
    public Guid UserId { get; set; }

    [Required(ErrorMessage = "AI Agent ID is required")]
    public Guid AiAgentId { get; set; }

    public bool IsDefault { get; set; } = false;
}

public class UpdateUserAIAgentDto
{
    public Guid? AiAgentId { get; set; }
    public bool? IsDefault { get; set; }
}