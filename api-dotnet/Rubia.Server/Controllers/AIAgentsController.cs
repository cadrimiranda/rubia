using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/ai-agents")]
public class AIAgentsController : ControllerBase
{
    private readonly IAIAgentService _aiAgentService;
    private readonly ILogger<AIAgentsController> _logger;

    public AIAgentsController(IAIAgentService aiAgentService, ILogger<AIAgentsController> logger)
    {
        _aiAgentService = aiAgentService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<AIAgentDto>> Create([FromBody] CreateAIAgentDto createDto)
    {
        _logger.LogInformation("Creating AI agent with name: {Name} for company: {CompanyId}", createDto.Name, createDto.CompanyId);

        try
        {
            var created = await _aiAgentService.CreateAsync(createDto);
            return CreatedAtAction(nameof(GetById), new { id = created.Id }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating AI agent: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<AIAgentDto>> GetById(Guid id)
    {
        _logger.LogDebug("Fetching AI agent with id: {Id}", id);

        try
        {
            var agent = await _aiAgentService.GetByIdAsync(id);
            return Ok(agent);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("AI agent not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<AIAgentDto>>> GetAll([FromQuery] Guid companyId, [FromQuery] bool activeOnly = false)
    {
        _logger.LogDebug("Fetching AI agents for company: {CompanyId}, activeOnly: {ActiveOnly}", companyId, activeOnly);

        var agents = activeOnly 
            ? await _aiAgentService.GetActiveByCompanyAsync(companyId)
            : await _aiAgentService.GetAllByCompanyAsync(companyId);

        return Ok(agents);
    }

    [HttpGet("company/{companyId:guid}")]
    public async Task<ActionResult<List<AIAgentDto>>> GetByCompany(Guid companyId)
    {
        _logger.LogDebug("Fetching AI agents for company: {CompanyId}", companyId);
        var agents = await _aiAgentService.GetAllByCompanyOrderByNameAsync(companyId);
        return Ok(agents);
    }

    [HttpGet("company/{companyId:guid}/active")]
    public async Task<ActionResult<List<AIAgentDto>>> GetActiveByCompany(Guid companyId)
    {
        _logger.LogDebug("Fetching active AI agents for company: {CompanyId}", companyId);
        var agents = await _aiAgentService.GetActiveByCompanyAsync(companyId);
        return Ok(agents);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<AIAgentDto>> Update(Guid id, [FromBody] UpdateAIAgentDto updateDto)
    {
        _logger.LogInformation("Updating AI agent: {Id}", id);

        try
        {
            var updated = await _aiAgentService.UpdateAsync(id, updateDto);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating AI agent: {Message}", ex.Message);
            if (ex.Message.Contains("não encontrado"))
            {
                return NotFound();
            }
            return BadRequest(ex.Message);
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        _logger.LogInformation("Deleting AI agent: {Id}", id);

        try
        {
            await _aiAgentService.DeleteAsync(id);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting AI agent: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("company/{companyId:guid}/count")]
    public async Task<ActionResult<long>> CountByCompany(Guid companyId)
    {
        var count = await _aiAgentService.CountByCompanyAsync(companyId);
        return Ok(count);
    }

    [HttpGet("company/{companyId:guid}/count/active")]
    public async Task<ActionResult<long>> CountActiveByCompany(Guid companyId)
    {
        var count = await _aiAgentService.CountActiveByCompanyAsync(companyId);
        return Ok(count);
    }

    [HttpGet("company/{companyId:guid}/can-create")]
    public async Task<ActionResult<bool>> CanCreateAgent(Guid companyId)
    {
        try
        {
            var canCreate = await _aiAgentService.CanCreateAgentAsync(companyId);
            return Ok(canCreate);
        }
        catch (ArgumentException)
        {
            return NotFound();
        }
    }

    [HttpGet("company/{companyId:guid}/remaining-slots")]
    public async Task<ActionResult<int>> GetRemainingAgentSlots(Guid companyId)
    {
        try
        {
            var remainingSlots = await _aiAgentService.GetRemainingAgentSlotsAsync(companyId);
            return Ok(remainingSlots);
        }
        catch (ArgumentException)
        {
            return NotFound();
        }
    }

    [HttpGet("model/{modelId:guid}")]
    public async Task<ActionResult<List<AIAgentDto>>> GetByModelId(Guid modelId)
    {
        _logger.LogDebug("Fetching AI agents by model id: {ModelId}", modelId);
        var agents = await _aiAgentService.GetByModelIdAsync(modelId);
        return Ok(agents);
    }

    [HttpGet("temperament/{temperament}")]
    public async Task<ActionResult<List<AIAgentDto>>> GetByTemperament(string temperament)
    {
        _logger.LogDebug("Fetching AI agents by temperament: {Temperament}", temperament);
        var agents = await _aiAgentService.GetByTemperamentAsync(temperament);
        return Ok(agents);
    }

    [HttpGet("company/{companyId:guid}/message-limit")]
    public async Task<ActionResult<int?>> GetAIMessageLimitForCompany(Guid companyId)
    {
        var limit = await _aiAgentService.GetAIMessageLimitForCompanyAsync(companyId);
        return Ok(limit);
    }

    [HttpHead("company/{companyId:guid}/name/{name}")]
    public async Task<ActionResult> CheckAgentNameExists(Guid companyId, string name)
    {
        var exists = await _aiAgentService.ExistsByNameAndCompanyAsync(name, companyId);
        return exists ? Ok() : NotFound();
    }
}