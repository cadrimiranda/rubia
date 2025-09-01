namespace Rubia.Server.Events;

public class CampaignStartedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(CampaignStartedEvent);
    
    public Guid CampaignId { get; set; }
    public Guid CompanyId { get; set; }
    public string CampaignName { get; set; } = string.Empty;
    public int TotalContacts { get; set; }
    public Guid? MessageTemplateId { get; set; }
}

public class CampaignStoppedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(CampaignStoppedEvent);
    
    public Guid CampaignId { get; set; }
    public Guid CompanyId { get; set; }
    public string CampaignName { get; set; } = string.Empty;
    public string Reason { get; set; } = string.Empty;
}

public class CampaignMessageSentEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(CampaignMessageSentEvent);
    
    public Guid CampaignId { get; set; }
    public Guid CampaignContactId { get; set; }
    public Guid CustomerId { get; set; }
    public string CustomerPhone { get; set; } = string.Empty;
    public string MessageContent { get; set; } = string.Empty;
}

public class CampaignMessageFailedEvent : IEvent
{
    public Guid EventId { get; } = Guid.NewGuid();
    public DateTime OccurredOn { get; } = DateTime.UtcNow;
    public string EventType { get; } = nameof(CampaignMessageFailedEvent);
    
    public Guid CampaignId { get; set; }
    public Guid CampaignContactId { get; set; }
    public Guid CustomerId { get; set; }
    public string CustomerPhone { get; set; } = string.Empty;
    public string ErrorMessage { get; set; } = string.Empty;
}