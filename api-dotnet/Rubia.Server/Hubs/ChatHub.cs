using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.SignalR;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;
using System.Collections.Concurrent;

namespace Rubia.Server.Hubs;

[Authorize]
public class ChatHub : Hub
{
    private readonly ILogger<ChatHub> _logger;
    private readonly IWebSocketNotificationService _notificationService;
    
    private static readonly ConcurrentDictionary<string, UserSessionInfo> _userSessions = new();

    public ChatHub(ILogger<ChatHub> logger, IWebSocketNotificationService notificationService)
    {
        _logger = logger;
        _notificationService = notificationService;
    }

    public override async Task OnConnectedAsync()
    {
        var userId = GetUserId();
        var companyId = GetCompanyId();
        var username = GetUsername();

        if (userId.HasValue && companyId.HasValue)
        {
            var sessionInfo = new UserSessionInfo
            {
                ConnectionId = Context.ConnectionId,
                UserId = userId.Value,
                CompanyId = companyId.Value,
                Username = username ?? "Unknown"
            };

            _userSessions[Context.ConnectionId] = sessionInfo;

            // Join company group for notifications
            await Groups.AddToGroupAsync(Context.ConnectionId, $"company_{companyId}");
            
            _logger.LogInformation("User connected: {Username} (connection: {ConnectionId}, company: {CompanyId})", 
                username, Context.ConnectionId, companyId);
        }

        await base.OnConnectedAsync();
    }

    public override async Task OnDisconnectedAsync(Exception? exception)
    {
        if (_userSessions.TryRemove(Context.ConnectionId, out var sessionInfo))
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"company_{sessionInfo.CompanyId}");
            
            _logger.LogInformation("User disconnected: {Username} (connection: {ConnectionId})", 
                sessionInfo.Username, Context.ConnectionId);
        }

        await base.OnDisconnectedAsync(exception);
    }

    public async Task JoinConversation(string conversationId)
    {
        if (Guid.TryParse(conversationId, out var convId))
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, $"conversation_{convId}");
            _logger.LogDebug("User joined conversation {ConversationId}", convId);
        }
    }

    public async Task LeaveConversation(string conversationId)
    {
        if (Guid.TryParse(conversationId, out var convId))
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, $"conversation_{convId}");
            _logger.LogDebug("User left conversation {ConversationId}", convId);
        }
    }

    public async Task SendTypingIndicator(string conversationId, bool isTyping)
    {
        if (Guid.TryParse(conversationId, out var convId))
        {
            var username = GetUsername();
            await Clients.OthersInGroup($"conversation_{convId}")
                .SendAsync("TypingIndicator", conversationId, username, isTyping);
        }
    }

    private Guid? GetUserId()
    {
        var userIdClaim = Context.User?.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return userIdClaim != null && Guid.TryParse(userIdClaim, out var userId) ? userId : null;
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = Context.User?.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }

    private string? GetUsername()
    {
        return Context.User?.FindFirst(ClaimTypes.Name)?.Value;
    }

    public static IReadOnlyDictionary<string, UserSessionInfo> GetUserSessions()
    {
        return _userSessions;
    }
}

public class UserSessionInfo
{
    public string ConnectionId { get; set; } = string.Empty;
    public Guid UserId { get; set; }
    public Guid CompanyId { get; set; }
    public string Username { get; set; } = string.Empty;
}