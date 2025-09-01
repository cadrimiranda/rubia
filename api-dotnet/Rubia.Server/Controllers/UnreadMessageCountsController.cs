using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/unread-counts")]
[Authorize]
public class UnreadMessageCountsController : ControllerBase
{
    private readonly IUnreadMessageCountService _unreadCountService;
    private readonly ILogger<UnreadMessageCountsController> _logger;

    public UnreadMessageCountsController(IUnreadMessageCountService unreadCountService, ILogger<UnreadMessageCountsController> logger)
    {
        _unreadCountService = unreadCountService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<UnreadMessageCountDto>>> GetUserUnreadCounts()
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var counts = await _unreadCountService.GetUserUnreadCountsAsync(userId.Value);
            return Ok(counts);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting user unread counts");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("conversation/{conversationId:guid}")]
    public async Task<ActionResult<UnreadMessageCountDto>> GetConversationUnreadCount(Guid conversationId)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var count = await _unreadCountService.GetConversationUnreadCountAsync(userId.Value, conversationId);
            if (count == null)
            {
                return Ok(new UnreadMessageCountDto 
                { 
                    UserId = userId.Value, 
                    ConversationId = conversationId, 
                    Count = 0 
                });
            }

            return Ok(count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting conversation unread count {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("total")]
    public async Task<ActionResult<int>> GetTotalUnreadCount()
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var totalCount = await _unreadCountService.GetTotalUnreadCountAsync(userId.Value);
            return Ok(totalCount);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting total unread count");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("bulk")]
    public async Task<ActionResult<Dictionary<Guid, int>>> GetBulkUnreadCounts([FromBody] BulkUnreadCountDto dto)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var counts = await _unreadCountService.GetConversationUnreadCountsAsync(userId.Value, dto.ConversationIds);
            return Ok(counts);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting bulk unread counts");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("mark-as-read")]
    public async Task<IActionResult> MarkAsRead([FromBody] MarkAsReadDto dto)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            await _unreadCountService.MarkAsReadAsync(userId.Value, dto.ConversationId);
            return Ok(new { message = "Marked as read" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error marking conversation as read {ConversationId}", dto.ConversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("mark-all-as-read")]
    public async Task<IActionResult> MarkAllAsRead()
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            await _unreadCountService.MarkAllAsReadAsync(userId.Value);
            return Ok(new { message = "All conversations marked as read" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error marking all conversations as read");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("recalculate/{conversationId:guid}")]
    public async Task<IActionResult> RecalculateUnreadCount(Guid conversationId)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            await _unreadCountService.RecalculateUnreadCountAsync(userId.Value, conversationId);
            return Ok(new { message = "Unread count recalculated" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error recalculating unread count for conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("summary")]
    public async Task<ActionResult<UnreadSummaryDto>> GetUnreadSummary()
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var summary = await _unreadCountService.GetUnreadSummaryAsync(userId.Value);
            return Ok(summary);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting unread summary");
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return userIdClaim != null && Guid.TryParse(userIdClaim, out var userId) ? userId : null;
    }
}