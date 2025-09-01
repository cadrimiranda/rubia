using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IConversationParticipantService
{
    Task<ConversationParticipantDto> AddParticipantAsync(CreateConversationParticipantDto createDto, CancellationToken cancellationToken = default);
    Task<bool> RemoveParticipantAsync(Guid participantId, CancellationToken cancellationToken = default);
    Task<IEnumerable<ConversationParticipantDto>> GetConversationParticipantsAsync(Guid conversationId, CancellationToken cancellationToken = default);
    Task<IEnumerable<ConversationDto>> GetUserConversationsAsync(Guid userId, CancellationToken cancellationToken = default);
    
    Task<bool> AssignConversationToUserAsync(Guid conversationId, Guid userId, Guid assignedByUserId, CancellationToken cancellationToken = default);
    Task<bool> UnassignConversationFromUserAsync(Guid conversationId, Guid userId, CancellationToken cancellationToken = default);
    Task<bool> TransferConversationAsync(Guid conversationId, Guid fromUserId, Guid toUserId, Guid transferredByUserId, string? reason = null, CancellationToken cancellationToken = default);
    
    Task<ConversationParticipantDto?> GetActiveParticipantAsync(Guid conversationId, CancellationToken cancellationToken = default);
    Task<IEnumerable<ConversationParticipantDto>> GetParticipantHistoryAsync(Guid conversationId, CancellationToken cancellationToken = default);
    
    Task<bool> SetParticipantPermissionAsync(Guid participantId, string permission, bool allowed, CancellationToken cancellationToken = default);
    Task<ConversationParticipantStatsDto> GetParticipantStatsAsync(Guid userId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default);
}