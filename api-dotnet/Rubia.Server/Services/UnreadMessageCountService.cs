using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class UnreadMessageCountService : IUnreadMessageCountService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<UnreadMessageCountService> _logger;

    public UnreadMessageCountService(RubiaDbContext context, ILogger<UnreadMessageCountService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<IEnumerable<UnreadMessageCountDto>> GetUserUnreadCountsAsync(Guid userId)
    {
        var counts = await _context.UnreadMessageCounts
            .Include(umc => umc.Conversation)
            .Where(umc => umc.UserId == userId && umc.Count > 0)
            .OrderByDescending(umc => umc.UpdatedAt)
            .ToListAsync();

        return counts.Select(MapToDto);
    }

    public async Task<UnreadMessageCountDto?> GetConversationUnreadCountAsync(Guid userId, Guid conversationId)
    {
        var count = await _context.UnreadMessageCounts
            .Include(umc => umc.Conversation)
            .FirstOrDefaultAsync(umc => umc.UserId == userId && umc.ConversationId == conversationId);

        return count != null ? MapToDto(count) : null;
    }

    public async Task<int> GetTotalUnreadCountAsync(Guid userId)
    {
        return await _context.UnreadMessageCounts
            .Where(umc => umc.UserId == userId)
            .SumAsync(umc => umc.Count);
    }

    public async Task<Dictionary<Guid, int>> GetConversationUnreadCountsAsync(Guid userId, List<Guid> conversationIds)
    {
        var counts = await _context.UnreadMessageCounts
            .Where(umc => umc.UserId == userId && conversationIds.Contains(umc.ConversationId))
            .ToDictionaryAsync(umc => umc.ConversationId, umc => umc.Count);

        // Ensure all requested conversation IDs are in the result
        foreach (var conversationId in conversationIds)
        {
            if (!counts.ContainsKey(conversationId))
            {
                counts[conversationId] = 0;
            }
        }

        return counts;
    }

    public async Task IncrementUnreadCountAsync(Guid userId, Guid conversationId, int increment = 1)
    {
        var existingCount = await _context.UnreadMessageCounts
            .FirstOrDefaultAsync(umc => umc.UserId == userId && umc.ConversationId == conversationId);

        if (existingCount != null)
        {
            existingCount.Count += increment;
            existingCount.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            var newCount = new UnreadMessageCount
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                ConversationId = conversationId,
                Count = increment,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };
            _context.UnreadMessageCounts.Add(newCount);
        }

        await _context.SaveChangesAsync();
    }

    public async Task MarkAsReadAsync(Guid userId, Guid conversationId)
    {
        var count = await _context.UnreadMessageCounts
            .FirstOrDefaultAsync(umc => umc.UserId == userId && umc.ConversationId == conversationId);

        if (count != null)
        {
            count.Count = 0;
            count.LastReadAt = DateTime.UtcNow;
            count.UpdatedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
        }
    }

    public async Task MarkAllAsReadAsync(Guid userId)
    {
        var counts = await _context.UnreadMessageCounts
            .Where(umc => umc.UserId == userId && umc.Count > 0)
            .ToListAsync();

        foreach (var count in counts)
        {
            count.Count = 0;
            count.LastReadAt = DateTime.UtcNow;
            count.UpdatedAt = DateTime.UtcNow;
        }

        if (counts.Any())
        {
            await _context.SaveChangesAsync();
        }
    }

    public async Task RecalculateUnreadCountAsync(Guid userId, Guid conversationId)
    {
        var lastReadAt = await _context.UnreadMessageCounts
            .Where(umc => umc.UserId == userId && umc.ConversationId == conversationId)
            .Select(umc => umc.LastReadAt)
            .FirstOrDefaultAsync();

        var cutoffDate = lastReadAt ?? DateTime.MinValue;

        var actualUnreadCount = await _context.Messages
            .Where(m => m.ConversationId == conversationId 
                       && m.CreatedAt > cutoffDate
                       && m.SenderType != Rubia.Server.Enums.SenderType.USER) // Don't count user's own messages
            .CountAsync();

        var existingCount = await _context.UnreadMessageCounts
            .FirstOrDefaultAsync(umc => umc.UserId == userId && umc.ConversationId == conversationId);

        if (existingCount != null)
        {
            existingCount.Count = actualUnreadCount;
            existingCount.UpdatedAt = DateTime.UtcNow;
        }
        else if (actualUnreadCount > 0)
        {
            var newCount = new UnreadMessageCount
            {
                Id = Guid.NewGuid(),
                UserId = userId,
                ConversationId = conversationId,
                Count = actualUnreadCount,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };
            _context.UnreadMessageCounts.Add(newCount);
        }

        await _context.SaveChangesAsync();
    }

    public async Task<UnreadSummaryDto> GetUnreadSummaryAsync(Guid userId)
    {
        var summary = await _context.UnreadMessageCounts
            .Where(umc => umc.UserId == userId && umc.Count > 0)
            .GroupBy(umc => 1) // Group all records
            .Select(g => new 
            {
                TotalUnread = g.Sum(umc => umc.Count),
                ConversationsWithUnread = g.Count(),
                OldestUnread = g.Min(umc => umc.UpdatedAt)
            })
            .FirstOrDefaultAsync();

        return new UnreadSummaryDto
        {
            TotalUnreadMessages = summary?.TotalUnread ?? 0,
            ConversationsWithUnread = summary?.ConversationsWithUnread ?? 0,
            OldestUnreadMessage = summary?.OldestUnread
        };
    }

    private static UnreadMessageCountDto MapToDto(UnreadMessageCount count)
    {
        return new UnreadMessageCountDto
        {
            UserId = count.UserId,
            ConversationId = count.ConversationId,
            Count = count.Count,
            LastReadAt = count.LastReadAt,
            UpdatedAt = count.UpdatedAt
        };
    }
}