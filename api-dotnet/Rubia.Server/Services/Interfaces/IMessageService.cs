using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface IMessageService
{
    Task<IEnumerable<MessageDto>> GetMessagesByConversationAsync(Guid conversationId, int limit = 50);
    Task<MessageDto?> GetByIdAsync(Guid messageId);
    Task<MessageDto> CreateAsync(CreateMessageDto dto);
    Task<MessageDto> CreateAsync(MessageDto dto, CancellationToken cancellationToken = default);
    Task<MessageDto?> UpdateStatusAsync(Guid messageId, MessageStatus status);
    Task<int> GetUnreadCountAsync(Guid conversationId, Guid userId);
    Task MarkAsReadAsync(Guid conversationId, Guid userId);
    Task<IEnumerable<MessageDto>> GetRecentMessagesAsync(long conversationId, int limit = 10, CancellationToken cancellationToken = default);
}