using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/users")]
public class UsersController : ControllerBase
{
    private readonly IUserService _userService;
    private readonly ILogger<UsersController> _logger;

    public UsersController(IUserService userService, ILogger<UsersController> logger)
    {
        _userService = userService;
        _logger = logger;
    }

    [HttpPost]
    public async Task<ActionResult<UserDto>> Create([FromBody] CreateUserDto createDto)
    {
        _logger.LogInformation("Creating user: {Email}", createDto.Email);
        
        try
        {
            var created = await _userService.CreateAsync(createDto);
            return CreatedAtAction(nameof(FindById), new { id = created.Id }, created);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error creating user: {Message}", ex.Message);
            return BadRequest(ex.Message);
        }
    }

    [HttpGet("{id:guid}")]
    public async Task<ActionResult<UserDto>> FindById(Guid id)
    {
        _logger.LogDebug("Finding user by id: {Id}", id);
        
        try
        {
            var user = await _userService.FindByIdAsync(id);
            return Ok(user);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("User not found: {Id}", id);
            return NotFound();
        }
    }

    [HttpGet("email/{email}")]
    public async Task<ActionResult<UserDto>> FindByEmail(string email, [FromQuery] Guid companyId)
    {
        _logger.LogDebug("Finding user by email: {Email}", email);
        
        try
        {
            var user = await _userService.FindByEmailAndCompanyAsync(email, companyId);
            return Ok(user);
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("User not found: {Email}", email);
            return NotFound();
        }
    }

    [HttpGet]
    public async Task<ActionResult<List<UserDto>>> FindAll([FromQuery] Guid companyId, [FromQuery] Guid? departmentId = null)
    {
        _logger.LogDebug("Finding users, departmentId: {DepartmentId}", departmentId);
        
        var users = departmentId.HasValue 
            ? await _userService.FindByDepartmentAndCompanyAsync(departmentId.Value, companyId)
            : await _userService.FindAllByCompanyAsync(companyId);
            
        return Ok(users);
    }

    [HttpGet("available-agents")]
    public async Task<ActionResult<List<UserDto>>> FindAvailableAgents([FromQuery] Guid companyId, [FromQuery] Guid? departmentId = null)
    {
        _logger.LogDebug("Finding available agents, departmentId: {DepartmentId}", departmentId);
        
        var agents = departmentId.HasValue
            ? await _userService.FindAvailableAgentsByDepartmentAndCompanyAsync(departmentId.Value, companyId)
            : await _userService.FindAvailableAgentsByCompanyAsync(companyId);
            
        return Ok(agents);
    }

    [HttpPut("{id:guid}")]
    public async Task<ActionResult<UserDto>> Update(Guid id, [FromBody] UpdateUserDto updateDto)
    {
        _logger.LogInformation("Updating user: {Id}", id);
        
        try
        {
            var updated = await _userService.UpdateAsync(id, updateDto);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating user: {Message}", ex.Message);
            if (ex.Message.Contains("não encontrado"))
            {
                return NotFound();
            }
            return BadRequest(ex.Message);
        }
    }

    [HttpPut("{id:guid}/online-status")]
    public async Task<ActionResult<UserDto>> UpdateOnlineStatus(Guid id, [FromQuery] bool isOnline)
    {
        _logger.LogInformation("Updating online status for user: {Id} to {IsOnline}", id, isOnline);
        
        try
        {
            var updated = await _userService.UpdateOnlineStatusAsync(id, isOnline);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error updating online status: {Message}", ex.Message);
            return NotFound();
        }
    }

    [HttpPut("{userId:guid}/assign-department/{departmentId:guid}")]
    public async Task<ActionResult<UserDto>> AssignToDepartment(Guid userId, Guid departmentId)
    {
        _logger.LogInformation("Assigning user {UserId} to department {DepartmentId}", userId, departmentId);
        
        try
        {
            var updated = await _userService.AssignToDepartmentAsync(userId, departmentId);
            return Ok(updated);
        }
        catch (ArgumentException ex)
        {
            _logger.LogWarning("Error assigning user to department: {Message}", ex.Message);
            return NotFound();
        }
    }

    [HttpPost("login")]
    public async Task<ActionResult<bool>> Login([FromBody] UserLoginDto loginDto, [FromQuery] Guid companyId)
    {
        _logger.LogInformation("Login attempt for email: {Email}", loginDto.Email);
        
        var isValid = await _userService.ValidateLoginByCompanyAsync(loginDto, companyId);
        
        if (isValid)
        {
            _logger.LogInformation("Login successful for email: {Email}", loginDto.Email);
            return Ok(true);
        }
        else
        {
            _logger.LogWarning("Login failed for email: {Email}", loginDto.Email);
            return Unauthorized(false);
        }
    }

    [HttpDelete("{id:guid}")]
    public async Task<IActionResult> Delete(Guid id)
    {
        _logger.LogInformation("Deleting user: {Id}", id);
        
        try
        {
            await _userService.DeleteAsync(id);
            return NoContent();
        }
        catch (ArgumentException)
        {
            _logger.LogWarning("Error deleting user: {Id}", id);
            return NotFound();
        }
    }
}