using Microsoft.AspNetCore.SignalR;
using Rubia.Server.DTOs;
using Rubia.Server.Hubs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class WebSocketNotificationService : IWebSocketNotificationService
{
    private readonly IHubContext<ChatHub> _hubContext;
    private readonly ILogger<WebSocketNotificationService> _logger;

    public WebSocketNotificationService(IHubContext<ChatHub> hubContext, ILogger<WebSocketNotificationService> logger)
    {
        _hubContext = hubContext;
        _logger = logger;
    }

    public async Task NotifyNewMessageAsync(Guid companyId, Guid conversationId, MessageDto message)
    {
        try
        {
            await _hubContext.Clients.Group($"company_{companyId}")
                .SendAsync("NewMessage", new
                {
                    ConversationId = conversationId,
                    Message = message
                });

            await _hubContext.Clients.Group($"conversation_{conversationId}")
                .SendAsync("MessageReceived", message);

            _logger.LogDebug("New message notification sent for conversation {ConversationId}", conversationId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending new message notification for conversation {ConversationId}", conversationId);
        }
    }

    public async Task NotifyConversationStatusChangeAsync(Guid companyId, ConversationDto conversation)
    {
        try
        {
            await _hubContext.Clients.Group($"company_{companyId}")
                .SendAsync("ConversationStatusChanged", new
                {
                    ConversationId = conversation.Id,
                    Status = conversation.Status.ToString(),
                    Conversation = conversation
                });

            _logger.LogDebug("Conversation status change notification sent for conversation {ConversationId}", conversation.Id);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending conversation status change notification for conversation {ConversationId}", conversation.Id);
        }
    }

    public async Task NotifyMessageStatusUpdateAsync(Guid conversationId, MessageDto message)
    {
        try
        {
            await _hubContext.Clients.Group($"conversation_{conversationId}")
                .SendAsync("MessageStatusUpdated", new
                {
                    MessageId = message.Id,
                    Status = message.Status.ToString(),
                    DeliveredAt = message.DeliveredAt,
                    ReadAt = message.ReadAt
                });

            _logger.LogDebug("Message status update notification sent for message {MessageId}", message.Id);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending message status update notification for message {MessageId}", message.Id);
        }
    }

    public async Task NotifyUserAssignmentAsync(Guid companyId, Guid conversationId, Guid? userId, string? userName)
    {
        try
        {
            await _hubContext.Clients.Group($"company_{companyId}")
                .SendAsync("ConversationAssigned", new
                {
                    ConversationId = conversationId,
                    AssignedUserId = userId,
                    AssignedUserName = userName
                });

            _logger.LogDebug("User assignment notification sent for conversation {ConversationId}", conversationId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending user assignment notification for conversation {ConversationId}", conversationId);
        }
    }

    public async Task NotifyUnreadCountUpdateAsync(Guid userId, Guid conversationId, int unreadCount)
    {
        try
        {
            var userSessions = ChatHub.GetUserSessions();
            var userConnections = userSessions.Where(s => s.Value.UserId == userId)
                .Select(s => s.Key).ToList();

            if (userConnections.Any())
            {
                await _hubContext.Clients.Clients(userConnections)
                    .SendAsync("UnreadCountUpdated", new
                    {
                        ConversationId = conversationId,
                        UnreadCount = unreadCount
                    });

                _logger.LogDebug("Unread count update notification sent to user {UserId}", userId);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending unread count update notification to user {UserId}", userId);
        }
    }

    public async Task NotifyMessageSentAsync(Guid conversationId, MessageDto message)
    {
        try
        {
            await _hubContext.Clients.Group($"conversation_{conversationId}")
                .SendAsync("MessageSent", new
                {
                    MessageId = message.Id,
                    ConversationId = conversationId,
                    Content = message.Content,
                    SenderType = message.SenderType.ToString(),
                    CreatedAt = message.CreatedAt
                });

            _logger.LogDebug("Message sent notification sent for message {MessageId}", message.Id);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending message sent notification for message {MessageId}", message.Id);
        }
    }

    public async Task NotifyMessageReceivedAsync(Guid conversationId, MessageDto message)
    {
        try
        {
            await _hubContext.Clients.Group($"conversation_{conversationId}")
                .SendAsync("MessageReceived", new
                {
                    MessageId = message.Id,
                    ConversationId = conversationId,
                    Content = message.Content,
                    SenderType = message.SenderType.ToString(),
                    CreatedAt = message.CreatedAt,
                    Sentiment = message.Sentiment,
                    Keywords = message.Keywords
                });

            _logger.LogDebug("Message received notification sent for message {MessageId}", message.Id);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending message received notification for message {MessageId}", message.Id);
        }
    }

    public async Task NotifyInstanceStatusChangedAsync(Guid instanceId, ConnectionStatus status)
    {
        try
        {
            await _hubContext.Clients.All
                .SendAsync("WhatsAppInstanceStatusChanged", new
                {
                    InstanceId = instanceId,
                    Status = status.ToString(),
                    Timestamp = DateTime.UtcNow
                });

            _logger.LogDebug("Instance status change notification sent for instance {InstanceId}: {Status}", instanceId, status);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending instance status change notification for instance {InstanceId}", instanceId);
        }
    }

    public async Task NotifyQrCodeUpdatedAsync(Guid instanceId, QrCodeResult qrCode)
    {
        try
        {
            await _hubContext.Clients.All
                .SendAsync("WhatsAppQrCodeUpdated", new
                {
                    InstanceId = instanceId,
                    QrCodeData = qrCode.QrCodeData,
                    QrCodeImageUrl = qrCode.QrCodeImageUrl,
                    ExpiresAt = qrCode.ExpiresAt,
                    Timestamp = DateTime.UtcNow
                });

            _logger.LogDebug("QR Code update notification sent for instance {InstanceId}", instanceId);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending QR Code update notification for instance {InstanceId}", instanceId);
        }
    }
}