using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class ConversationParticipantService : IConversationParticipantService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<ConversationParticipantService> _logger;

    public ConversationParticipantService(
        RubiaDbContext context,
        ILogger<ConversationParticipantService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<ConversationParticipantDto> AddParticipantAsync(CreateConversationParticipantDto createDto, CancellationToken cancellationToken = default)
    {
        // Validate conversation exists
        var conversation = await _context.Conversations.FindAsync(createDto.ConversationId, cancellationToken);
        if (conversation == null)
            throw new ArgumentException($"Conversation {createDto.ConversationId} not found");

        // Validate user exists and belongs to same company
        var user = await _context.Users.FindAsync(createDto.UserId, cancellationToken);
        if (user == null)
            throw new ArgumentException($"User {createDto.UserId} not found");

        if (user.CompanyId != conversation.CompanyId)
            throw new InvalidOperationException("User must belong to the same company as the conversation");

        // Check if participant already exists
        var existingParticipant = await _context.ConversationParticipants
            .FirstOrDefaultAsync(cp => cp.ConversationId == createDto.ConversationId && cp.UserId == createDto.UserId, cancellationToken);

        if (existingParticipant != null)
        {
            if (existingParticipant.IsActive)
                throw new InvalidOperationException("User is already a participant in this conversation");
            
            // Reactivate existing participant
            existingParticipant.IsActive = true;
            existingParticipant.JoinedAt = DateTime.UtcNow;
            existingParticipant.LeftAt = null;
            existingParticipant.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync(cancellationToken);
            return await MapToDtoAsync(existingParticipant, cancellationToken);
        }

        var participant = new ConversationParticipant
        {
            Id = Guid.NewGuid(),
            ConversationId = createDto.ConversationId,
            UserId = createDto.UserId,
            Role = createDto.Role ?? ConversationParticipantRole.Member.ToString(),
            IsActive = true,
            CanRead = true,
            CanWrite = true,
            CanAssign = createDto.Role == ConversationParticipantRole.Owner.ToString() || createDto.Role == ConversationParticipantRole.Admin.ToString(),
            JoinedAt = DateTime.UtcNow,
            CreatedAt = DateTime.UtcNow
        };

        _context.ConversationParticipants.Add(participant);
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("User {UserId} added as participant to conversation {ConversationId}", 
            createDto.UserId, createDto.ConversationId);

        return await MapToDtoAsync(participant, cancellationToken);
    }

    public async Task<bool> RemoveParticipantAsync(Guid participantId, CancellationToken cancellationToken = default)
    {
        var participant = await _context.ConversationParticipants.FindAsync(participantId, cancellationToken);
        if (participant == null)
            return false;

        participant.IsActive = false;
        participant.LeftAt = DateTime.UtcNow;
        participant.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Participant {ParticipantId} removed from conversation {ConversationId}", 
            participantId, participant.ConversationId);

        return true;
    }

    public async Task<IEnumerable<ConversationParticipantDto>> GetConversationParticipantsAsync(Guid conversationId, CancellationToken cancellationToken = default)
    {
        var participants = await _context.ConversationParticipants
            .Include(cp => cp.User)
            .Include(cp => cp.Conversation)
            .Where(cp => cp.ConversationId == conversationId && cp.IsActive)
            .OrderBy(cp => cp.JoinedAt)
            .ToListAsync(cancellationToken);

        var tasks = participants.Select(p => MapToDtoAsync(p, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<IEnumerable<ConversationDto>> GetUserConversationsAsync(Guid userId, CancellationToken cancellationToken = default)
    {
        var conversations = await _context.ConversationParticipants
            .Include(cp => cp.Conversation)
                .ThenInclude(c => c!.Customer)
            .Include(cp => cp.Conversation)
                .ThenInclude(c => c!.Campaign)
            .Where(cp => cp.UserId == userId && cp.IsActive)
            .Select(cp => cp.Conversation!)
            .Distinct()
            .OrderByDescending(c => c.UpdatedAt ?? c.CreatedAt)
            .ToListAsync(cancellationToken);

        return conversations.Select(MapConversationToDto);
    }

    public async Task<bool> AssignConversationToUserAsync(Guid conversationId, Guid userId, Guid assignedByUserId, CancellationToken cancellationToken = default)
    {
        var conversation = await _context.Conversations.FindAsync(conversationId, cancellationToken);
        if (conversation == null)
            return false;

        // Add user as participant if not already
        var existingParticipant = await _context.ConversationParticipants
            .FirstOrDefaultAsync(cp => cp.ConversationId == conversationId && cp.UserId == userId && cp.IsActive, cancellationToken);

        if (existingParticipant == null)
        {
            await AddParticipantAsync(new CreateConversationParticipantDto
            {
                ConversationId = conversationId,
                UserId = userId,
                Role = ConversationParticipantRole.Member.ToString()
            }, cancellationToken);
        }

        // Update conversation assignment
        conversation.AssignedUserId = userId;
        conversation.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Conversation {ConversationId} assigned to user {UserId} by user {AssignedByUserId}", 
            conversationId, userId, assignedByUserId);

        return true;
    }

    public async Task<bool> UnassignConversationFromUserAsync(Guid conversationId, Guid userId, CancellationToken cancellationToken = default)
    {
        var conversation = await _context.Conversations.FindAsync(conversationId, cancellationToken);
        if (conversation == null || conversation.AssignedUserId != userId)
            return false;

        conversation.AssignedUserId = null;
        conversation.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Conversation {ConversationId} unassigned from user {UserId}", conversationId, userId);

        return true;
    }

    public async Task<bool> TransferConversationAsync(Guid conversationId, Guid fromUserId, Guid toUserId, Guid transferredByUserId, string? reason = null, CancellationToken cancellationToken = default)
    {
        var conversation = await _context.Conversations.FindAsync(conversationId, cancellationToken);
        if (conversation == null)
            return false;

        // Ensure target user is a participant
        await AddParticipantAsync(new CreateConversationParticipantDto
        {
            ConversationId = conversationId,
            UserId = toUserId,
            Role = ConversationParticipantRole.Member.ToString()
        }, cancellationToken);

        // Update assignment
        conversation.AssignedUserId = toUserId;
        conversation.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync(cancellationToken);

        // Log transfer
        _logger.LogInformation("Conversation {ConversationId} transferred from user {FromUserId} to user {ToUserId} by user {TransferredByUserId}. Reason: {Reason}", 
            conversationId, fromUserId, toUserId, transferredByUserId, reason ?? "Not specified");

        return true;
    }

    public async Task<ConversationParticipantDto?> GetActiveParticipantAsync(Guid conversationId, CancellationToken cancellationToken = default)
    {
        var conversation = await _context.Conversations
            .Include(c => c.AssignedUser)
            .FirstOrDefaultAsync(c => c.Id == conversationId, cancellationToken);

        if (conversation?.AssignedUserId == null)
            return null;

        var participant = await _context.ConversationParticipants
            .Include(cp => cp.User)
            .FirstOrDefaultAsync(cp => cp.ConversationId == conversationId && cp.UserId == conversation.AssignedUserId && cp.IsActive, cancellationToken);

        return participant != null ? await MapToDtoAsync(participant, cancellationToken) : null;
    }

    public async Task<IEnumerable<ConversationParticipantDto>> GetParticipantHistoryAsync(Guid conversationId, CancellationToken cancellationToken = default)
    {
        var participants = await _context.ConversationParticipants
            .Include(cp => cp.User)
            .Where(cp => cp.ConversationId == conversationId)
            .OrderByDescending(cp => cp.JoinedAt)
            .ToListAsync(cancellationToken);

        var tasks = participants.Select(p => MapToDtoAsync(p, cancellationToken));
        return await Task.WhenAll(tasks);
    }

    public async Task<bool> SetParticipantPermissionAsync(Guid participantId, string permission, bool allowed, CancellationToken cancellationToken = default)
    {
        var participant = await _context.ConversationParticipants.FindAsync(participantId, cancellationToken);
        if (participant == null)
            return false;

        switch (permission.ToLower())
        {
            case "read":
                participant.CanRead = allowed;
                break;
            case "write":
                participant.CanWrite = allowed;
                break;
            case "assign":
                participant.CanAssign = allowed;
                break;
            default:
                return false;
        }

        participant.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Participant {ParticipantId} permission {Permission} set to {Allowed}", 
            participantId, permission, allowed);

        return true;
    }

    public async Task<ConversationParticipantStatsDto> GetParticipantStatsAsync(Guid userId, DateTime? fromDate = null, DateTime? toDate = null, CancellationToken cancellationToken = default)
    {
        fromDate ??= DateTime.UtcNow.AddMonths(-1);
        toDate ??= DateTime.UtcNow;

        var participations = await _context.ConversationParticipants
            .Include(cp => cp.Conversation)
            .Where(cp => cp.UserId == userId 
                        && cp.JoinedAt >= fromDate 
                        && cp.JoinedAt <= toDate)
            .ToListAsync(cancellationToken);

        var messageCount = await _context.Messages
            .Where(m => m.SenderType == SenderType.User.ToString() 
                       && m.SenderId == userId
                       && m.CreatedAt >= fromDate 
                       && m.CreatedAt <= toDate)
            .CountAsync(cancellationToken);

        var activeConversations = participations.Count(p => p.IsActive);
        var totalConversations = participations.Count;
        var avgResponseTime = participations
            .Where(p => p.LeftAt.HasValue)
            .Select(p => (p.LeftAt!.Value - p.JoinedAt).TotalMinutes)
            .DefaultIfEmpty(0)
            .Average();

        return new ConversationParticipantStatsDto
        {
            UserId = userId,
            PeriodStart = fromDate.Value,
            PeriodEnd = toDate.Value,
            ActiveConversations = activeConversations,
            TotalConversations = totalConversations,
            MessagesSent = messageCount,
            AverageResponseTimeMinutes = avgResponseTime,
            ConversationsResolved = participations.Count(p => p.LeftAt.HasValue),
            ConversationsTransferred = 0 // Would need additional tracking
        };
    }

    private async Task<ConversationParticipantDto> MapToDtoAsync(ConversationParticipant participant, CancellationToken cancellationToken)
    {
        if (participant.User == null)
        {
            await _context.Entry(participant)
                .Reference(p => p.User)
                .LoadAsync(cancellationToken);
        }

        if (participant.Conversation == null)
        {
            await _context.Entry(participant)
                .Reference(p => p.Conversation)
                .LoadAsync(cancellationToken);
        }

        return new ConversationParticipantDto
        {
            Id = participant.Id,
            ConversationId = participant.ConversationId,
            UserId = participant.UserId,
            Role = Enum.Parse<ConversationParticipantRole>(participant.Role),
            IsActive = participant.IsActive,
            CanRead = participant.CanRead,
            CanWrite = participant.CanWrite,
            CanAssign = participant.CanAssign,
            JoinedAt = participant.JoinedAt,
            LeftAt = participant.LeftAt,
            UserName = participant.User?.Name ?? "Unknown",
            UserEmail = participant.User?.Email ?? "Unknown",
            CreatedAt = participant.CreatedAt,
            UpdatedAt = participant.UpdatedAt
        };
    }

    private static ConversationDto MapConversationToDto(Conversation conversation)
    {
        return new ConversationDto
        {
            Id = conversation.Id,
            CompanyId = conversation.CompanyId,
            CustomerId = conversation.CustomerId,
            AssignedUserId = conversation.AssignedUserId,
            OwnerUserId = conversation.OwnerUserId,
            ExternalId = conversation.ExternalId,
            ChatLid = conversation.ChatLid,
            Channel = Enum.Parse<ConversationChannel>(conversation.Channel),
            ConversationType = Enum.Parse<ConversationType>(conversation.ConversationType),
            Status = Enum.Parse<ConversationStatus>(conversation.Status),
            CustomerName = conversation.Customer?.Name ?? "Unknown",
            CampaignName = conversation.Campaign?.Name,
            CreatedAt = conversation.CreatedAt,
            UpdatedAt = conversation.UpdatedAt
        };
    }
}