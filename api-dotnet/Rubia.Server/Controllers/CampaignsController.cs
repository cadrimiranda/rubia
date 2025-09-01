using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/campaigns")]
[Authorize]
public class CampaignsController : ControllerBase
{
    private readonly ICampaignService _campaignService;
    private readonly ILogger<CampaignsController> _logger;

    public CampaignsController(ICampaignService campaignService, ILogger<CampaignsController> logger)
    {
        _campaignService = campaignService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<ActionResult<IEnumerable<CampaignDto>>> GetCampaigns()
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var campaigns = await _campaignService.GetAllAsync(companyId.Value);
            return Ok(campaigns);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting campaigns");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<CampaignDto>> GetCampaign(Guid id)
    {
        try
        {
            var campaign = await _campaignService.GetByIdAsync(id);
            if (campaign == null)
            {
                return NotFound();
            }

            return Ok(campaign);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting campaign {CampaignId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost]
    public async Task<ActionResult<CampaignDto>> CreateCampaign([FromBody] CreateCampaignDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            dto.CompanyId = companyId.Value;
            var campaign = await _campaignService.CreateAsync(dto);
            
            return CreatedAtAction(nameof(GetCampaign), new { id = campaign.Id }, campaign);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error creating campaign");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<CampaignDto>> UpdateCampaign(Guid id, [FromBody] UpdateCampaignDto dto)
    {
        try
        {
            var campaign = await _campaignService.UpdateAsync(id, dto);
            if (campaign == null)
            {
                return NotFound();
            }

            return Ok(campaign);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating campaign {CampaignId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> DeleteCampaign(Guid id)
    {
        try
        {
            var success = await _campaignService.DeleteAsync(id);
            if (!success)
            {
                return NotFound();
            }

            return NoContent();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting campaign {CampaignId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPatch("{id:guid}/status")]
    public async Task<ActionResult<CampaignDto>> UpdateCampaignStatus(
        Guid id, 
        [FromBody] UpdateCampaignStatusDto dto)
    {
        try
        {
            var campaign = await _campaignService.UpdateStatusAsync(id, dto.Status);
            if (campaign == null)
            {
                return NotFound();
            }

            return Ok(campaign);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error updating campaign status {CampaignId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("{id:guid}/contacts")]
    public async Task<IActionResult> AddContacts(Guid id, [FromBody] AddContactsDto dto)
    {
        try
        {
            var addedCount = await _campaignService.AddContactsAsync(id, dto.CustomerIds);
            return Ok(new { message = $"Added {addedCount} new contacts to campaign" });
        }
        catch (ArgumentException ex)
        {
            return NotFound(new { message = ex.Message });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error adding contacts to campaign {CampaignId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("{id:guid}/stats")]
    public async Task<ActionResult<CampaignStatsDto>> GetCampaignStats(Guid id)
    {
        try
        {
            var stats = await _campaignService.GetStatsAsync(id);
            return Ok(stats);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting campaign stats {CampaignId}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = User.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }
}