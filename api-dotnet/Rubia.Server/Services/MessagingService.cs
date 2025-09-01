using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;
using System.Text.Json;

namespace Rubia.Server.Services;

public class MessagingService : IMessagingService
{
    private readonly IMessageService _messageService;
    private readonly IConversationService _conversationService;
    private readonly IOpenAIService _openAIService;
    private readonly IWhatsAppService _whatsAppService;
    private readonly IWebSocketNotificationService _notificationService;
    private readonly ILogger<MessagingService> _logger;
    private readonly RubiaDbContext _context;

    public MessagingService(
        IMessageService messageService,
        IConversationService conversationService,
        IOpenAIService openAIService,
        IWhatsAppService whatsAppService,
        IWebSocketNotificationService notificationService,
        ILogger<MessagingService> logger,
        RubiaDbContext context)
    {
        _messageService = messageService;
        _conversationService = conversationService;
        _openAIService = openAIService;
        _whatsAppService = whatsAppService;
        _notificationService = notificationService;
        _logger = logger;
        _context = context;
    }

    public async Task<MessageDto> SendMessageAsync(Guid conversationId, string content, SenderType senderType, Guid? senderId = null, CancellationToken cancellationToken = default)
    {
        try
        {
            // Validate conversation exists
            var conversation = await _conversationService.GetByIdAsync(conversationId);
            if (conversation == null)
            {
                throw new ArgumentException("Conversation not found", nameof(conversationId));
            }

            // Create message
            var message = new MessageDto
            {
                Id = Guid.NewGuid(),
                ConversationId = conversationId,
                Content = content,
                SenderType = senderType,
                SenderId = senderId,
                Status = MessageStatus.Pending,
                CreatedAt = DateTime.UtcNow,
                IsAiGenerated = senderType == SenderType.Bot
            };

            // Save message to database
            var savedMessage = await _messageService.CreateAsync(message, cancellationToken);

            // Send via external provider (WhatsApp, etc.)
            if (senderType == SenderType.User || senderType == SenderType.Bot)
            {
                var success = await SendToExternalProvider(conversation, savedMessage, cancellationToken);
                if (success)
                {
                    await _messageService.UpdateStatusAsync(savedMessage.Id.Value, MessageStatus.Sent);
                }
            }

            // Notify via WebSocket
            await _notificationService.NotifyMessageSentAsync(conversationId, savedMessage);

            _logger.LogInformation("Message sent successfully: {MessageId} for conversation {ConversationId}", 
                savedMessage.Id, conversationId);

            return savedMessage;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending message for conversation {ConversationId}", conversationId);
            throw;
        }
    }

    public async Task<MessageDto> ProcessIncomingMessageAsync(MessageDto incomingMessage, CancellationToken cancellationToken = default)
    {
        try
        {
            // Process with OpenAI for sentiment analysis and auto-response
            var processedMessage = await _openAIService.ProcessIncomingMessageAsync(
                incomingMessage, 
                (long)incomingMessage.ConversationId.GetHashCode(), 
                cancellationToken);

            // Update conversation status if needed
            await UpdateConversationStatusAsync(processedMessage.ConversationId, cancellationToken);

            // Notify via WebSocket
            await _notificationService.NotifyMessageReceivedAsync(processedMessage.ConversationId, processedMessage);

            _logger.LogInformation("Incoming message processed: {MessageId} for conversation {ConversationId}", 
                processedMessage.Id, processedMessage.ConversationId);

            return processedMessage;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing incoming message: {MessageId}", incomingMessage.Id);
            throw;
        }
    }

