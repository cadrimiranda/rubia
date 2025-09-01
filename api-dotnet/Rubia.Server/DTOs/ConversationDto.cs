using Rubia.Server.Enums;

namespace Rubia.Server.DTOs;

public class ConversationDto
{
    public Guid Id { get; set; }
    public Guid CompanyId { get; set; }
    public Channel Channel { get; set; }
    public ConversationStatus Status { get; set; }
    public int? Priority { get; set; }
    public ConversationType ConversationType { get; set; }
    public string? ChatLid { get; set; }
    public bool AiAutoResponseEnabled { get; set; }
    public int AiMessagesUsed { get; set; }
    public DateTime? AiLimitReachedAt { get; set; }
    public Guid? AssignedUserId { get; set; }
    public string? AssignedUserName { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public List<ConversationParticipantDto> Participants { get; set; } = new();
    public List<MessageSummaryDto> Messages { get; set; } = new();
}

public class ConversationParticipantDto
{
    public Guid Id { get; set; }
    public string? CustomerName { get; set; }
    public string? CustomerPhone { get; set; }
    public bool IsActive { get; set; }
    public DateTime JoinedAt { get; set; }
}

public class MessageSummaryDto
{
    public Guid Id { get; set; }
    public string? Content { get; set; }
    public SenderType SenderType { get; set; }
    public MessageStatus Status { get; set; }
    public DateTime CreatedAt { get; set; }
    public bool HasMedia { get; set; }
}

public class CreateConversationDto
{
    public Guid CompanyId { get; set; }
    public Channel Channel { get; set; } = Channel.WHATSAPP;
    public ConversationType ConversationType { get; set; } = ConversationType.ONE_TO_ONE;
    public string? ChatLid { get; set; }
    public bool? AiAutoResponseEnabled { get; set; }
    public List<Guid>? CustomerIds { get; set; }
}

public class UpdateConversationStatusDto
{
    public ConversationStatus Status { get; set; }
}

public class AssignUserDto
{
    public Guid? UserId { get; set; }
}