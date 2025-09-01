using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class UserService : IUserService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<UserService> _logger;

    public UserService(RubiaDbContext context, ILogger<UserService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<UserDto> CreateAsync(CreateUserDto createDto)
    {
        _logger.LogInformation("Creating user with email: {Email}", createDto.Email);

        if (await _context.Users.AnyAsync(u => u.Email == createDto.Email && u.CompanyId == createDto.CompanyId))
        {
            throw new ArgumentException($"Usuário com email '{createDto.Email}' já existe nesta empresa");
        }

        var company = await _context.Companies.FirstOrDefaultAsync(c => c.Id == createDto.CompanyId);
        if (company == null)
        {
            throw new ArgumentException("Empresa não encontrada");
        }

        var department = await _context.Departments.FirstOrDefaultAsync(d => d.Id == createDto.DepartmentId);
        if (department == null)
        {
            throw new ArgumentException("Departamento não encontrado");
        }

        var user = new User
        {
            Name = createDto.Name,
            Email = createDto.Email,
            PasswordHash = BCrypt.Net.BCrypt.HashPassword(createDto.Password),
            CompanyId = createDto.CompanyId,
            DepartmentId = createDto.DepartmentId,
            Role = createDto.Role,
            AvatarUrl = createDto.AvatarUrl,
            BirthDate = createDto.BirthDate,
            Weight = createDto.Weight,
            Height = createDto.Height,
            Address = createDto.Address,
            IsOnline = false
        };

        _context.Users.Add(user);
        await _context.SaveChangesAsync();

        // Load related entities for DTO conversion
        await _context.Entry(user)
            .Reference(u => u.Department)
            .LoadAsync();

        _logger.LogInformation("User created successfully with id: {Id}", user.Id);
        return ToDto(user);
    }

    public async Task<UserDto> FindByIdAsync(Guid id)
    {
        _logger.LogDebug("Finding user by id: {Id}", id);

        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Id == id);

        if (user == null)
        {
            throw new ArgumentException("Usuário não encontrado");
        }

        return ToDto(user);
    }

    public async Task<UserDto> FindByEmailAndCompanyAsync(string email, Guid companyId)
    {
        _logger.LogDebug("Finding user by email: {Email} for company: {CompanyId}", email, companyId);

        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Email == email && u.CompanyId == companyId);

        if (user == null)
        {
            throw new ArgumentException("Usuário não encontrado nesta empresa");
        }

        return ToDto(user);
    }

    public async Task<List<UserDto>> FindAllByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Finding all users for company: {CompanyId}", companyId);

        var users = await _context.Users
            .Include(u => u.Department)
            .Where(u => u.CompanyId == companyId)
            .OrderBy(u => u.Name)
            .ToListAsync();

        return users.Select(ToDto).ToList();
    }

    public async Task<List<UserDto>> FindByDepartmentAndCompanyAsync(Guid departmentId, Guid companyId)
    {
        _logger.LogDebug("Finding users by department: {DepartmentId} for company: {CompanyId}", 
            departmentId, companyId);

        var users = await _context.Users
            .Include(u => u.Department)
            .Where(u => u.DepartmentId == departmentId && u.CompanyId == companyId)
            .OrderBy(u => u.Name)
            .ToListAsync();

        return users.Select(ToDto).ToList();
    }

    public async Task<List<UserDto>> FindAvailableAgentsByCompanyAsync(Guid companyId)
    {
        _logger.LogDebug("Finding available agents for company: {CompanyId}", companyId);

        var users = await _context.Users
            .Include(u => u.Department)
            .Where(u => u.CompanyId == companyId && 
                       (u.Role == UserRole.Agent || u.Role == UserRole.Supervisor || u.Role == UserRole.Admin))
            .OrderBy(u => u.Name)
            .ToListAsync();

        return users.Select(ToDto).ToList();
    }

    public async Task<List<UserDto>> FindAvailableAgentsByDepartmentAndCompanyAsync(Guid departmentId, Guid companyId)
    {
        _logger.LogDebug("Finding available agents by department: {DepartmentId} for company: {CompanyId}", 
            departmentId, companyId);

        var users = await _context.Users
            .Include(u => u.Department)
            .Where(u => u.DepartmentId == departmentId && 
                       u.CompanyId == companyId && 
                       (u.Role == UserRole.Agent || u.Role == UserRole.Supervisor || u.Role == UserRole.Admin))
            .OrderBy(u => u.Name)
            .ToListAsync();

        return users.Select(ToDto).ToList();
    }

    public async Task<UserDto> UpdateAsync(Guid id, UpdateUserDto updateDto)
    {
        _logger.LogInformation("Updating user with id: {Id}", id);

        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Id == id);

        if (user == null)
        {
            throw new ArgumentException("Usuário não encontrado");
        }

        if (updateDto.Name != null) user.Name = updateDto.Name;

        if (updateDto.Email != null)
        {
            if (updateDto.Email != user.Email && 
                await _context.Users.AnyAsync(u => u.Email == updateDto.Email && u.CompanyId == user.CompanyId))
            {
                throw new ArgumentException($"Usuário com email '{updateDto.Email}' já existe nesta empresa");
            }
            user.Email = updateDto.Email;
        }

        if (updateDto.Password != null)
        {
            user.PasswordHash = BCrypt.Net.BCrypt.HashPassword(updateDto.Password);
        }

        if (updateDto.DepartmentId.HasValue)
        {
            var department = await _context.Departments.FirstOrDefaultAsync(d => d.Id == updateDto.DepartmentId);
            if (department == null)
            {
                throw new ArgumentException("Departamento não encontrado");
            }
            user.DepartmentId = updateDto.DepartmentId.Value;
        }

        if (updateDto.Role.HasValue) user.Role = updateDto.Role.Value;
        if (updateDto.AvatarUrl != null) user.AvatarUrl = updateDto.AvatarUrl;
        if (updateDto.BirthDate.HasValue) user.BirthDate = updateDto.BirthDate;
        if (updateDto.Weight.HasValue) user.Weight = updateDto.Weight;
        if (updateDto.Height.HasValue) user.Height = updateDto.Height;
        if (updateDto.Address != null) user.Address = updateDto.Address;

        await _context.SaveChangesAsync();
        
        // Reload department if changed
        if (updateDto.DepartmentId.HasValue)
        {
            await _context.Entry(user)
                .Reference(u => u.Department)
                .LoadAsync();
        }

        _logger.LogInformation("User updated successfully");
        return ToDto(user);
    }

    public async Task<UserDto> UpdateOnlineStatusAsync(Guid id, bool isOnline)
    {
        _logger.LogInformation("Updating online status for user: {Id} to {IsOnline}", id, isOnline);

        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Id == id);

        if (user == null)
        {
            throw new ArgumentException("Usuário não encontrado");
        }

        user.IsOnline = isOnline;
        if (!isOnline)
        {
            user.LastSeen = DateTime.UtcNow;
        }

        await _context.SaveChangesAsync();
        _logger.LogInformation("User online status updated successfully");

        return ToDto(user);
    }

    public async Task<UserDto> AssignToDepartmentAsync(Guid userId, Guid departmentId)
    {
        _logger.LogInformation("Assigning user {UserId} to department {DepartmentId}", userId, departmentId);

        var user = await _context.Users
            .Include(u => u.Department)
            .FirstOrDefaultAsync(u => u.Id == userId);

        if (user == null)
        {
            throw new ArgumentException("Usuário não encontrado");
        }

        var department = await _context.Departments.FirstOrDefaultAsync(d => d.Id == departmentId);
        if (department == null)
        {
            throw new ArgumentException("Departamento não encontrado");
        }

        user.DepartmentId = departmentId;
        await _context.SaveChangesAsync();

        // Reload department
        await _context.Entry(user)
            .Reference(u => u.Department)
            .LoadAsync();

        _logger.LogInformation("User assigned to department successfully");
        return ToDto(user);
    }

    public async Task<bool> ValidateLoginByCompanyAsync(UserLoginDto loginDto, Guid companyId)
    {
        _logger.LogDebug("Validating login for email: {Email} in company: {CompanyId}", loginDto.Email, companyId);

        var user = await _context.Users
            .FirstOrDefaultAsync(u => u.Email == loginDto.Email && u.CompanyId == companyId);

        if (user == null)
        {
            return false;
        }

        return BCrypt.Net.BCrypt.Verify(loginDto.Password, user.PasswordHash);
    }

    public async Task DeleteAsync(Guid id)
    {
        _logger.LogInformation("Deleting user with id: {Id}", id);

        var user = await _context.Users.FirstOrDefaultAsync(u => u.Id == id);
        if (user == null)
        {
            throw new ArgumentException("Usuário não encontrado");
        }

        _context.Users.Remove(user);
        await _context.SaveChangesAsync();

        _logger.LogInformation("User deleted successfully");
    }

    public async Task DeleteAllByCompanyAsync(Guid companyId)
    {
        _logger.LogInformation("Deleting all users for company: {CompanyId}", companyId);

        var users = await _context.Users.Where(u => u.CompanyId == companyId).ToListAsync();
        _context.Users.RemoveRange(users);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Deleted {Count} users for company: {CompanyId}", users.Count, companyId);
    }

    private static UserDto ToDto(User user)
    {
        return new UserDto
        {
            Id = user.Id,
            Name = user.Name,
            Email = user.Email,
            CompanyId = user.CompanyId,
            DepartmentId = user.DepartmentId,
            DepartmentName = user.Department?.Name,
            Role = user.Role,
            AvatarUrl = user.AvatarUrl,
            IsOnline = user.IsOnline,
            LastSeen = user.LastSeen,
            BirthDate = user.BirthDate,
            Weight = user.Weight,
            Height = user.Height,
            Address = user.Address,
            CreatedAt = user.CreatedAt,
            UpdatedAt = user.UpdatedAt
        };
    }
}