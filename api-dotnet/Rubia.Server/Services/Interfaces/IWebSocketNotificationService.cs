using Rubia.Server.DTOs;
using Rubia.Server.Integrations.Adapters;

namespace Rubia.Server.Services.Interfaces;

public interface IWebSocketNotificationService
{
    Task NotifyNewMessageAsync(Guid companyId, Guid conversationId, MessageDto message);
    Task NotifyConversationStatusChangeAsync(Guid companyId, ConversationDto conversation);
    Task NotifyMessageStatusUpdateAsync(Guid conversationId, MessageDto message);
    Task NotifyUserAssignmentAsync(Guid companyId, Guid conversationId, Guid? userId, string? userName);
    Task NotifyUnreadCountUpdateAsync(Guid userId, Guid conversationId, int unreadCount);
    Task NotifyMessageSentAsync(Guid conversationId, MessageDto message);
    Task NotifyMessageReceivedAsync(Guid conversationId, MessageDto message);
    Task NotifyInstanceStatusChangedAsync(Guid instanceId, ConnectionStatus status);
    Task NotifyQrCodeUpdatedAsync(Guid instanceId, QrCodeResult qrCode);
}