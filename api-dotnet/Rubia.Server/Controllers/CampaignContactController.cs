using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/[controller]")]
public class CampaignContactController : ControllerBase
{
    private readonly ICampaignContactService _campaignContactService;
    private readonly ILogger<CampaignContactController> _logger;

    public CampaignContactController(
        ICampaignContactService campaignContactService,
        ILogger<CampaignContactController> logger)
    {
        _campaignContactService = campaignContactService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<CampaignContactDto>> CreateCampaignContact([FromBody] CreateCampaignContactDto createDto, CancellationToken cancellationToken)
    {
        try
        {
            var result = await _campaignContactService.CreateCampaignContactAsync(createDto, cancellationToken);
            return Ok(result);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("{campaignContactId}")]
    public async Task<ActionResult<CampaignContactDto>> GetCampaignContact(Guid campaignContactId, CancellationToken cancellationToken)
    {
        var result = await _campaignContactService.GetCampaignContactByIdAsync(campaignContactId, cancellationToken);
        
        if (result == null)
            return NotFound($"Campaign contact {campaignContactId} not found");

        return Ok(result);
    }

    [HttpGet("campaign/{campaignId}")]
    public async Task<ActionResult<IEnumerable<CampaignContactDto>>> GetCampaignContacts(Guid campaignId, CancellationToken cancellationToken)
    {
        var result = await _campaignContactService.GetCampaignContactsAsync(campaignId, cancellationToken);
        return Ok(result);
    }

    [HttpGet("campaign/{campaignId}/paged")]
    public async Task<ActionResult<PagedResult<CampaignContactDto>>> GetCampaignContactsPaginated(
        Guid campaignId,
        [FromQuery] int page = 1,
        [FromQuery] int pageSize = 50,
        [FromQuery] string? status = null,
        CancellationToken cancellationToken = default)
    {
        var result = await _campaignContactService.GetCampaignContactsPaginatedAsync(campaignId, page, pageSize, status, cancellationToken);
        return Ok(result);
    }

    [HttpPut("{campaignContactId}/status")]
    public async Task<ActionResult> UpdateCampaignContactStatus(
        Guid campaignContactId,
        [FromBody] UpdateCampaignContactStatusDto updateDto,
        CancellationToken cancellationToken)
    {
        var success = await _campaignContactService.UpdateCampaignContactStatusAsync(campaignContactId, updateDto.Status, cancellationToken);
        
        if (!success)
            return NotFound($"Campaign contact {campaignContactId} not found");

        return NoContent();
    }

    [HttpDelete("{campaignContactId}")]
    public async Task<ActionResult> DeleteCampaignContact(Guid campaignContactId, CancellationToken cancellationToken)
    {
        try
        {
            var success = await _campaignContactService.DeleteCampaignContactAsync(campaignContactId, cancellationToken);
            
            if (!success)
                return NotFound($"Campaign contact {campaignContactId} not found");

            return NoContent();
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPost("campaign/{campaignId}/import/csv")]
    public async Task<ActionResult<IEnumerable<CampaignContactDto>>> ImportContactsFromCsv(
        Guid campaignId,
        IFormFile csvFile,
        CancellationToken cancellationToken)
    {
        try
        {
            if (csvFile == null || csvFile.Length == 0)
                return BadRequest("CSV file is required");

            if (!csvFile.ContentType.Contains("csv") && !csvFile.ContentType.Contains("text"))
                return BadRequest("File must be a CSV file");

            using var stream = csvFile.OpenReadStream();
            var result = await _campaignContactService.ImportContactsFromCsvAsync(campaignId, stream, cancellationToken);
            
            return Ok(result);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPost("campaign/{campaignId}/import/customers")]
    public async Task<ActionResult<IEnumerable<CampaignContactDto>>> ImportContactsFromCustomers(
        Guid campaignId,
        [FromBody] ImportCustomersDto importDto,
        CancellationToken cancellationToken)
    {
        try
        {
            var result = await _campaignContactService.ImportContactsFromCustomersAsync(campaignId, importDto.CustomerIds, cancellationToken);
            return Ok(result);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpPost("campaign/{campaignId}/import/criteria")]
    public async Task<ActionResult<IEnumerable<CampaignContactDto>>> ImportContactsFromCriteria(
        Guid campaignId,
        [FromBody] CustomerSearchCriteriaDto criteria,
        CancellationToken cancellationToken)
    {
        try
        {
            var result = await _campaignContactService.ImportContactsFromCriteriaAsync(campaignId, criteria, cancellationToken);
            return Ok(result);
        }
        catch (ArgumentException ex)
        {
            return BadRequest(ex.Message);
        }
        catch (InvalidOperationException ex)
        {
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("campaign/{campaignId}/stats")]
    public async Task<ActionResult<CampaignContactStatsDto>> GetCampaignContactStats(Guid campaignId, CancellationToken cancellationToken)
    {
        var result = await _campaignContactService.GetCampaignContactStatsAsync(campaignId, cancellationToken);
        return Ok(result);
    }

    [HttpGet("campaign/{campaignId}/failed")]
    public async Task<ActionResult<IEnumerable<CampaignContactDto>>> GetFailedCampaignContacts(Guid campaignId, CancellationToken cancellationToken)
    {
        var result = await _campaignContactService.GetFailedCampaignContactsAsync(campaignId, cancellationToken);
        return Ok(result);
    }

    [HttpPost("{campaignContactId}/retry")]
    public async Task<ActionResult> RetryFailedContact(Guid campaignContactId, CancellationToken cancellationToken)
    {
        var success = await _campaignContactService.RetryFailedContactAsync(campaignContactId, cancellationToken);
        
        if (!success)
            return BadRequest("Failed to retry contact or contact is not in failed status");

        return Ok(new { message = "Contact retry initiated" });
    }

    [HttpPost("campaign/{campaignId}/retry-all")]
    public async Task<ActionResult> RetryAllFailedContacts(Guid campaignId, CancellationToken cancellationToken)
    {
        var successCount = await _campaignContactService.RetryAllFailedContactsAsync(campaignId, cancellationToken);
        return Ok(new { message = $"Retry initiated for {successCount} failed contacts" });
    }

    [HttpPut("{campaignContactId}/exclude")]
    public async Task<ActionResult> ExcludeContactFromCampaign(
        Guid campaignContactId,
        [FromBody] ExcludeContactDto excludeDto,
        CancellationToken cancellationToken)
    {
        var success = await _campaignContactService.ExcludeContactFromCampaignAsync(campaignContactId, excludeDto.Reason, cancellationToken);
        
        if (!success)
            return NotFound($"Campaign contact {campaignContactId} not found");

        return NoContent();
    }

    [HttpPut("{campaignContactId}/reinclude")]
    public async Task<ActionResult> ReincludeContactInCampaign(Guid campaignContactId, CancellationToken cancellationToken)
    {
        var success = await _campaignContactService.ReincludeContactInCampaignAsync(campaignContactId, cancellationToken);
        
        if (!success)
            return BadRequest("Contact not found or not in excluded status");

        return NoContent();
    }
}