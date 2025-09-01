using Rubia.Server.Enums;
using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class MessageDraftDto
{
    public Guid Id { get; set; }
    public Guid UserId { get; set; }
    public Guid? ConversationId { get; set; }
    public string Content { get; set; } = string.Empty;
    public MessageType DraftType { get; set; }
    public bool IsTemplate { get; set; }
    public string? TemplateName { get; set; }
    public bool AutoSave { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class CreateMessageDraftDto
{
    [Required]
    public Guid UserId { get; set; }

    public Guid? ConversationId { get; set; }

    [Required]
    public string Content { get; set; } = string.Empty;

    public MessageType DraftType { get; set; } = MessageType.Text;

    public bool IsTemplate { get; set; } = false;

    public string? TemplateName { get; set; }

    public bool AutoSave { get; set; } = true;
}

public class UpdateMessageDraftDto
{
    public string? Content { get; set; }
    public MessageType? DraftType { get; set; }
    public bool? IsTemplate { get; set; }
    public string? TemplateName { get; set; }
}

public class SaveAsTemplateDto
{
    [Required]
    [MaxLength(100)]
    public string TemplateName { get; set; } = string.Empty;
}