using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/message-templates")]
public class MessageTemplatesController : ControllerBase
{
    private readonly IMessageTemplateService _messageTemplateService;
    private readonly ILogger<MessageTemplatesController> _logger;

    public MessageTemplatesController(IMessageTemplateService messageTemplateService, ILogger<MessageTemplatesController> logger)
    {
        _messageTemplateService = messageTemplateService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<MessageTemplateDto>> Create([FromBody] CreateMessageTemplateDto createDto, [FromQuery] Guid? currentUserId = null)
    {
        _logger.LogInformation("Creating message template: {Name} for company: {CompanyId}", createDto.Name, createDto.CompanyId);

        try
        {
            var created = await _messageTemplateService.CreateAsync(createDto, currentUserId);
            return CreatedAtAction(nameof(GetById), new { id = created.Id, companyId = createDto.CompanyId }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating message template: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<MessageTemplateDto>> GetById(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogDebug("Fetching message template with id: {Id} for company: {CompanyId}", id, companyId);

        try
        {
            var template = await _messageTemplateService.GetByIdAsync(id, companyId);
            return Ok(template);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Message template not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetAll(
        [FromQuery] Guid companyId,
        [FromQuery] bool activeOnly = true,
        [FromQuery] bool? aiGenerated = null,
        [FromQuery] string? tone = null,
        [FromQuery] Guid? createdByUserId = null,
        [FromQuery] Guid? aiAgentId = null)
    {
        _logger.LogDebug("Fetching message templates for company: {CompanyId} with filters", companyId);

        List<MessageTemplateDto> templates;

        if (createdByUserId.HasValue)
        {
            templates = await _messageTemplateService.GetByCreatedByUserAsync(createdByUserId.Value, companyId);
        }
        else if (aiAgentId.HasValue)
        {
            templates = await _messageTemplateService.GetByAIAgentAsync(aiAgentId.Value, companyId);
        }
        else if (!string.IsNullOrWhiteSpace(tone))
        {
            templates = await _messageTemplateService.GetByToneAsync(tone, companyId);
        }
        else if (aiGenerated.HasValue)
        {
            templates = aiGenerated.Value 
                ? await _messageTemplateService.GetAIGeneratedByCompanyAsync(companyId)
                : await _messageTemplateService.GetManualByCompanyAsync(companyId);
        }
        else
        {
            templates = activeOnly 
                ? await _messageTemplateService.GetActiveByCompanyAsync(companyId)
                : await _messageTemplateService.GetAllByCompanyAsync(companyId);
        }

        return Ok(templates);
    }

    [HttpGet("company/{companyId:guid}")]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetByCompany(Guid companyId)
    {
        _logger.LogDebug("Fetching message templates for company: {CompanyId}", companyId);
        var templates = await _messageTemplateService.GetActiveByCompanyAsync(companyId);
        return Ok(templates);
    }

    [HttpGet("company/{companyId:guid}/ai-generated")]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetAIGeneratedByCompany(Guid companyId)
    {
        _logger.LogDebug("Fetching AI-generated message templates for company: {CompanyId}", companyId);
        var templates = await _messageTemplateService.GetAIGeneratedByCompanyAsync(companyId);
        return Ok(templates);
    }

    [HttpGet("company/{companyId:guid}/manual")]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetManualByCompany(Guid companyId)
    {
        _logger.LogDebug("Fetching manual message templates for company: {CompanyId}", companyId);
        var templates = await _messageTemplateService.GetManualByCompanyAsync(companyId);
        return Ok(templates);
    }

    [HttpGet("tone/{tone}/company/{companyId:guid}")]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetByTone(string tone, Guid companyId)
    {
        _logger.LogDebug("Fetching message templates by tone: {Tone} for company: {CompanyId}", tone, companyId);
        var templates = await _messageTemplateService.GetByToneAsync(tone, companyId);
        return Ok(templates);
    }

    [HttpGet("user/{userId:guid}/company/{companyId:guid}")]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetByCreatedByUser(Guid userId, Guid companyId)
    {
        _logger.LogDebug("Fetching message templates created by user: {UserId} for company: {CompanyId}", userId, companyId);
        var templates = await _messageTemplateService.GetByCreatedByUserAsync(userId, companyId);
        return Ok(templates);
    }

    [HttpGet("ai-agent/{aiAgentId:guid}/company/{companyId:guid}")]
    public async Task<ActionResult<List<MessageTemplateDto>>> GetByAIAgent(Guid aiAgentId, Guid companyId)
    {
        _logger.LogDebug("Fetching message templates by AI agent: {AIAgentId} for company: {CompanyId}", aiAgentId, companyId);
        var templates = await _messageTemplateService.GetByAIAgentAsync(aiAgentId, companyId);
        return Ok(templates);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<MessageTemplateDto>> Update(
        Guid id, 
        [FromBody] UpdateMessageTemplateDto updateDto, 
        [FromQuery] Guid companyId,
        [FromQuery] Guid? currentUserId = null)
    {
        _logger.LogInformation("Updating message template: {Id} for company: {CompanyId}", id, companyId);

        try
        {
            var updated = await _messageTemplateService.UpdateAsync(id, updateDto, companyId, currentUserId);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating message template: {Message}", ex.Message);
            if (ex.Message.Contains("não encontrado"))
            {
                return NotFound();
            }
            return BadRequest(ex.Message);
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id, [FromQuery] Guid companyId, [FromQuery] bool softDelete = true)
    {
        _logger.LogInformation("Deleting message template: {Id} for company: {CompanyId}, softDelete: {SoftDelete}", id, companyId, softDelete);

        try
        {
            if (softDelete)
            {
                await _messageTemplateService.SoftDeleteAsync(id, companyId);
            }
            else
            {
                await _messageTemplateService.DeleteAsync(id, companyId);
            }
            
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting message template: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("company/{companyId:guid}/count")]
    public async Task<ActionResult<long>> CountByCompany(Guid companyId, [FromQuery] bool activeOnly = true)
    {
        var count = activeOnly 
            ? await _messageTemplateService.CountActiveByCompanyAsync(companyId)
            : await _messageTemplateService.CountByCompanyAsync(companyId);
        return Ok(count);
    }

    [HttpGet("company/{companyId:guid}/count/ai-generated")]
    public async Task<ActionResult<long>> CountAIGeneratedByCompany(Guid companyId)
    {
        var count = await _messageTemplateService.CountAIGeneratedByCompanyAsync(companyId);
        return Ok(count);
    }

    [HttpHead("company/{companyId:guid}/name/{name}")]
    public async Task<ActionResult> CheckTemplateNameExists(Guid companyId, string name)
    {
        var exists = await _messageTemplateService.ExistsByNameAndCompanyAsync(name, companyId);
        return exists ? Ok() : NotFound();
    }
}