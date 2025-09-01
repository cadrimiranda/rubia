using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class MessageDto
{
    public Guid Id { get; set; }
    public Guid ConversationId { get; set; }
    public string? Content { get; set; }
    public SenderType SenderType { get; set; }
    public Guid? SenderId { get; set; }
    public MessageStatus Status { get; set; }
    public DateTime? DeliveredAt { get; set; }
    public DateTime? ReadAt { get; set; }
    public string? ExternalMessageId { get; set; }
    public bool? IsAiGenerated { get; set; }
    public double? AiConfidence { get; set; }
    public string? Sentiment { get; set; }
    public string? Keywords { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public MediaDto? Media { get; set; }
}

public class CreateMessageDto
{
    [Required]
    public Guid ConversationId { get; set; }

    public string? Content { get; set; }

    [Required]
    public SenderType SenderType { get; set; }

    public Guid? SenderId { get; set; }
    public string? ExternalMessageId { get; set; }
    public bool? IsAiGenerated { get; set; }
    public double? AiConfidence { get; set; }
    public Guid? AiAgentId { get; set; }
    public Guid? MessageTemplateId { get; set; }
}

public class UpdateMessageStatusDto
{
    [Required]
    public MessageStatus Status { get; set; }
}

public class MediaDto
{
    public Guid Id { get; set; }
    public MediaType MediaType { get; set; }
    public string? FileUrl { get; set; }
    public string? FileName { get; set; }
    public long? FileSize { get; set; }
}

public class MarkAsReadDto
{
    [Required]
    public Guid ConversationId { get; set; }
}