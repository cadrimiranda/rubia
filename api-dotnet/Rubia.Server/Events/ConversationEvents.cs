namespace Rubia.Server.Events;

public class ConversationCreatedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(ConversationCreatedEvent);
    
    public Guid ConversationId { get; set; }
    public Guid CompanyId { get; set; }
    public Guid CustomerId { get; set; }
    public string Channel { get; set; } = string.Empty;
    public string? ChatLid { get; set; }
}

public class ConversationAssignedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(ConversationAssignedEvent);
    
    public Guid ConversationId { get; set; }
    public Guid CompanyId { get; set; }
    public Guid AssignedUserId { get; set; }
    public Guid? PreviousAssignedUserId { get; set; }
    public Guid AssignedByUserId { get; set; }
}

public class ConversationStatusChangedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(ConversationStatusChangedEvent);
    
    public Guid ConversationId { get; set; }
    public Guid CompanyId { get; set; }
    public string PreviousStatus { get; set; } = string.Empty;
    public string NewStatus { get; set; } = string.Empty;
    public Guid? ChangedByUserId { get; set; }
}

public class ConversationParticipantAddedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(ConversationParticipantAddedEvent);
    
    public Guid ConversationId { get; set; }
    public Guid UserId { get; set; }
    public string Role { get; set; } = string.Empty;
    public Guid? AddedByUserId { get; set; }
}

public class ConversationParticipantRemovedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(ConversationParticipantRemovedEvent);
    
    public Guid ConversationId { get; set; }
    public Guid UserId { get; set; }
    public string Reason { get; set; } = string.Empty;
    public Guid? RemovedByUserId { get; set; }
}