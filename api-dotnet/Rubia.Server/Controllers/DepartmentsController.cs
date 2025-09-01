using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/departments")]
public class DepartmentsController : ControllerBase
{
    private readonly IDepartmentService _departmentService;
    private readonly ILogger<DepartmentsController> _logger;

    public DepartmentsController(IDepartmentService departmentService, ILogger<DepartmentsController> logger)
    {
        _departmentService = departmentService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<DepartmentDto>> Create([FromQuery] Guid companyId, [FromBody] CreateDepartmentDto createDto)
    {
        _logger.LogInformation("Creating department: {Name}", createDto.Name);
        
        try
        {
            var created = await _departmentService.CreateAsync(createDto, companyId);
            return CreatedAtAction(nameof(FindById), new { id = created.Id, companyId }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating department: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<DepartmentDto>> FindById(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogDebug("Finding department by id: {Id}", id);
        
        try
        {
            var department = await _departmentService.FindByIdAsync(id, companyId);
            return Ok(department);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Department not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<DepartmentDto>>> FindAll([FromQuery] Guid companyId, [FromQuery] bool autoAssignOnly = false)
    {
        _logger.LogDebug("Finding all departments, autoAssignOnly: {AutoAssignOnly}", autoAssignOnly);
        
        var departments = autoAssignOnly 
            ? await _departmentService.FindByAutoAssignAsync(companyId)
            : await _departmentService.FindAllAsync(companyId);
            
        return Ok(departments);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<DepartmentDto>> Update(Guid id, [FromQuery] Guid companyId, [FromBody] UpdateDepartmentDto updateDto)
    {
        _logger.LogInformation("Updating department: {Id}", id);
        
        try
        {
            var updated = await _departmentService.UpdateAsync(id, updateDto, companyId);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating department: {Message}", ex.Message);
            if (ex.Message.Contains("não encontrado"))
            {
                return NotFound();
            }
            return BadRequest(ex.Message);
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Deleting department: {Id}", id);
        
        try
        {
            await _departmentService.DeleteAsync(id, companyId);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting department: {Id}", id);
            return NotFound();
        }
    }
}