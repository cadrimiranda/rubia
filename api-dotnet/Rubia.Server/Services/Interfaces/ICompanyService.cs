using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface ICompanyService
{
    Task<List<CompanyDto>> FindAllAsync();
    Task<List<CompanyDto>> FindActiveCompaniesAsync();
    Task<CompanyDto?> FindByIdAsync(Guid id);
    Task<CompanyDto?> FindBySlugAsync(string slug);
    Task<CompanyDto> CreateAsync(CreateCompanyDto createDto);
    Task<CompanyDto> UpdateAsync(Guid id, UpdateCompanyDto updateDto);
    Task DeleteByIdAsync(Guid id);
    Task<bool> ExistsBySlugAsync(string slug);
    Task<List<CompanyDto>> FindByPlanTypeAsync(CompanyPlanType planType);
}