using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class AudioMessageDto
{
    public Guid Id { get; set; }
    public string MessageId { get; set; } = string.Empty;
    public string FromNumber { get; set; } = string.Empty;
    public string? ToNumber { get; set; }
    public MessageDirection Direction { get; set; }
    public string? AudioUrl { get; set; }
    public string? FilePath { get; set; }
    public string? MimeType { get; set; }
    public int? DurationSeconds { get; set; }
    public long? FileSizeBytes { get; set; }
    public ProcessingStatus Status { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? ProcessedAt { get; set; }
    public string? ErrorMessage { get; set; }
    public Guid? ConversationId { get; set; }
}

public class SendAudioRequestDto
{
    [Required]
    public string ToNumber { get; set; } = string.Empty;

    [Required]
    public string AudioUrl { get; set; } = string.Empty;
}

public class AudioStatsDto
{
    public long Total { get; set; }
    public long Received { get; set; }
    public long Processing { get; set; }
    public long Completed { get; set; }
    public long Failed { get; set; }
}