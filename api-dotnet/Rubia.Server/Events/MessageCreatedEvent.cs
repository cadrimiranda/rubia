using Rubia.Server.DTOs;

namespace Rubia.Server.Events;

public class MessageCreatedEvent
{
    public MessageDto Message { get; set; } = null!;
    public Guid CompanyId { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}

public class ConversationStatusChangedEvent
{
    public ConversationDto Conversation { get; set; } = null!;
    public Guid CompanyId { get; set; }
    public DateTime ChangedAt { get; set; } = DateTime.UtcNow;
}

public class MessageStatusUpdatedEvent
{
    public MessageDto Message { get; set; } = null!;
    public Guid ConversationId { get; set; }
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}

public class UserAssignedEvent
{
    public Guid ConversationId { get; set; }
    public Guid CompanyId { get; set; }
    public Guid? UserId { get; set; }
    public string? UserName { get; set; }
    public DateTime AssignedAt { get; set; } = DateTime.UtcNow;
}