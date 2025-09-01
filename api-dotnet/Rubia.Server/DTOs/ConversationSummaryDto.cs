using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class ConversationSummaryDto
{
    public Guid Id { get; set; }
    public string? CustomerName { get; set; }
    public string? CustomerPhone { get; set; }
    public string? AssignedUserName { get; set; }
    public ConversationStatus Status { get; set; }
    public Channel Channel { get; set; }
    public int? Priority { get; set; }
    public DateTime UpdatedAt { get; set; }
    public long UnreadCount { get; set; }
    public string? LastMessageContent { get; set; }
    public DateTime? LastMessageTime { get; set; }
    public string? ChatLid { get; set; }
}