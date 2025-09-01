using Rubia.Server.Enums;

namespace Rubia.Server.Entities;

public class AudioMessage
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
    public ProcessingStatus Status { get; set; } = ProcessingStatus.RECEIVED;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? ProcessedAt { get; set; }
    public string? ErrorMessage { get; set; }
    public long Version { get; set; }
    public Guid? ConversationId { get; set; }
    public Conversation? Conversation { get; set; }
}

public enum MessageDirection
{
    INCOMING,
    OUTGOING
}

public enum ProcessingStatus
{
    RECEIVED,
    DOWNLOADING,
    PROCESSING,
    COMPLETED,
    FAILED
}