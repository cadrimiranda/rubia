namespace Rubia.Server.Events;

public class MessageReceivedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(MessageReceivedEvent);
    
    public Guid MessageId { get; set; }
    public Guid ConversationId { get; set; }
    public Guid CompanyId { get; set; }
    public string Content { get; set; } = string.Empty;
    public string SenderType { get; set; } = string.Empty;
    public Guid? SenderId { get; set; }
    public string? ExternalMessageId { get; set; }
}

public class MessageSentEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(MessageSentEvent);
    
    public Guid MessageId { get; set; }
    public Guid ConversationId { get; set; }
    public Guid CompanyId { get; set; }
    public string Content { get; set; } = string.Empty;
    public string SenderType { get; set; } = string.Empty;
    public Guid? SenderId { get; set; }
    public bool? IsAiGenerated { get; set; }
}

public class MessageDeliveredEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(MessageDeliveredEvent);
    
    public Guid MessageId { get; set; }
    public string? ExternalMessageId { get; set; }
    public DateTime DeliveredAt { get; set; }
}

public class MessageReadEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(MessageReadEvent);
    
    public Guid MessageId { get; set; }
    public string? ExternalMessageId { get; set; }
    public DateTime ReadAt { get; set; }
}