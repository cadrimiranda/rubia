using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IUserService
{
    Task<UserDto> CreateAsync(CreateUserDto createDto);
    Task<UserDto> FindByIdAsync(Guid id);
    Task<UserDto> FindByEmailAndCompanyAsync(string email, Guid companyId);
    Task<List<UserDto>> FindAllByCompanyAsync(Guid companyId);
    Task<List<UserDto>> FindByDepartmentAndCompanyAsync(Guid departmentId, Guid companyId);
    Task<List<UserDto>> FindAvailableAgentsByCompanyAsync(Guid companyId);
    Task<List<UserDto>> FindAvailableAgentsByDepartmentAndCompanyAsync(Guid departmentId, Guid companyId);
    Task<UserDto> UpdateAsync(Guid id, UpdateUserDto updateDto);
    Task<UserDto> UpdateOnlineStatusAsync(Guid id, bool isOnline);
    Task<UserDto> AssignToDepartmentAsync(Guid userId, Guid departmentId);
    Task<bool> ValidateLoginByCompanyAsync(UserLoginDto loginDto, Guid companyId);
    Task DeleteAsync(Guid id);
    Task DeleteAllByCompanyAsync(Guid companyId);
}