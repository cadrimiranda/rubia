using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface IMessagingService
{
    Task<MessageDto> SendMessageAsync(Guid conversationId, string content, SenderType senderType, Guid? senderId = null, CancellationToken cancellationToken = default);
    Task<MessageDto> ProcessIncomingMessageAsync(MessageDto incomingMessage, CancellationToken cancellationToken = default);
    Task<bool> HandleWebhookMessageAsync(object webhookPayload, string provider, CancellationToken cancellationToken = default);
    Task<MessageDto> SendTemplateMessageAsync(Guid conversationId, Guid templateId, Dictionary<string, string> parameters, CancellationToken cancellationToken = default);
    Task<MessageDto> SendMediaMessageAsync(Guid conversationId, string mediaUrl, MediaType mediaType, SenderType senderType, Guid? senderId = null, CancellationToken cancellationToken = default);
    Task<bool> DeliveryStatusUpdateAsync(string externalMessageId, MessageStatus newStatus, CancellationToken cancellationToken = default);
    Task<IEnumerable<MessageDto>> GetConversationHistoryAsync(Guid conversationId, int limit = 50, CancellationToken cancellationToken = default);
    Task<MessageDto> ForwardMessageAsync(Guid sourceConversationId, Guid targetConversationId, Guid messageId, CancellationToken cancellationToken = default);
    Task<bool> MarkConversationAsReadAsync(Guid conversationId, Guid userId, CancellationToken cancellationToken = default);
    Task<int> GetUnreadCountAsync(Guid conversationId, Guid userId, CancellationToken cancellationToken = default);
}