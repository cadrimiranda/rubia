using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/faqs")]
[Authorize]
public class FAQsController : ControllerBase
{
    private readonly IFAQService _faqService;
    private readonly ILogger<FAQsController> _logger;

    public FAQsController(IFAQService faqService, ILogger<FAQsController> logger)
    {
        _faqService = faqService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<FAQDto>>> GetFAQs()
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var faqs = await _faqService.GetAllAsync(companyId.Value);
            return Ok(faqs);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting FAQs");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<FAQDto>> GetFAQ(Guid id)
    {
        try
        {
            var faq = await _faqService.GetByIdAsync(id);
            if (faq == null)
            {
                return NotFound();
            }

            return Ok(faq);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting FAQ {FAQId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<FAQDto>> CreateFAQ([FromBody] CreateFAQDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            dto.CompanyId = companyId.Value;
            var faq = await _faqService.CreateAsync(dto);
            
            return CreatedAtAction(nameof(GetFAQ), new { id = faq.Id }, faq);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating FAQ");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<FAQDto>> UpdateFAQ(Guid id, [FromBody] UpdateFAQDto dto)
    {
        try
        {
            var faq = await _faqService.UpdateAsync(id, dto);
            if (faq == null)
            {
                return NotFound();
            }

            return Ok(faq);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating FAQ {FAQId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteFAQ(Guid id)
    {
        try
        {
            var success = await _faqService.DeleteAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting FAQ {FAQId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("search")]
    public async Task<ActionResult<IEnumerable<FAQMatchDto>>> SearchFAQs([FromBody] FAQSearchDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var matches = await _faqService.SearchAsync(companyId.Value, dto.Query);
            return Ok(matches);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error searching FAQs");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id:guid}/increment-usage")]
    public async Task<IActionResult> IncrementUsage(Guid id)
    {
        try
        {
            await _faqService.IncrementUsageAsync(id);
            return Ok(new { message = "Usage count incremented" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error incrementing FAQ usage {FAQId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("stats")]
    public async Task<ActionResult<FAQStatsDto>> GetStats()
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var stats = await _faqService.GetStatsAsync(companyId.Value);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting FAQ stats");
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = User.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }
}