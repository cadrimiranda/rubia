using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/conversation-media")]
[Authorize]
public class ConversationMediaController : ControllerBase
{
    private readonly IConversationMediaService _conversationMediaService;
    private readonly ILogger<ConversationMediaController> _logger;

    public ConversationMediaController(IConversationMediaService conversationMediaService, ILogger<ConversationMediaController> logger)
    {
        _conversationMediaService = conversationMediaService;
        _logger = logger;
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<ConversationMediaDto>> GetMedia(Guid id)
    {
        try
        {
            var media = await _conversationMediaService.GetByIdAsync(id);
            if (media == null)
            {
                return NotFound();
            }

            return Ok(media);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting media {MediaId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("conversation/{conversationId:guid}")]
    public async Task<ActionResult<IEnumerable<ConversationMediaDto>>> GetConversationMedia(Guid conversationId)
    {
        try
        {
            var media = await _conversationMediaService.GetByConversationAsync(conversationId);
            return Ok(media);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting conversation media {ConversationId}", conversationId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("upload")]
    public async Task<ActionResult<ConversationMediaDto>> UploadFile([FromForm] MediaUploadDto dto)
    {
        try
        {
            var media = await _conversationMediaService.UploadFileAsync(dto.File, dto.MediaType);
            return CreatedAtAction(nameof(GetMedia), new { id = media.Id }, media);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error uploading file");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<ConversationMediaDto>> CreateMedia([FromBody] CreateConversationMediaDto dto)
    {
        try
        {
            var media = await _conversationMediaService.CreateAsync(dto);
            return CreatedAtAction(nameof(GetMedia), new { id = media.Id }, media);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating media");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteMedia(Guid id)
    {
        try
        {
            var success = await _conversationMediaService.DeleteAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting media {MediaId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}/download")]
    public async Task<IActionResult> DownloadFile(Guid id)
    {
        try
        {
            var (stream, contentType, fileName) = await _conversationMediaService.GetFileStreamAsync(id);
            
            if (stream == null)
            {
                return NotFound();
            }

            return File(stream, contentType ?? "application/octet-stream", fileName);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error downloading file {MediaId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}/content")]
    public async Task<IActionResult> GetFileContent(Guid id)
    {
        try
        {
            var media = await _conversationMediaService.GetByIdAsync(id);
            if (media == null)
            {
                return NotFound();
            }

            var content = await _conversationMediaService.GetFileContentAsync(id);
            if (content == null)
            {
                return NotFound("File content not found");
            }

            return File(content, media.MimeType ?? "application/octet-stream");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting file content {MediaId}", id);
            return StatusCode(500, "Internal server error");
        }
    }
}