using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class ConversationService : IConversationService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<ConversationService> _logger;

    public ConversationService(RubiaDbContext context, ILogger<ConversationService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<IEnumerable<ConversationDto>> GetConversationsByStatusAsync(Guid companyId, ConversationStatus status)
    {
        var conversations = await _context.Conversations
            .Include(c => c.Participants)
                .ThenInclude(p => p.Customer)
            .Include(c => c.AssignedUser)
            .Where(c => c.CompanyId == companyId && c.Status == status)
            .OrderByDescending(c => c.UpdatedAt)
            .ToListAsync();

        return conversations.Select(MapToDto);
    }

    public async Task<ConversationDto?> GetByIdAsync(Guid conversationId)
    {
        var conversation = await _context.Conversations
            .Include(c => c.Participants)
                .ThenInclude(p => p.Customer)
            .Include(c => c.AssignedUser)
            .Include(c => c.Messages.OrderBy(m => m.CreatedAt))
                .ThenInclude(m => m.Media)
            .FirstOrDefaultAsync(c => c.Id == conversationId);

        return conversation != null ? MapToDto(conversation) : null;
    }

    public async Task<ConversationDto> CreateAsync(CreateConversationDto dto)
    {
        var conversation = new Conversation
        {
            Id = Guid.NewGuid(),
            CompanyId = dto.CompanyId,
            Channel = dto.Channel,
            Status = ConversationStatus.ENTRADA,
            ConversationType = dto.ConversationType,
            ChatLid = dto.ChatLid,
            AiAutoResponseEnabled = dto.AiAutoResponseEnabled ?? true
        };

        _context.Conversations.Add(conversation);

        // Add participants
        if (dto.CustomerIds?.Any() == true)
        {
            foreach (var customerId in dto.CustomerIds)
            {
                var participant = new ConversationParticipant
                {
                    Id = Guid.NewGuid(),
                    ConversationId = conversation.Id,
                    CompanyId = dto.CompanyId,
                    CustomerId = customerId,
                    JoinedAt = DateTime.UtcNow
                };
                _context.ConversationParticipants.Add(participant);
            }
        }

        await _context.SaveChangesAsync();

        return await GetByIdAsync(conversation.Id) ?? throw new InvalidOperationException("Failed to create conversation");
    }

    public async Task<ConversationDto?> UpdateStatusAsync(Guid conversationId, ConversationStatus status)
    {
        var conversation = await _context.Conversations.FindAsync(conversationId);
        if (conversation == null)
            return null;

        conversation.Status = status;
        await _context.SaveChangesAsync();

        return await GetByIdAsync(conversationId);
    }

    public async Task<ConversationDto?> AssignUserAsync(Guid conversationId, Guid? userId)
    {
        var conversation = await _context.Conversations.FindAsync(conversationId);
        if (conversation == null)
            return null;

        conversation.AssignedUserId = userId;
        await _context.SaveChangesAsync();

        return await GetByIdAsync(conversationId);
    }

    private static ConversationDto MapToDto(Conversation conversation)
    {
        return new ConversationDto
        {
            Id = conversation.Id,
            CompanyId = conversation.CompanyId,
            Channel = conversation.Channel,
            Status = conversation.Status,
            Priority = conversation.Priority,
            ConversationType = conversation.ConversationType,
            ChatLid = conversation.ChatLid,
            AiAutoResponseEnabled = conversation.AiAutoResponseEnabled,
            AiMessagesUsed = conversation.AiMessagesUsed,
            AiLimitReachedAt = conversation.AiLimitReachedAt,
            AssignedUserId = conversation.AssignedUserId,
            AssignedUserName = conversation.AssignedUser?.Name,
            CreatedAt = conversation.CreatedAt,
            UpdatedAt = conversation.UpdatedAt,
            Participants = conversation.Participants?.Select(p => new ConversationParticipantDto
            {
                Id = p.Id,
                CustomerName = p.Customer?.Name,
                CustomerPhone = p.Customer?.Phone,
                IsActive = p.IsActive,
                JoinedAt = p.JoinedAt
            }).ToList() ?? new List<ConversationParticipantDto>(),
            Messages = conversation.Messages?.Select(m => new MessageSummaryDto
            {
                Id = m.Id,
                Content = m.Content,
                SenderType = m.SenderType,
                Status = m.Status,
                CreatedAt = m.CreatedAt,
                HasMedia = m.ConversationMediaId.HasValue
            }).ToList() ?? new List<MessageSummaryDto>()
        };
    }
}