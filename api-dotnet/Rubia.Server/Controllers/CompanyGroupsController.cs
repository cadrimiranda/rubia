using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/company-groups")]
public class CompanyGroupsController : ControllerBase
{
    private readonly ICompanyGroupService _companyGroupService;
    private readonly ILogger<CompanyGroupsController> _logger;

    public CompanyGroupsController(ICompanyGroupService companyGroupService, ILogger<CompanyGroupsController> logger)
    {
        _companyGroupService = companyGroupService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<CompanyGroupDto>> Create([FromBody] CreateCompanyGroupDto createDto)
    {
        _logger.LogInformation("Creating company group: {Name}", createDto.Name);
        
        try
        {
            var created = await _companyGroupService.CreateAsync(createDto);
            return CreatedAtAction(nameof(FindById), new { id = created.Id }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating company group: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<CompanyGroupDto>>> FindAll()
    {
        _logger.LogDebug("Finding all company groups");
        var companyGroups = await _companyGroupService.FindAllAsync();
        return Ok(companyGroups);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<CompanyGroupDto>> FindById(Guid id)
    {
        _logger.LogDebug("Finding company group by id: {Id}", id);
        
        try
        {
            var companyGroup = await _companyGroupService.FindByIdAsync(id);
            return Ok(companyGroup);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Company group not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<CompanyGroupDto>> Update(Guid id, [FromBody] UpdateCompanyGroupDto updateDto)
    {
        _logger.LogInformation("Updating company group: {Id}", id);
        
        try
        {
            var updated = await _companyGroupService.UpdateAsync(id, updateDto);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating company group: {Message}", ex.Message);
            if (ex.Message.Contains("not found"))
            {
                return NotFound();
            }
            return BadRequest(ex.Message);
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        _logger.LogInformation("Deleting company group: {Id}", id);
        
        try
        {
            await _companyGroupService.DeleteAsync(id);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting company group: {Id}", id);
            return NotFound();
        }
    }
}