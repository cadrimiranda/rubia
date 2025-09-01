using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface ICompanyGroupService
{
    Task<CompanyGroupDto> CreateAsync(CreateCompanyGroupDto createDto);
    Task<List<CompanyGroupDto>> FindAllAsync();
    Task<CompanyGroupDto> FindByIdAsync(Guid id);
    Task<CompanyGroupDto> UpdateAsync(Guid id, UpdateCompanyGroupDto updateDto);
    Task DeleteAsync(Guid id);
}