using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/ai-logs")]
[Authorize]
public class AILogController : ControllerBase
{
    private readonly IAILogService _aiLogService;
    private readonly ILogger<AILogController> _logger;

    public AILogController(IAILogService aiLogService, ILogger<AILogController> logger)
    {
        _aiLogService = aiLogService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<AILogDto>> CreateAILog([FromBody] CreateAILogDto createDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            _logger.LogInformation("Creating new AI log for company: {CompanyId}", createDto.CompanyId);
            var aiLog = await _aiLogService.CreateAILogAsync(createDto);
            var dto = MapToDto(aiLog);
            
            return CreatedAtAction(nameof(GetAILogById), new { id = aiLog.Id }, dto);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating AI log");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<AILogDto>> GetAILogById(Guid id)
    {
        try
        {
            _logger.LogInformation("Fetching AI log with ID: {Id}", id);
            var aiLog = await _aiLogService.GetAILogByIdAsync(id);
            
            if (aiLog == null)
            {
                return NotFound();
            }

            return Ok(MapToDto(aiLog));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching AI log {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<AILogDto>>> GetAllAILogs(
        [FromQuery] int page = 0,
        [FromQuery] int size = 10,
        [FromQuery] string sortBy = "createdAt",
        [FromQuery] string sortDir = "desc")
    {
        try
        {
            var aiLogs = await _aiLogService.GetAllAILogsAsync(page, size, sortBy, sortDir);
            var dtos = aiLogs.Select(MapToDto);
            
            return Ok(dtos);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching AI logs");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("company/{companyId:guid}")]
    public async Task<ActionResult<IEnumerable<AILogDto>>> GetAILogsByCompanyId(Guid companyId)
    {
        try
        {
            _logger.LogInformation("Fetching AI logs for company: {CompanyId}", companyId);
            var aiLogs = await _aiLogService.GetAILogsByCompanyIdAsync(companyId);
            var dtos = aiLogs.Select(MapToDto);
            
            return Ok(dtos);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching AI logs for company {CompanyId}", companyId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("status/{status}")]
    public async Task<ActionResult<IEnumerable<AILogDto>>> GetAILogsByStatus(AILogStatus status)
    {
        try
        {
            _logger.LogInformation("Fetching AI logs with status: {Status}", status);
            var aiLogs = await _aiLogService.GetAILogsByStatusAsync(status);
            var dtos = aiLogs.Select(MapToDto);
            
            return Ok(dtos);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching AI logs by status {Status}", status);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("ai-agent/{aiAgentId:guid}")]
    public async Task<ActionResult<IEnumerable<AILogDto>>> GetAILogsByAIAgentId(Guid aiAgentId)
    {
        try
        {
            _logger.LogInformation("Fetching AI logs for AI agent: {AiAgentId}", aiAgentId);
            var aiLogs = await _aiLogService.GetAILogsByAIAgentIdAsync(aiAgentId);
            var dtos = aiLogs.Select(MapToDto);
            
            return Ok(dtos);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching AI logs for AI agent {AiAgentId}", aiAgentId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<AILogDto>> UpdateAILog(Guid id, [FromBody] UpdateAILogDto updateDto)
    {
        try
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            _logger.LogInformation("Updating AI log with ID: {Id}", id);
            var updatedAILog = await _aiLogService.UpdateAILogAsync(id, updateDto);
            
            if (updatedAILog == null)
            {
                return NotFound();
            }

            return Ok(MapToDto(updatedAILog));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating AI log {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteAILog(Guid id)
    {
        try
        {
            _logger.LogInformation("Deleting AI log with ID: {Id}", id);
            var deleted = await _aiLogService.DeleteAILogAsync(id);
            
            if (!deleted)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting AI log {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("company/{companyId:guid}/total-cost")]
    public async Task<ActionResult<decimal>> GetTotalCostByCompanyId(Guid companyId)
    {
        try
        {
            _logger.LogInformation("Calculating total cost for company: {CompanyId}", companyId);
            var totalCost = await _aiLogService.GetTotalCostByCompanyIdAsync(companyId);
            
            return Ok(totalCost);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error calculating total cost for company {CompanyId}", companyId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("company/{companyId:guid}/total-tokens")]
    public async Task<ActionResult<long>> GetTotalTokensUsedByCompanyId(Guid companyId)
    {
        try
        {
            _logger.LogInformation("Calculating total tokens used for company: {CompanyId}", companyId);
            var totalTokens = await _aiLogService.GetTotalTokensUsedByCompanyIdAsync(companyId);
            
            return Ok(totalTokens);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error calculating total tokens for company {CompanyId}", companyId);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("company/{companyId:guid}/count")]
    public async Task<ActionResult<long>> CountAILogsByCompanyIdAndStatus(
        Guid companyId,
        [FromQuery] AILogStatus status)
    {
        try
        {
            _logger.LogInformation("Counting AI logs for company: {CompanyId} with status: {Status}", companyId, status);
            var count = await _aiLogService.CountAILogsByCompanyIdAndStatusAsync(companyId, status);
            
            return Ok(count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error counting AI logs for company {CompanyId} with status {Status}", companyId, status);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("date-range")]
    public async Task<ActionResult<IEnumerable<AILogDto>>> GetAILogsByDateRange(
        [FromQuery] DateTime startDate,
        [FromQuery] DateTime endDate)
    {
        try
        {
            _logger.LogInformation("Fetching AI logs between {StartDate} and {EndDate}", startDate, endDate);
            var aiLogs = await _aiLogService.GetAILogsByDateRangeAsync(startDate, endDate);
            var dtos = aiLogs.Select(MapToDto);
            
            return Ok(dtos);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error fetching AI logs by date range {StartDate} - {EndDate}", startDate, endDate);
            return StatusCode(500, "Internal server error");
        }
    }

    private static AILogDto MapToDto(AILog aiLog)
    {
        return new AILogDto
        {
            Id = aiLog.Id,
            CompanyId = aiLog.CompanyId,
            AiAgentId = aiLog.AiAgentId,
            UserId = aiLog.UserId,
            ConversationId = aiLog.ConversationId,
            MessageId = aiLog.MessageId,
            MessageTemplateId = aiLog.MessageTemplateId,
            RequestPrompt = aiLog.RequestPrompt,
            RawResponse = aiLog.RawResponse,
            ProcessedResponse = aiLog.ProcessedResponse,
            TokensUsedInput = aiLog.TokensUsedInput,
            TokensUsedOutput = aiLog.TokensUsedOutput,
            EstimatedCost = aiLog.EstimatedCost,
            Status = aiLog.Status,
            ErrorMessage = aiLog.ErrorMessage,
            CreatedAt = aiLog.CreatedAt
        };
    }
}