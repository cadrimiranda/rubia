using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface ICustomerService
{
    Task<CustomerDto> CreateAsync(CreateCustomerDto createDto, Guid companyId);
    Task<CustomerDto> FindByIdAsync(Guid id, Guid companyId);
    Task<CustomerDto> UpdateAsync(Guid id, UpdateCustomerDto updateDto, Guid companyId);
    Task<CustomerDto> BlockCustomerAsync(Guid id, Guid companyId);
    Task<CustomerDto> UnblockCustomerAsync(Guid id, Guid companyId);
    Task DeleteAsync(Guid id, Guid companyId);
    
    // Company-scoped methods
    Task<List<CustomerDto>> FindAllByCompanyAsync(Guid companyId);
    Task<CustomerDto?> FindByPhoneAndCompanyAsync(string phone, Guid companyId);
    Task<List<CustomerDto>> FindActiveByCompanyAsync(Guid companyId);
    Task<List<CustomerDto>> SearchByNameOrPhoneAndCompanyAsync(string searchTerm, Guid companyId);
    Task<long> CountActiveByCompanyAsync(Guid companyId);
    Task DeleteAllByCompanyAsync(Guid companyId);
}