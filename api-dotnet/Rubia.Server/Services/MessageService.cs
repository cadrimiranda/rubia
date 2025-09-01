using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Events;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class MessageService : IMessageService
{
    private readonly RubiaDbContext _context;
    private readonly IEventBusService _eventBus;
    private readonly ILogger<MessageService> _logger;

    public MessageService(RubiaDbContext context, IEventBusService eventBus, ILogger<MessageService> logger)
    {
        _context = context;
        _eventBus = eventBus;
        _logger = logger;
    }

    public async Task<IEnumerable<MessageDto>> GetMessagesByConversationAsync(Guid conversationId, int limit = 50)
    {
        var messages = await _context.Messages
            .Include(m => m.Media)
            .Where(m => m.ConversationId == conversationId)
            .OrderByDescending(m => m.CreatedAt)
            .Take(limit)
            .ToListAsync();

        return messages.OrderBy(m => m.CreatedAt).Select(MapToDto);
    }

    public async Task<MessageDto?> GetByIdAsync(Guid messageId)
    {
        var message = await _context.Messages
            .Include(m => m.Media)
            .Include(m => m.Conversation)
            .FirstOrDefaultAsync(m => m.Id == messageId);

        return message != null ? MapToDto(message) : null;
    }

    public async Task<MessageDto> CreateAsync(CreateMessageDto dto)
    {
        var message = new Message
        {
            Id = Guid.NewGuid(),
            ConversationId = dto.ConversationId,
            Content = dto.Content,
            SenderType = dto.SenderType,
            SenderId = dto.SenderId,
            Status = MessageStatus.Sent,
            ExternalMessageId = dto.ExternalMessageId,
            IsAiGenerated = dto.IsAiGenerated,
            AiConfidence = dto.AiConfidence,
            AiAgentId = dto.AiAgentId,
            MessageTemplateId = dto.MessageTemplateId
        };

        _context.Messages.Add(message);
        await _context.SaveChangesAsync();

        var createdMessage = await GetByIdAsync(message.Id) ?? throw new InvalidOperationException("Failed to create message");

        // Publish event
        var conversation = await _context.Conversations.FindAsync(dto.ConversationId);
        if (conversation != null)
        {
            await _eventBus.PublishAsync(new MessageCreatedEvent
            {
                Message = createdMessage,
                CompanyId = conversation.CompanyId
            });
        }

        return createdMessage;
    }

    public async Task<MessageDto?> UpdateStatusAsync(Guid messageId, MessageStatus status)
    {
        var message = await _context.Messages.FindAsync(messageId);
        if (message == null)
            return null;

        message.Status = status;

        if (status == MessageStatus.Delivered && !message.DeliveredAt.HasValue)
        {
            message.DeliveredAt = DateTime.UtcNow;
        }
        else if (status == MessageStatus.Read && !message.ReadAt.HasValue)
        {
            message.ReadAt = DateTime.UtcNow;
            if (!message.DeliveredAt.HasValue)
            {
                message.DeliveredAt = DateTime.UtcNow;
            }
        }

        await _context.SaveChangesAsync();

        return await GetByIdAsync(messageId);
    }

    public async Task<int> GetUnreadCountAsync(Guid conversationId, Guid userId)
    {
        var count = await _context.UnreadMessageCounts
            .Where(umc => umc.ConversationId == conversationId && umc.UserId == userId)
            .Select(umc => umc.Count)
            .FirstOrDefaultAsync();

        return count;
    }

    public async Task MarkAsReadAsync(Guid conversationId, Guid userId)
    {
        var unreadCount = await _context.UnreadMessageCounts
            .FirstOrDefaultAsync(umc => umc.ConversationId == conversationId && umc.UserId == userId);

        if (unreadCount != null)
        {
            unreadCount.Count = 0;
            unreadCount.LastReadAt = DateTime.UtcNow;
        }
        else
        {
            unreadCount = new UnreadMessageCount
            {
                Id = Guid.NewGuid(),
                ConversationId = conversationId,
                UserId = userId,
                Count = 0,
                LastReadAt = DateTime.UtcNow
            };
            _context.UnreadMessageCounts.Add(unreadCount);
        }

        await _context.SaveChangesAsync();
    }

    public async Task<MessageDto> CreateAsync(MessageDto dto, CancellationToken cancellationToken = default)
    {
        var message = new Message
        {
            Id = dto.Id ?? Guid.NewGuid(),
            ConversationId = dto.ConversationId,
            Content = dto.Content,
            SenderType = dto.SenderType,
            SenderId = dto.SenderId,
            Status = dto.Status,
            ExternalMessageId = dto.ExternalMessageId,
            IsAiGenerated = dto.IsAiGenerated,
            AiConfidence = dto.AiConfidence,
            CreatedAt = dto.CreatedAt,
            Sentiment = dto.Sentiment,
            Keywords = dto.Keywords
        };

        _context.Messages.Add(message);
        await _context.SaveChangesAsync(cancellationToken);

        // Publish event for auto-processing
        await _eventBus.PublishAsync(new MessageCreatedEvent
        {
            MessageId = message.Id,
            ConversationId = message.ConversationId,
            Content = message.Content,
            SenderType = message.SenderType,
            Timestamp = message.CreatedAt
        });

        return MapToDto(message);
    }

    public async Task<IEnumerable<MessageDto>> GetRecentMessagesAsync(long conversationId, int limit = 10, CancellationToken cancellationToken = default)
    {
        var guidConversationId = new Guid(conversationId.ToString().PadLeft(32, '0'));
        
        var messages = await _context.Messages
            .Include(m => m.Media)
            .Where(m => m.ConversationId == guidConversationId)
            .OrderByDescending(m => m.CreatedAt)
            .Take(limit)
            .ToListAsync(cancellationToken);

        return messages.OrderBy(m => m.CreatedAt).Select(MapToDto);
    }

    private static MessageDto MapToDto(Message message)
    {
        return new MessageDto
        {
            Id = message.Id,
            ConversationId = message.ConversationId,
            Content = message.Content,
            SenderType = message.SenderType,
            SenderId = message.SenderId,
            Status = message.Status,
            DeliveredAt = message.DeliveredAt,
            ReadAt = message.ReadAt,
            ExternalMessageId = message.ExternalMessageId,
            IsAiGenerated = message.IsAiGenerated,
            AiConfidence = message.AiConfidence,
            Sentiment = message.Sentiment,
            Keywords = message.Keywords,
            CreatedAt = message.CreatedAt,
            UpdatedAt = message.UpdatedAt,
            Media = message.Media != null ? new MediaDto
            {
                Id = message.Media.Id,
                MediaType = message.Media.MediaType,
                FileUrl = message.Media.FileUrl,
                FileName = message.Media.FileName,
                FileSize = message.Media.FileSize
            } : null
        };
    }
}