using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IUnreadMessageCountService
{
    Task<IEnumerable<UnreadMessageCountDto>> GetUserUnreadCountsAsync(Guid userId);
    Task<UnreadMessageCountDto?> GetConversationUnreadCountAsync(Guid userId, Guid conversationId);
    Task<int> GetTotalUnreadCountAsync(Guid userId);
    Task<Dictionary<Guid, int>> GetConversationUnreadCountsAsync(Guid userId, List<Guid> conversationIds);
    Task IncrementUnreadCountAsync(Guid userId, Guid conversationId, int increment = 1);
    Task MarkAsReadAsync(Guid userId, Guid conversationId);
    Task MarkAllAsReadAsync(Guid userId);
    Task RecalculateUnreadCountAsync(Guid userId, Guid conversationId);
    Task<UnreadSummaryDto> GetUnreadSummaryAsync(Guid userId);
}