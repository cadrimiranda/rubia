using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Services.Interfaces;
using System.Text.Json;

namespace Rubia.Server.Services;

public class WhatsAppService : IWhatsAppService
{
    private readonly RubiaDbContext _context;
    private readonly HttpClient _httpClient;
    private readonly IConfiguration _configuration;
    private readonly ILogger<WhatsAppService> _logger;

    public WhatsAppService(
        RubiaDbContext context,
        HttpClient httpClient,
        IConfiguration configuration,
        ILogger<WhatsAppService> logger)
    {
        _context = context;
        _httpClient = httpClient;
        _configuration = configuration;
        _logger = logger;
    }

    public async Task<WhatsAppInstance?> GetActiveInstanceAsync(Guid companyId)
    {
        return await _context.WhatsAppInstances
            .FirstOrDefaultAsync(w => w.CompanyId == companyId 
                                    && w.IsActive 
                                    && w.IsPrimary);
    }

    public async Task<bool> SendTextMessageAsync(string phoneNumber, string message, Guid companyId)
    {
        var instance = await GetActiveInstanceAsync(companyId);
        if (instance == null)
        {
            _logger.LogError("No active WhatsApp instance found for company {CompanyId}", companyId);
            return false;
        }

        try
        {
            var url = $"https://api.z-api.io/instances/{instance.InstanceId}/token/{instance.Token}/send-text";
            
            var payload = new
            {
                phone = phoneNumber,
                message = message
            };

            var jsonContent = JsonSerializer.Serialize(payload);
            var content = new StringContent(jsonContent, System.Text.Encoding.UTF8, "application/json");

            var response = await _httpClient.PostAsync(url, content);
            
            if (response.IsSuccessStatusCode)
            {
                _logger.LogDebug("Message sent successfully to {PhoneNumber} via instance {InstanceId}", 
                    phoneNumber, instance.InstanceId);
                return true;
            }

            var errorContent = await response.Content.ReadAsStringAsync();
            _logger.LogError("Failed to send message via Z-API. Status: {StatusCode}, Content: {Content}", 
                response.StatusCode, errorContent);
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending WhatsApp message to {PhoneNumber}", phoneNumber);
            return false;
        }
    }

    public async Task<ZApiStatusDto> GetInstanceStatusAsync(string instanceId, string token)
    {
        try
        {
            var url = $"https://api.z-api.io/instances/{instanceId}/token/{token}/status";
            var response = await _httpClient.GetAsync(url);

            if (response.IsSuccessStatusCode)
            {
                var jsonContent = await response.Content.ReadAsStringAsync();
                var statusResponse = JsonSerializer.Deserialize<ZApiStatusResponse>(jsonContent);
                
                return new ZApiStatusDto
                {
                    Connected = statusResponse?.Connected ?? false,
                    Session = statusResponse?.Session ?? "UNKNOWN",
                    Smartphone = statusResponse?.Smartphone ?? false
                };
            }

            _logger.LogWarning("Failed to get Z-API status. Status: {StatusCode}", response.StatusCode);
            return new ZApiStatusDto { Connected = false, Session = "ERROR" };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting Z-API instance status for {InstanceId}", instanceId);
            return new ZApiStatusDto { Connected = false, Session = "ERROR" };
        }
    }

    public async Task<string?> GenerateQRCodeAsync(string instanceId, string token)
    {
        try
        {
            var url = $"https://api.z-api.io/instances/{instanceId}/token/{token}/qr-code";
            var response = await _httpClient.GetAsync(url);

            if (response.IsSuccessStatusCode)
            {
                var jsonContent = await response.Content.ReadAsStringAsync();
                var qrResponse = JsonSerializer.Deserialize<ZApiQRResponse>(jsonContent);
                return qrResponse?.Value;
            }

            _logger.LogWarning("Failed to generate QR code. Status: {StatusCode}", response.StatusCode);
            return null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error generating QR code for instance {InstanceId}", instanceId);
            return null;
        }
    }

    public async Task<bool> RestartInstanceAsync(string instanceId, string token)
    {
        try
        {
            var url = $"https://api.z-api.io/instances/{instanceId}/token/{token}/restart";
            var response = await _httpClient.PostAsync(url, null);

            if (response.IsSuccessStatusCode)
            {
                _logger.LogInformation("Instance {InstanceId} restarted successfully", instanceId);
                return true;
            }

            _logger.LogWarning("Failed to restart instance {InstanceId}. Status: {StatusCode}", 
                instanceId, response.StatusCode);
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error restarting instance {InstanceId}", instanceId);
            return false;
        }
    }
}

// DTOs for Z-API responses
public class ZApiStatusResponse
{
    public bool Connected { get; set; }
    public string Session { get; set; } = string.Empty;
    public bool Smartphone { get; set; }
}

public class ZApiQRResponse
{
    public string Value { get; set; } = string.Empty;
}