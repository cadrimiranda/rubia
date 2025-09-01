using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Integrations.Adapters;

public interface IMessagingAdapter
{
    string ProviderName { get; }
    Task<bool> IsAvailableAsync(CancellationToken cancellationToken = default);
    Task<MessagingResult> SendTextMessageAsync(string recipientId, string content, CancellationToken cancellationToken = default);
    Task<MessagingResult> SendMediaMessageAsync(string recipientId, string mediaUrl, MediaType mediaType, string? caption = null, CancellationToken cancellationToken = default);
    Task<MessagingResult> SendTemplateMessageAsync(string recipientId, string templateName, Dictionary<string, string> parameters, CancellationToken cancellationToken = default);
    Task<QrCodeResult?> GetQrCodeAsync(string instanceId, CancellationToken cancellationToken = default);
    Task<ConnectionStatus> GetConnectionStatusAsync(string instanceId, CancellationToken cancellationToken = default);
    Task<bool> DisconnectInstanceAsync(string instanceId, CancellationToken cancellationToken = default);
    Task<IncomingMessage?> ParseWebhookPayloadAsync(object payload, CancellationToken cancellationToken = default);
    Task<bool> ValidateWebhookAsync(string signature, string payload, string secret, CancellationToken cancellationToken = default);
}

public class MessagingResult
{
    public bool Success { get; set; }
    public string? ExternalMessageId { get; set; }
    public string? ErrorMessage { get; set; }
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
    public Dictionary<string, object>? Metadata { get; set; }
}

public class QrCodeResult
{
    public string? QrCodeData { get; set; }
    public string? QrCodeImageUrl { get; set; }
    public DateTime ExpiresAt { get; set; }
    public bool IsExpired => DateTime.UtcNow > ExpiresAt;
}

public class IncomingMessage
{
    public string ExternalMessageId { get; set; } = string.Empty;
    public string SenderId { get; set; } = string.Empty;
    public string SenderName { get; set; } = string.Empty;
    public string RecipientId { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public MessageType MessageType { get; set; }
    public DateTime Timestamp { get; set; }
    public MediaInfo? Media { get; set; }
    public Dictionary<string, object>? Metadata { get; set; }
}

public class MediaInfo
{
    public string? Url { get; set; }
    public string? FileName { get; set; }
    public string? MimeType { get; set; }
    public long? Size { get; set; }
    public MediaType MediaType { get; set; }
}

public enum ConnectionStatus
{
    Disconnected,
    Connecting,
    Connected,
    QrCodeRequired,
    Error
}

public enum MessageType
{
    Text,
    Image,
    Audio,
    Video,
    Document,
    Location,
    Contact
}