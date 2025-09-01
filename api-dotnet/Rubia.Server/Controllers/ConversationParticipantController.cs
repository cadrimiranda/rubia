using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ConversationParticipantController : ControllerBase
{
    private readonly IConversationParticipantService _participantService;
    private readonly ILogger<ConversationParticipantController> _logger;

    public ConversationParticipantController(
        IConversationParticipantService participantService,
        ILogger<ConversationParticipantController> logger)
    {
        _participantService = participantService;
        _logger = logger;
    }

    [HttpGet("conversation/{conversationId}")]
    public async Task<ActionResult<IEnumerable<ConversationParticipantDto>>> GetParticipants(int conversationId)
    {
        try
        {
            var participants = await _participantService.GetParticipantsAsync(conversationId);
            return Ok(participants);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving participants for conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("conversation/{conversationId}/add")]
    public async Task<ActionResult<ConversationParticipantDto>> AddParticipant(
        int conversationId, 
        [FromBody] AddParticipantDto addParticipantDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var participant = await _participantService.AddParticipantAsync(conversationId, addParticipantDto);
            return Ok(participant);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error adding participant to conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{participantId}/role")]
    public async Task<ActionResult<ConversationParticipantDto>> UpdateRole(
        int participantId, 
        [FromBody] UpdateParticipantRoleDto updateRoleDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var participant = await _participantService.UpdateParticipantRoleAsync(participantId, updateRoleDto.Role);
            if (participant == null)
            {
                return NotFound();
            }

            return Ok(participant);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating participant role {ParticipantId}", participantId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{participantId}")]
    public async Task<IActionResult> RemoveParticipant(int participantId)
    {
        try
        {
            var success = await _participantService.RemoveParticipantAsync(participantId);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error removing participant {ParticipantId}", participantId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("conversation/{conversationId}/transfer")]
    public async Task<IActionResult> TransferConversation(
        int conversationId, 
        [FromBody] TransferConversationDto transferDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var success = await _participantService.TransferConversationAsync(
                conversationId, 
                transferDto.FromUserId, 
                transferDto.ToUserId, 
                transferDto.Reason);

            if (!success)
            {
                return BadRequest("Transfer failed");
            }

            return Ok();
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error transferring conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("user/{userId}/conversations")]
    public async Task<ActionResult<IEnumerable<ConversationSummaryDto>>> GetUserConversations(int userId)
    {
        try
        {
            var conversations = await _participantService.GetUserConversationsAsync(userId);
            return Ok(conversations);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving conversations for user {UserId}", userId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("conversation/{conversationId}/can-assign")]
    public async Task<ActionResult<IEnumerable<UserAvailabilityDto>>> GetAvailableUsers(int conversationId)
    {
        try
        {
            var availableUsers = await _participantService.GetAvailableUsersForAssignmentAsync(conversationId);
            return Ok(availableUsers);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving available users for conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("conversation/{conversationId}/assign")]
    public async Task<IActionResult> AssignConversation(
        int conversationId, 
        [FromBody] AssignConversationDto assignDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var success = await _participantService.AssignConversationAsync(conversationId, assignDto.UserId);
            if (!success)
            {
                return BadRequest("Assignment failed");
            }

            return Ok();
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error assigning conversation {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }
}