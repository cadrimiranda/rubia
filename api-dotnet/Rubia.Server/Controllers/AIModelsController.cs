using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/ai-models")]
public class AIModelsController : ControllerBase
{
    private readonly IAIModelService _aiModelService;
    private readonly ILogger<AIModelsController> _logger;

    public AIModelsController(IAIModelService aiModelService, ILogger<AIModelsController> logger)
    {
        _aiModelService = aiModelService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<List<AIModelDto>>> GetAllModels()
    {
        _logger.LogInformation("GET /api/ai-models - Fetching all AI models");
        var models = await _aiModelService.GetAllModelsAsync();
        return Ok(models);
    }

    [HttpGet("active")]
    public async Task<ActionResult<List<AIModelDto>>> GetActiveModels()
    {
        _logger.LogInformation("GET /api/ai-models/active - Fetching active AI models");
        var models = await _aiModelService.GetActiveModelsAsync();
        return Ok(models);
    }

    [HttpGet("provider/{provider}")]
    public async Task<ActionResult<List<AIModelDto>>> GetModelsByProvider(string provider)
    {
        _logger.LogInformation("GET /api/ai-models/provider/{Provider} - Fetching AI models by provider", provider);
        var models = await _aiModelService.GetModelsByProviderAsync(provider);
        return Ok(models);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<AIModelDto>> GetModelById(Guid id)
    {
        _logger.LogInformation("GET /api/ai-models/{Id} - Fetching AI model by id", id);
        
        try
        {
            var model = await _aiModelService.GetModelByIdAsync(id);
            return Ok(model);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("AI model not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("name/{name}")]
    public async Task<ActionResult<AIModelDto>> GetModelByName(string name)
    {
        _logger.LogInformation("GET /api/ai-models/name/{Name} - Fetching AI model by name", name);
        
        var model = await _aiModelService.GetModelByNameAsync(name);
        
        if (model == null)
        {
            _logger.LogWarning("AI model not found: {Name}", name);
            return NotFound();
        }
        
        return Ok(model);
    }

    [HttpPost]
    public async Task<ActionResult<AIModelDto>> Create([FromBody] CreateAIModelDto createDto)
    {
        _logger.LogInformation("Creating AI model: {Name}", createDto.Name);
        
        try
        {
            var created = await _aiModelService.CreateAsync(createDto);
            return CreatedAtAction(nameof(GetModelById), new { id = created.Id }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating AI model: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<AIModelDto>> Update(Guid id, [FromBody] UpdateAIModelDto updateDto)
    {
        _logger.LogInformation("Updating AI model: {Id}", id);
        
        try
        {
            var updated = await _aiModelService.UpdateAsync(id, updateDto);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating AI model: {Message}", ex.Message);
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
        _logger.LogInformation("Deleting AI model: {Id}", id);
        
        try
        {
            await _aiModelService.DeleteAsync(id);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting AI model: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("count/active")]
    public async Task<ActionResult<long>> CountActiveModels()
    {
        var count = await _aiModelService.CountActiveModelsAsync();
        return Ok(count);
    }

    [HttpHead("exists/{name}")]
    public async Task<ActionResult> CheckModelExists(string name)
    {
        var exists = await _aiModelService.ExistsByNameAsync(name);
        return exists ? Ok() : NotFound();
    }
}