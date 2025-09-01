using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/customers")]
public class CustomersController : ControllerBase
{
    private readonly ICustomerService _customerService;
    private readonly IPhoneService _phoneService;
    private readonly ILogger<CustomersController> _logger;

    public CustomersController(ICustomerService customerService, IPhoneService phoneService, ILogger<CustomersController> logger)
    {
        _customerService = customerService;
        _phoneService = phoneService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<CustomerDto>> Create([FromBody] CreateCustomerDto createDto, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Creating customer: {Phone}", createDto.Phone);
        
        try
        {
            var created = await _customerService.CreateAsync(createDto, companyId);
            return CreatedAtAction(nameof(FindById), new { id = created.Id, companyId }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating customer: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<CustomerDto>> FindById(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogDebug("Finding customer by id: {Id}", id);
        
        try
        {
            var customer = await _customerService.FindByIdAsync(id, companyId);
            return Ok(customer);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Customer not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("phone/{phone}")]
    public async Task<ActionResult<CustomerDto>> FindByPhone(string phone, [FromQuery] Guid companyId)
    {
        _logger.LogDebug("Finding customer by phone: {Phone}", phone);
        
        try
        {
            var normalizedPhone = _phoneService.Normalize(phone);
            var customer = await _customerService.FindByPhoneAndCompanyAsync(normalizedPhone, companyId);
            
            if (customer == null)
            {
                return NotFound();
            }
            
            return Ok(customer);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Customer not found: {Phone}", phone);
            return NotFound();
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<CustomerDto>>> FindAll(
        [FromQuery] Guid companyId,
        [FromQuery] string? search = null,
        [FromQuery] bool includeBlocked = false)
    {
        _logger.LogDebug("Finding customers, search: {Search}, includeBlocked: {IncludeBlocked}", search, includeBlocked);
        
        List<CustomerDto> customers;
        
        if (!string.IsNullOrWhiteSpace(search))
        {
            customers = await _customerService.SearchByNameOrPhoneAndCompanyAsync(search, companyId);
        }
        else if (!includeBlocked)
        {
            customers = await _customerService.FindActiveByCompanyAsync(companyId);
        }
        else
        {
            customers = await _customerService.FindAllByCompanyAsync(companyId);
        }
        
        return Ok(customers);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<CustomerDto>> Update(Guid id, [FromBody] UpdateCustomerDto updateDto, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Updating customer: {Id}", id);
        
        try
        {
            var updated = await _customerService.UpdateAsync(id, updateDto, companyId);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating customer: {Message}", ex.Message);
            if (ex.Message.Contains("não encontrado"))
            {
                return NotFound();
            }
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("{id:guid}/block")]
    public async Task<ActionResult<CustomerDto>> BlockCustomer(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Blocking customer: {Id}", id);
        
        try
        {
            var blocked = await _customerService.BlockCustomerAsync(id, companyId);
            return Ok(blocked);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error blocking customer: {Message}", ex.Message);
            return NotFound();
        }
    }

    [HttpPut("{id:guid}/unblock")]
    public async Task<ActionResult<CustomerDto>> UnblockCustomer(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Unblocking customer: {Id}", id);
        
        try
        {
            var unblocked = await _customerService.UnblockCustomerAsync(id, companyId);
            return Ok(unblocked);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error unblocking customer: {Message}", ex.Message);
            return NotFound();
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Deleting customer: {Id}", id);
        
        try
        {
            await _customerService.DeleteAsync(id, companyId);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting customer: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("count")]
    public async Task<ActionResult<long>> CountActive([FromQuery] Guid companyId)
    {
        var count = await _customerService.CountActiveByCompanyAsync(companyId);
        return Ok(count);
    }

    [HttpDelete("company/{companyId:guid}")]
    public async Task<IActionResult> DeleteAllByCompany(Guid companyId)
    {
        _logger.LogInformation("Deleting all customers for company: {CompanyId}", companyId);
        
        await _customerService.DeleteAllByCompanyAsync(companyId);
        return NoContent();
    }
}