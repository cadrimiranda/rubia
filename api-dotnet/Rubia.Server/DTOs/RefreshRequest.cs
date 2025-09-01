using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class RefreshRequest
{
    [Required]
    public string Token { get; set; } = string.Empty;
}