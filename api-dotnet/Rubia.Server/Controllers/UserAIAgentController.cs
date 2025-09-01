using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/user-ai-agents")]
[Authorize]
public class UserAIAgentController : ControllerBase
{
    private readonly UserAIAgentService _userAIAgentService;
    private readonly CompanyContextService _companyContextService;
    private readonly ILogger<UserAIAgentController> _logger;

    public UserAIAgentController(
        UserAIAgentService userAIAgentService,
        CompanyContextService companyContextService,
        ILogger<UserAIAgentController> logger)
    {
        _userAIAgentService = userAIAgentService;
        _companyContextService = companyContextService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<UserAIAgentDto>> Create([FromBody] CreateUserAIAgentDto createDto)
    {
        _logger.LogDebug("Creating UserAIAgent assignment: User {UserId} to AI Agent {AiAgentId}",
            createDto.UserId, createDto.AiAgentId);

        var userAIAgent = await _userAIAgentService.CreateAsync(
            createDto.UserId, 
            createDto.AiAgentId, 
            createDto.IsDefault);

        var responseDto = ConvertToDto(userAIAgent);
        return Ok(responseDto);
    }

    [HttpPut("{id}")]
    public async Task<ActionResult<UserAIAgentDto>> Update(Guid id, [FromBody] UpdateUserAIAgentDto updateDto)
    {
        _logger.LogDebug("Updating UserAIAgent {Id}", id);

        var userAIAgent = await _userAIAgentService.UpdateAsync(id, updateDto.IsDefault);
        if (userAIAgent == null)
        {
            return NotFound();
        }

        var responseDto = ConvertToDto(userAIAgent);
        return Ok(responseDto);
    }

    [HttpDelete("{id}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        _logger.LogDebug("Deleting UserAIAgent {Id}", id);

        var deleted = await _userAIAgentService.DeleteAsync(id);
        if (!deleted)
        {
            return NotFound();
        }

        return NoContent();
    }

    [HttpGet("{id}")]
    public async Task<ActionResult<UserAIAgentDto>> GetById(Guid id)
    {
        var userAIAgent = await _userAIAgentService.GetByIdAsync(id);
        if (userAIAgent == null)
        {
            return NotFound();
        }

        var responseDto = ConvertToDto(userAIAgent);
        return Ok(responseDto);
    }

    [HttpGet("user/{userId}")]
    public async Task<ActionResult<List<UserAIAgentDto>>> GetByUserId(Guid userId)
    {
        _logger.LogDebug("Finding UserAIAgents by user ID: {UserId}", userId);

        var userAIAgents = await _userAIAgentService.GetByUserIdAsync(userId);
        var responseDtos = userAIAgents.Select(ConvertToDto).ToList();

        return Ok(responseDtos);
    }

    [HttpGet("ai-agent/{aiAgentId}")]
    public async Task<ActionResult<List<UserAIAgentDto>>> GetByAiAgentId(Guid aiAgentId)
    {
        _logger.LogDebug("Finding UserAIAgents by AI agent ID: {AiAgentId}", aiAgentId);

        var userAIAgents = await _userAIAgentService.GetByAiAgentIdAsync(aiAgentId);
        var responseDtos = userAIAgents.Select(ConvertToDto).ToList();

        return Ok(responseDtos);
    }

    [HttpGet("user/{userId}/ai-agent/{aiAgentId}")]
    public async Task<ActionResult<UserAIAgentDto>> GetByUserIdAndAiAgentId(Guid userId, Guid aiAgentId)
    {
        _logger.LogDebug("Finding UserAIAgent by user ID: {UserId} and AI agent ID: {AiAgentId}", userId, aiAgentId);

        var userAIAgent = await _userAIAgentService.GetByUserIdAndAiAgentIdAsync(userId, aiAgentId);
        if (userAIAgent == null)
        {
            return NotFound();
        }

        var responseDto = ConvertToDto(userAIAgent);
        return Ok(responseDto);
    }

    [HttpGet("default/{isDefault}")]
    public async Task<ActionResult<List<UserAIAgentDto>>> GetByIsDefault(bool isDefault)
    {
        _logger.LogDebug("Finding UserAIAgents by isDefault: {IsDefault}", isDefault);

        var userAIAgents = await _userAIAgentService.GetByIsDefaultAsync(isDefault);
        var responseDtos = userAIAgents.Select(ConvertToDto).ToList();

        return Ok(responseDtos);
    }

    [HttpGet("user/{userId}/default")]
    public async Task<ActionResult<UserAIAgentDto>> GetByUserIdAndIsDefault(Guid userId)
    {
        _logger.LogDebug("Finding default UserAIAgent for user ID: {UserId}", userId);

        var userAIAgent = await _userAIAgentService.GetByUserIdAndIsDefaultAsync(userId, true);
        if (userAIAgent == null)
        {
            return NotFound();
        }

        var responseDto = ConvertToDto(userAIAgent);
        return Ok(responseDto);
    }

    [HttpGet("user/{userId}/ai-agent/{aiAgentId}/exists")]
    public async Task<ActionResult<bool>> ExistsByUserIdAndAiAgentId(Guid userId, Guid aiAgentId)
    {
        _logger.LogDebug("Checking if UserAIAgent exists by user ID: {UserId} and AI agent ID: {AiAgentId}", userId, aiAgentId);

        var exists = await _userAIAgentService.ExistsByUserIdAndAiAgentIdAsync(userId, aiAgentId);
        return Ok(exists);
    }

    [HttpGet("count/ai-agent/{aiAgentId}")]
    public async Task<ActionResult<int>> CountByAiAgentId(Guid aiAgentId)
    {
        _logger.LogDebug("Counting UserAIAgents by AI agent ID: {AiAgentId}", aiAgentId);

        var count = await _userAIAgentService.CountByAiAgentIdAsync(aiAgentId);
        return Ok(count);
    }

    [HttpGet("count/user/{userId}")]
    public async Task<ActionResult<int>> CountByUserId(Guid userId)
    {
        _logger.LogDebug("Counting UserAIAgents by user ID: {UserId}", userId);

        var count = await _userAIAgentService.CountByUserIdAsync(userId);
        return Ok(count);
    }

    [HttpGet("count/default/{isDefault}")]
    public async Task<ActionResult<int>> CountByIsDefault(bool isDefault)
    {
        _logger.LogDebug("Counting UserAIAgents by isDefault: {IsDefault}", isDefault);

        var count = await _userAIAgentService.CountByIsDefaultAsync(isDefault);
        return Ok(count);
    }

    [HttpPut("{id}/set-default")]
    public async Task<ActionResult<UserAIAgentDto>> SetAsDefault(Guid id, [FromQuery] bool isDefault)
    {
        _logger.LogDebug("Setting UserAIAgent as default with ID: {Id} and status: {IsDefault}", id, isDefault);

        var updated = await _userAIAgentService.SetAsDefaultAsync(id, isDefault);
        if (updated == null)
        {
            return NotFound();
        }

        var responseDto = ConvertToDto(updated);
        return Ok(responseDto);
    }

    [HttpDelete("user/{userId}/clear-defaults")]
    public async Task<IActionResult> ClearDefaultForUser(Guid userId)
    {
        _logger.LogDebug("Clearing default UserAIAgent for user: {UserId}", userId);

        await _userAIAgentService.ClearDefaultForUserAsync(userId);
        return NoContent();
    }

    [HttpPost("assign")]
    public async Task<ActionResult<UserAIAgentDto>> AssignUserToAgent(
        [FromQuery] Guid userId,
        [FromQuery] Guid aiAgentId,
        [FromQuery] bool isDefault = false)
    {
        _logger.LogDebug("Assigning user: {UserId} to AI agent: {AiAgentId} with default: {IsDefault}",
            userId, aiAgentId, isDefault);

        var userAIAgent = await _userAIAgentService.CreateAsync(userId, aiAgentId, isDefault);
        var responseDto = ConvertToDto(userAIAgent);

        return Ok(responseDto);
    }

    [HttpDelete("user/{userId}/ai-agent/{aiAgentId}")]
    public async Task<IActionResult> RemoveUserFromAgent(Guid userId, Guid aiAgentId)
    {
        _logger.LogDebug("Removing user: {UserId} from AI agent: {AiAgentId}", userId, aiAgentId);

        var removed = await _userAIAgentService.RemoveUserFromAgentAsync(userId, aiAgentId);
        if (!removed)
        {
            return NotFound();
        }

        return NoContent();
    }

    private UserAIAgentDto ConvertToDto(UserAIAgent userAIAgent)
    {
        return new UserAIAgentDto
        {
            Id = userAIAgent.Id,
            CompanyId = userAIAgent.CompanyId,
            CompanyName = userAIAgent.Company?.Name,
            UserId = userAIAgent.UserId,
            UserName = userAIAgent.User?.Name,
            AiAgentId = userAIAgent.AiAgentId,
            AiAgentName = userAIAgent.AiAgent?.Name,
            IsDefault = userAIAgent.IsDefault,
            AssignedAt = userAIAgent.AssignedAt,
            CreatedAt = userAIAgent.CreatedAt,
            UpdatedAt = userAIAgent.UpdatedAt
        };
    }
}