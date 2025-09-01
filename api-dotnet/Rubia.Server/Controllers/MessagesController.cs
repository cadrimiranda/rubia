using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/messages")]
[Authorize]
public class MessagesController : ControllerBase
{
    private readonly IMessageService _messageService;
    private readonly IWebSocketNotificationService _notificationService;
    private readonly ILogger<MessagesController> _logger;

    public MessagesController(IMessageService messageService, IWebSocketNotificationService notificationService, ILogger<MessagesController> logger)
    {
        _messageService = messageService;
        _notificationService = notificationService;
        _logger = logger;
    }

    [HttpGet("conversation/{conversationId:guid}")]
    public async Task<ActionResult<IEnumerable<MessageDto>>> GetMessagesByConversation(
        Guid conversationId, 
        [FromQuery] int limit = 50)
    {
        try
        {
            var messages = await _messageService.GetMessagesByConversationAsync(conversationId, limit);
            return Ok(messages);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting messages for conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<MessageDto>> GetMessage(Guid id)
    {
        try
        {
            var message = await _messageService.GetByIdAsync(id);
            if (message == null)
            {
                return NotFound();
            }

            return Ok(message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting message {MessageId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<MessageDto>> CreateMessage([FromBody] CreateMessageDto dto)
    {
        try
        {
            var message = await _messageService.CreateAsync(dto);
            return CreatedAtAction(nameof(GetMessage), new { id = message.Id }, message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating message");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPatch("{id:guid}/status")]
    public async Task<ActionResult<MessageDto>> UpdateMessageStatus(
        Guid id, 
        [FromBody] UpdateMessageStatusDto dto)
    {
        try
        {
            var message = await _messageService.UpdateStatusAsync(id, dto.Status);
            if (message == null)
            {
                return NotFound();
            }

            return Ok(message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating message status {MessageId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("unread-count/{conversationId:guid}")]
    public async Task<ActionResult<int>> GetUnreadCount(Guid conversationId)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var count = await _messageService.GetUnreadCountAsync(conversationId, userId.Value);
            return Ok(count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting unread count for conversation {ConversationId}", conversationId);
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

            await _messageService.MarkAsReadAsync(dto.ConversationId, userId.Value);
            return Ok();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error marking messages as read for conversation {ConversationId}", dto.ConversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return userIdClaim != null && Guid.TryParse(userIdClaim, out var userId) ? userId : null;
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = User.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }
}