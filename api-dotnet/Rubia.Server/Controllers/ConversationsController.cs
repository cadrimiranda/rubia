using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/conversations")]
[Authorize]
public class ConversationsController : ControllerBase
{
    private readonly IConversationService _conversationService;
    private readonly ILogger<ConversationsController> _logger;

    public ConversationsController(IConversationService conversationService, ILogger<ConversationsController> logger)
    {
        _conversationService = conversationService;
        _logger = logger;
    }

    [HttpGet("by-status/{status}")]
    public async Task<ActionResult<IEnumerable<ConversationDto>>> GetConversationsByStatus(
        ConversationStatus status)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var conversations = await _conversationService.GetConversationsByStatusAsync(companyId.Value, status);
            return Ok(conversations);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting conversations by status {Status}", status);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ConversationDto>> GetConversation(Guid id)
    {
        try
        {
            var conversation = await _conversationService.GetByIdAsync(id);
            if (conversation == null)
            {
                return NotFound();
            }

            return Ok(conversation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting conversation {ConversationId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<ConversationDto>> CreateConversation([FromBody] CreateConversationDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            dto.CompanyId = companyId.Value;
            var conversation = await _conversationService.CreateAsync(dto);
            
            return CreatedAtAction(nameof(GetConversation), new { id = conversation.Id }, conversation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating conversation");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPatch("{id:guid}/status")]
    public async Task<ActionResult<ConversationDto>> UpdateConversationStatus(
        Guid id, 
        [FromBody] UpdateConversationStatusDto dto)
    {
        try
        {
            var conversation = await _conversationService.UpdateStatusAsync(id, dto.Status);
            if (conversation == null)
            {
                return NotFound();
            }

            return Ok(conversation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating conversation status {ConversationId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPatch("{id:guid}/assign")]
    public async Task<ActionResult<ConversationDto>> AssignUser(
        Guid id, 
        [FromBody] AssignUserDto dto)
    {
        try
        {
            var conversation = await _conversationService.AssignUserAsync(id, dto.UserId);
            if (conversation == null)
            {
                return NotFound();
            }

            return Ok(conversation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error assigning user to conversation {ConversationId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = User.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }
}