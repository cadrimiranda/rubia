using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class ConversationMediaDto
{
    public Guid Id { get; set; }
    public string FileName { get; set; } = string.Empty;
    public string FileUrl { get; set; } = string.Empty;
    public MediaType MediaType { get; set; }
    public long? FileSize { get; set; }
    public string? MimeType { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateConversationMediaDto
{
    [Required]
    [MaxLength(255)]
    public string FileName { get; set; } = string.Empty;

    [Required]
    public string FileUrl { get; set; } = string.Empty;

    [Required]
    public MediaType MediaType { get; set; }

    public long? FileSize { get; set; }
    public string? MimeType { get; set; }
}

public class MediaUploadDto
{
    [Required]
    public IFormFile File { get; set; } = null!;

    [Required]
    public MediaType MediaType { get; set; }
}