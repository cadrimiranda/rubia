using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/companies")]
public class CompaniesController : ControllerBase
{
    private readonly ICompanyService _companyService;
    private readonly ILogger<CompaniesController> _logger;

    public CompaniesController(ICompanyService companyService, ILogger<CompaniesController> logger)
    {
        _companyService = companyService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<CompanyDto>> Create([FromBody] CreateCompanyDto createDto)
    {
        _logger.LogInformation("Creating company: {Name}", createDto.Name);
        
        try
        {
            var created = await _companyService.CreateAsync(createDto);
            return CreatedAtAction(nameof(FindById), new { id = created.Id }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating company: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<CompanyDto>>> FindAll()
    {
        _logger.LogDebug("Finding all companies");
        var companies = await _companyService.FindAllAsync();
        return Ok(companies);
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<CompanyDto>> FindById(Guid id)
    {
        _logger.LogDebug("Finding company by id: {Id}", id);
        
        var company = await _companyService.FindByIdAsync(id);
        if (company == null)
        {
            _logger.LogWarning("Company not found: {Id}", id);
            return NotFound();
        }

        return Ok(company);
    }

    [HttpGet("slug/{slug}")]
    public async Task<ActionResult<CompanyDto>> FindBySlug(string slug)
    {
        _logger.LogDebug("Finding company by slug: {Slug}", slug);
        
        var company = await _companyService.FindBySlugAsync(slug);
        if (company == null)
        {
            _logger.LogWarning("Company not found: {Slug}", slug);
            return NotFound();
        }

        return Ok(company);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<CompanyDto>> Update(Guid id, [FromBody] UpdateCompanyDto updateDto)
    {
        _logger.LogInformation("Updating company: {Id}", id);
        
        try
        {
            var updated = await _companyService.UpdateAsync(id, updateDto);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating company: {Message}", ex.Message);
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
        _logger.LogInformation("Deleting company: {Id}", id);
        
        try
        {
            await _companyService.DeleteByIdAsync(id);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting company: {Id}", id);
            return NotFound();
        }
    }
}