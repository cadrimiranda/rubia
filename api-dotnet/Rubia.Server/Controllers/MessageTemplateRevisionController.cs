using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/message-template-revisions")]
[Authorize]
public class MessageTemplateRevisionController : ControllerBase
{
    private readonly IMessageTemplateRevisionService _messageTemplateRevisionService;
    private readonly ILogger<MessageTemplateRevisionController> _logger;

    public MessageTemplateRevisionController(
        IMessageTemplateRevisionService messageTemplateRevisionService,
        ILogger<MessageTemplateRevisionController> logger)
    {
        _messageTemplateRevisionService = messageTemplateRevisionService;
        _logger = logger;
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> GetById(Guid id)
    {
        try
        {
            var revision = await _messageTemplateRevisionService.GetByIdAsync(id);
            if (revision == null)
            {
                return NotFound();
            }

            return Ok(revision);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting revision {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<MessageTemplateRevisionDto>>> GetAll()
    {
        try
        {
            var revisions = await _messageTemplateRevisionService.GetAllAsync();
            return Ok(revisions);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting all revisions");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<MessageTemplateRevisionDto>> Create([FromBody] CreateMessageTemplateRevisionDto createDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var revision = await _messageTemplateRevisionService.CreateAsync(createDto);
            return CreatedAtAction(nameof(GetById), new { id = revision.Id }, revision);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "Invalid request for creating revision");
            return BadRequest(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating revision");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> Update(Guid id, [FromBody] UpdateMessageTemplateRevisionDto updateDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            var revision = await _messageTemplateRevisionService.UpdateAsync(id, updateDto);
            if (revision == null)
            {
                return NotFound();
            }

            return Ok(revision);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating revision {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        try
        {
            var success = await _messageTemplateRevisionService.DeleteAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting revision {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}")]
    public async Task<ActionResult<IEnumerable<MessageTemplateRevisionDto>>> GetByTemplateId(Guid templateId)
    {
        try
        {
            _logger.LogDebug("Finding MessageTemplateRevisions by template id via API: {TemplateId}", templateId);

            var revisions = await _messageTemplateRevisionService.GetByTemplateIdAsync(templateId);
            return Ok(revisions);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting revisions for template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/count")]
    public async Task<ActionResult<long>> CountByTemplateId(Guid templateId)
    {
        try
        {
            _logger.LogDebug("Counting MessageTemplateRevisions by template id via API: {TemplateId}", templateId);

            var count = await _messageTemplateRevisionService.CountByTemplateIdAsync(templateId);
            return Ok(count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error counting revisions for template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("edited-by-user/{userId:guid}")]
    public async Task<ActionResult<IEnumerable<MessageTemplateRevisionDto>>> GetByEditedByUserId(Guid userId)
    {
        try
        {
            _logger.LogDebug("Finding MessageTemplateRevisions by edited by user id via API: {UserId}", userId);

            var revisions = await _messageTemplateRevisionService.GetByEditedByUserIdAsync(userId);
            return Ok(revisions);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting revisions edited by user {UserId}", userId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/revision/{revisionNumber:int}")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> GetByTemplateIdAndRevisionNumber(Guid templateId, int revisionNumber)
    {
        try
        {
            _logger.LogDebug("Finding MessageTemplateRevision by template id: {TemplateId} and revision number: {RevisionNumber}", templateId, revisionNumber);

            var revision = await _messageTemplateRevisionService.GetByTemplateIdAndRevisionNumberAsync(templateId, revisionNumber);
            if (revision == null)
            {
                return NotFound();
            }

            return Ok(revision);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting revision {RevisionNumber} for template {TemplateId}", revisionNumber, templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/ordered")]
    public async Task<ActionResult<IEnumerable<MessageTemplateRevisionDto>>> GetByTemplateIdOrderByRevisionNumberDesc(Guid templateId)
    {
        try
        {
            _logger.LogDebug("Finding MessageTemplateRevisions by template id ordered by revision number desc via API: {TemplateId}", templateId);

            var revisions = await _messageTemplateRevisionService.GetByTemplateIdOrderByRevisionNumberDescAsync(templateId);
            return Ok(revisions);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting ordered revisions for template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/latest")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> GetLatestRevision(Guid templateId)
    {
        try
        {
            _logger.LogDebug("Getting latest revision for template id via API: {TemplateId}", templateId);

            var revision = await _messageTemplateRevisionService.GetLatestRevisionAsync(templateId);
            if (revision == null)
            {
                return NotFound();
            }

            return Ok(revision);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting latest revision for template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/original")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> GetOriginalRevision(Guid templateId)
    {
        try
        {
            _logger.LogDebug("Getting original revision for template id via API: {TemplateId}", templateId);

            var revision = await _messageTemplateRevisionService.GetOriginalRevisionAsync(templateId);
            if (revision == null)
            {
                return NotFound();
            }

            return Ok(revision);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting original revision for template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/revision/{revisionNumber:int}/exists")]
    public async Task<ActionResult<bool>> ExistsByTemplateIdAndRevisionNumber(Guid templateId, int revisionNumber)
    {
        try
        {
            _logger.LogDebug("Checking if revision exists for template id: {TemplateId} and revision number: {RevisionNumber}", templateId, revisionNumber);

            var exists = await _messageTemplateRevisionService.ExistsByTemplateIdAndRevisionNumberAsync(templateId, revisionNumber);
            return Ok(exists);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking if revision exists for template {TemplateId} revision {RevisionNumber}", templateId, revisionNumber);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/revisions/{minRevision:int}/{maxRevision:int}")]
    public async Task<ActionResult<IEnumerable<MessageTemplateRevisionDto>>> GetRevisionsBetweenNumbers(
        Guid templateId, int minRevision, int maxRevision)
    {
        try
        {
            _logger.LogDebug("Finding revisions between {MinRevision} and {MaxRevision} for template id: {TemplateId}", 
                minRevision, maxRevision, templateId);

            var revisions = await _messageTemplateRevisionService.GetRevisionsBetweenNumbersAsync(templateId, minRevision, maxRevision);
            return Ok(revisions);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting revisions between {MinRevision} and {MaxRevision} for template {TemplateId}", 
                minRevision, maxRevision, templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("template/{templateId:guid}/next-revision-number")]
    public async Task<ActionResult<int>> GetNextRevisionNumber(Guid templateId)
    {
        try
        {
            _logger.LogDebug("Getting next revision number for template id via API: {TemplateId}", templateId);

            var nextNumber = await _messageTemplateRevisionService.GetNextRevisionNumberAsync(templateId);
            return Ok(nextNumber);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting next revision number for template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("template/{templateId:guid}/create-revision")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> CreateRevisionFromTemplate(
        Guid templateId,
        [FromQuery] string content,
        [FromQuery] Guid editedByUserId)
    {
        try
        {
            _logger.LogDebug("Creating revision from template id: {TemplateId} with content by user: {EditedByUserId}", templateId, editedByUserId);

            var revision = await _messageTemplateRevisionService.CreateRevisionFromTemplateAsync(templateId, content, editedByUserId);
            return Ok(revision);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "Invalid request for creating revision from template");
            return BadRequest(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating revision from template {TemplateId}", templateId);
            return StatusCode(500, "Internal server error");
        }
    }
}