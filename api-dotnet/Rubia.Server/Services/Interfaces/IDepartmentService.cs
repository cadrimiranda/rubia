using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IDepartmentService
{
    Task<DepartmentDto> CreateAsync(CreateDepartmentDto createDto, Guid companyId);
    Task<DepartmentDto> FindByIdAsync(Guid id, Guid companyId);
    Task<List<DepartmentDto>> FindAllAsync(Guid companyId);
    Task<List<DepartmentDto>> FindByAutoAssignAsync(Guid companyId);
    Task<DepartmentDto> UpdateAsync(Guid id, UpdateDepartmentDto updateDto, Guid companyId);
    Task DeleteAsync(Guid id, Guid companyId);
}