namespace Rubia.Server.DTOs;

public class ZApiStatusDto
{
    public bool Connected { get; set; }
    public string Session { get; set; } = string.Empty;
    public bool Smartphone { get; set; }
}

public class QRCodeResponseDto
{
    public string? QRCode { get; set; }
    public string Status { get; set; } = string.Empty;
}

public class WebhookMessageDto
{
    public string Phone { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public string MessageId { get; set; } = string.Empty;
    public string SenderName { get; set; } = string.Empty;
    public DateTime Timestamp { get; set; }
    public string InstanceId { get; set; } = string.Empty;
}