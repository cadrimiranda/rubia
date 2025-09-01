using System.ComponentModel.DataAnnotations;

namespace Rubia.Server.DTOs;

public class UnreadMessageCountDto
{
    public Guid UserId { get; set; }
    public Guid ConversationId { get; set; }
    public int Count { get; set; }
    public DateTime? LastReadAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}

public class UnreadSummaryDto
{
    public int TotalUnreadMessages { get; set; }
    public int ConversationsWithUnread { get; set; }
    public DateTime? OldestUnreadMessage { get; set; }
}

public class MarkAsReadDto
{
    [Required]
    public Guid ConversationId { get; set; }
}

public class BulkUnreadCountDto
{
    [Required]
    public List<Guid> ConversationIds { get; set; } = new();
}