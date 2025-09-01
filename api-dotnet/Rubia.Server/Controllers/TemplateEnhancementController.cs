using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/template-enhancement")]
[Authorize]
public class TemplateEnhancementController : ControllerBase
{
    private readonly ITemplateEnhancementService _templateEnhancementService;
    private readonly ILogger<TemplateEnhancementController> _logger;

    public TemplateEnhancementController(
        ITemplateEnhancementService templateEnhancementService,
        ILogger<TemplateEnhancementController> logger)
    {
        _templateEnhancementService = templateEnhancementService;
        _logger = logger;
    }

    [HttpPost("enhance")]
    public async Task<ActionResult<EnhancedTemplateResponseDto>> EnhanceTemplate([FromBody] EnhanceTemplateDto request)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            _logger.LogInformation("POST /api/template-enhancement/enhance - Enhancing template for company: {CompanyId}", request.CompanyId);

            var response = await _templateEnhancementService.EnhanceTemplateAsync(request);
            return Ok(response);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "Invalid request for template enhancement");
            return BadRequest(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error enhancing template: {Error}", ex.Message);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("save-with-ai-metadata")]
    public async Task<ActionResult<MessageTemplateRevisionDto>> SaveTemplateWithAIMetadata([FromBody] SaveTemplateWithAiMetadataDto request)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            _logger.LogInformation("POST /api/template-enhancement/save-with-ai-metadata - Saving template with AI metadata for template: {TemplateId}", request.TemplateId);

            var response = await _templateEnhancementService.SaveTemplateWithAIMetadataAsync(request);
            return Ok(response);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning(ex, "Invalid request for saving template with AI metadata");
            return BadRequest(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error saving template with AI metadata: {Error}", ex.Message);
            return StatusCode(500, "Internal server error");
        }
    }
}