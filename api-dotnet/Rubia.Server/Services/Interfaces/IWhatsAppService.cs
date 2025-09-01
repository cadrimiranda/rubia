using Rubia.Server.DTOs;
using Rubia.Server.Entities;

namespace Rubia.Server.Services.Interfaces;

public interface IWhatsAppService
{
    Task<WhatsAppInstance?> GetActiveInstanceAsync(Guid companyId);
    Task<bool> SendTextMessageAsync(string phoneNumber, string message, Guid companyId);
    Task<ZApiStatusDto> GetInstanceStatusAsync(string instanceId, string token);
    Task<string?> GenerateQRCodeAsync(string instanceId, string token);
    Task<bool> RestartInstanceAsync(string instanceId, string token);
}