    public async Task<bool> HandleWebhookMessageAsync(object webhookPayload, string provider, CancellationToken cancellationToken = default)
    {
        try
        {
            switch (provider.ToLower())
            {
                case "whatsapp":
                case "zapi":
                    return await HandleWhatsAppWebhook(webhookPayload, cancellationToken);
                case "twilio":
                    return await HandleTwilioWebhook(webhookPayload, cancellationToken);
                default:
                    _logger.LogWarning("Unknown webhook provider: {Provider}", provider);
                    return false;
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling webhook from provider {Provider}", provider);
            return false;
        }
    }

    public async Task<MessageDto> SendTemplateMessageAsync(Guid conversationId, Guid templateId, Dictionary<string, string> parameters, CancellationToken cancellationToken = default)
    {
        try
        {
            // Get template (assuming you have a template service)
            // For now, using a simple placeholder
            var templateContent = $"Template {templateId} with parameters: {string.Join(", ", parameters.Select(p => $"{p.Key}={p.Value}"))}";
            
            return await SendMessageAsync(conversationId, templateContent, SenderType.Bot, null, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending template message: {TemplateId} for conversation {ConversationId}", 
                templateId, conversationId);
            throw;
        }
    }

    public async Task<MessageDto> SendMediaMessageAsync(Guid conversationId, string mediaUrl, MediaType mediaType, SenderType senderType, Guid? senderId = null, CancellationToken cancellationToken = default)
    {
        try
        {
            var content = $"[{mediaType}] {mediaUrl}";
            return await SendMessageAsync(conversationId, content, senderType, senderId, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending media message for conversation {ConversationId}", conversationId);
            throw;
        }
    }

    public async Task<bool> DeliveryStatusUpdateAsync(string externalMessageId, MessageStatus newStatus, CancellationToken cancellationToken = default)
    {
        try
        {
            var message = await _context.Messages
                .FirstOrDefaultAsync(m => m.ExternalMessageId == externalMessageId, cancellationToken);

            if (message == null)
            {
                _logger.LogWarning("Message not found for external ID: {ExternalMessageId}", externalMessageId);
                return false;
            }

            await _messageService.UpdateStatusAsync(message.Id, newStatus);
            
            _logger.LogInformation("Message status updated: {MessageId} to {Status}", message.Id, newStatus);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating delivery status for external message {ExternalMessageId}", externalMessageId);
            return false;
        }
    }

    public async Task<IEnumerable<MessageDto>> GetConversationHistoryAsync(Guid conversationId, int limit = 50, CancellationToken cancellationToken = default)
    {
        return await _messageService.GetMessagesByConversationAsync(conversationId, limit);
    }

    public async Task<MessageDto> ForwardMessageAsync(Guid sourceConversationId, Guid targetConversationId, Guid messageId, CancellationToken cancellationToken = default)
    {
        try
        {
            var originalMessage = await _messageService.GetByIdAsync(messageId);
            if (originalMessage == null)
            {
                throw new ArgumentException("Message not found", nameof(messageId));
            }

            var forwardedContent = $"[Forwarded] {originalMessage.Content}";
            return await SendMessageAsync(targetConversationId, forwardedContent, SenderType.User, null, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error forwarding message {MessageId}", messageId);
            throw;
        }
    }

    public async Task<bool> MarkConversationAsReadAsync(Guid conversationId, Guid userId, CancellationToken cancellationToken = default)
    {
        try
        {
            await _messageService.MarkAsReadAsync(conversationId, userId);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error marking conversation as read: {ConversationId} for user {UserId}", 
                conversationId, userId);
            return false;
        }
    }

    public async Task<int> GetUnreadCountAsync(Guid conversationId, Guid userId, CancellationToken cancellationToken = default)
    {
        return await _messageService.GetUnreadCountAsync(conversationId, userId);
    }

    // Private helper methods
    private async Task<bool> SendToExternalProvider(ConversationDto conversation, MessageDto message, CancellationToken cancellationToken)
    {
        try
        {
            // Send via WhatsApp or other providers
            if (conversation.Channel == Channel.WhatsApp)
            {
                // Use WhatsApp service to send message
                return await _whatsAppService.SendMessageAsync(conversation.ExternalId, message.Content, cancellationToken);
            }

            // Add other providers as needed
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending to external provider for conversation {ConversationId}", conversation.Id);
            return false;
        }
    }

    private async Task UpdateConversationStatusAsync(Guid conversationId, CancellationToken cancellationToken)
    {
        try
        {
            var conversation = await _conversationService.GetByIdAsync(conversationId);
            if (conversation != null && conversation.Status == ConversationStatus.Finalized)
            {
                // Reopen conversation if it was finalized
                // You might want to implement this in ConversationService
                _logger.LogInformation("Reopening finalized conversation: {ConversationId}", conversationId);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating conversation status: {ConversationId}", conversationId);
        }
    }

    private async Task<bool> HandleWhatsAppWebhook(object webhookPayload, CancellationToken cancellationToken)
    {
        try
        {
            // Parse WhatsApp webhook payload
            var jsonPayload = JsonSerializer.Serialize(webhookPayload);
            _logger.LogDebug("Received WhatsApp webhook: {Payload}", jsonPayload);

            // Process the webhook and create incoming message
            // This would depend on the specific WhatsApp provider format
            
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling WhatsApp webhook");
            return false;
        }
    }

    private async Task<bool> HandleTwilioWebhook(object webhookPayload, CancellationToken cancellationToken)
    {
        try
        {
            // Parse Twilio webhook payload
            var jsonPayload = JsonSerializer.Serialize(webhookPayload);
            _logger.LogDebug("Received Twilio webhook: {Payload}", jsonPayload);

            // Process the webhook and create incoming message
            
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error handling Twilio webhook");
            return false;
        }
    }
}