using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/message-drafts")]
[Authorize]
public class MessageDraftsController : ControllerBase
{
    private readonly IMessageDraftService _messageDraftService;
    private readonly ILogger<MessageDraftsController> _logger;

    public MessageDraftsController(IMessageDraftService messageDraftService, ILogger<MessageDraftsController> logger)
    {
        _messageDraftService = messageDraftService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<MessageDraftDto>>> GetUserDrafts([FromQuery] Guid? conversationId = null)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var drafts = await _messageDraftService.GetUserDraftsAsync(userId.Value, conversationId);
            return Ok(drafts);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting user drafts");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<MessageDraftDto>> GetDraft(Guid id)
    {
        try
        {
            var draft = await _messageDraftService.GetByIdAsync(id);
            if (draft == null)
            {
                return NotFound();
            }

            return Ok(draft);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting draft {DraftId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<MessageDraftDto>> CreateOrUpdateDraft([FromBody] CreateMessageDraftDto dto)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            dto.UserId = userId.Value;
            var draft = await _messageDraftService.CreateOrUpdateAsync(dto);
            
            return Ok(draft);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating/updating draft");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<MessageDraftDto>> UpdateDraft(Guid id, [FromBody] UpdateMessageDraftDto dto)
    {
        try
        {
            var draft = await _messageDraftService.UpdateAsync(id, dto);
            if (draft == null)
            {
                return NotFound();
            }

            return Ok(draft);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating draft {DraftId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteDraft(Guid id)
    {
        try
        {
            var success = await _messageDraftService.DeleteAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting draft {DraftId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("conversation/{conversationId:guid}")]
    public async Task<IActionResult> DeleteConversationDrafts(Guid conversationId)
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var deletedCount = await _messageDraftService.DeleteUserDraftsAsync(userId.Value, conversationId);
            return Ok(new { message = $"Deleted {deletedCount} drafts" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting conversation drafts {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("templates")]
    public async Task<ActionResult<IEnumerable<MessageDraftDto>>> GetTemplates()
    {
        try
        {
            var userId = GetUserId();
            if (!userId.HasValue)
            {
                return BadRequest("User context not found");
            }

            var templates = await _messageDraftService.GetTemplatesAsync(userId.Value);
            return Ok(templates);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting draft templates");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id:guid}/save-as-template")]
    public async Task<ActionResult<MessageDraftDto>> SaveAsTemplate(Guid id, [FromBody] SaveAsTemplateDto dto)
    {
        try
        {
            var template = await _messageDraftService.SaveAsTemplateAsync(id, dto.TemplateName);
            if (template == null)
            {
                return NotFound();
            }

            return Ok(template);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error saving draft as template {DraftId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return userIdClaim != null && Guid.TryParse(userIdClaim, out var userId) ? userId : null;
    }
}