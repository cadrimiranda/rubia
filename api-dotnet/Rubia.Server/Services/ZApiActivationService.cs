using Microsoft.Extensions.Configuration;
using Rubia.Server.Entities;
using System.Text.Json;

namespace Rubia.Server.Services;

public class ZApiActivationService
{
    private readonly HttpClient _httpClient;
    private readonly WhatsAppInstanceService _whatsAppInstanceService;
    private readonly CompanyContextService _companyContextService;
    private readonly IConfiguration _configuration;
    private readonly ILogger<ZApiActivationService> _logger;

    public ZApiActivationService(
        HttpClient httpClient,
        WhatsAppInstanceService whatsAppInstanceService,
        CompanyContextService companyContextService,
        IConfiguration configuration,
        ILogger<ZApiActivationService> logger)
    {
        _httpClient = httpClient;
        _whatsAppInstanceService = whatsAppInstanceService;
        _companyContextService = companyContextService;
        _configuration = configuration;
        _logger = logger;
    }

    private async Task<WhatsAppInstance> GetActiveInstanceAsync()
    {
        try
        {
            var currentCompany = _companyContextService.GetCurrentCompany();
            
            // First try to find a connected instance
            var connectedInstance = await _whatsAppInstanceService.FindActiveConnectedInstanceAsync(currentCompany.Id);
            if (connectedInstance != null)
            {
                return connectedInstance;
            }

            // If no connected instance, look for instances that are configured
            var instances = await _whatsAppInstanceService.FindByCompanyAsync(currentCompany.Id);
            var configuredInstance = instances.FirstOrDefault(instance => 
                !string.IsNullOrEmpty(instance.InstanceId) && 
                !string.IsNullOrEmpty(instance.AccessToken) &&
                instance.IsConfigured());

            if (configuredInstance == null)
            {
                throw new InvalidOperationException("No configured WhatsApp instance found for activation");
            }

            return configuredInstance;
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error getting active instance: {Message}", e.Message);
            throw new InvalidOperationException("Cannot determine active WhatsApp instance", e);
        }
    }

    private async Task<string> BuildZApiUrlAsync(string endpoint)
    {
        var instance = await GetActiveInstanceAsync();
        ValidateInstanceConfiguration(instance);
        return $"https://api.z-api.io/instances/{instance.InstanceId}/{endpoint}";
    }

    private static void ValidateInstanceConfiguration(WhatsAppInstance instance)
    {
        if (string.IsNullOrEmpty(instance.InstanceId))
        {
            throw new InvalidOperationException("Instance ID is required for Z-API activation");
        }
        
        if (string.IsNullOrEmpty(instance.AccessToken))
        {
            throw new InvalidOperationException("Access Token is required for Z-API activation");
        }
    }

    public async Task<ZApiStatus> GetInstanceStatusAsync()
    {
        try
        {
            var url = await BuildZApiUrlAsync("status");
            var request = new HttpRequestMessage(HttpMethod.Get, url);
            AddHeaders(request);

            var response = await _httpClient.SendAsync(request);

            if (response.IsSuccessStatusCode)
            {
                var content = await response.Content.ReadAsStringAsync();
                var responseData = JsonSerializer.Deserialize<Dictionary<string, object>>(content);
                return ParseStatusResponse(responseData ?? new Dictionary<string, object>());
            }
            
            return ZApiStatus.CreateError("Failed to get status");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error getting Z-API status: {Message}", e.Message);
            return ZApiStatus.CreateError($"Error: {e.Message}");
        }
    }

    public async Task<QrCodeResult> GetQrCodeBytesAsync()
    {
        try
        {
            var url = await BuildZApiUrlAsync("qr-code");
            var request = new HttpRequestMessage(HttpMethod.Get, url);
            AddHeaders(request);

            var response = await _httpClient.SendAsync(request);

            if (response.IsSuccessStatusCode)
            {
                var bytes = await response.Content.ReadAsByteArrayAsync();
                return QrCodeResult.CreateSuccess(bytes, "bytes");
            }
            
            return QrCodeResult.CreateError("Failed to get QR code bytes");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error getting QR code bytes: {Message}", e.Message);
            return QrCodeResult.CreateError($"Error: {e.Message}");
        }
    }

    public async Task<QrCodeResult> GetQrCodeImageAsync()
    {
        try
        {
            var url = await BuildZApiUrlAsync("qr-code/image");
            _logger.LogInformation("Getting QR code from URL: {Url}", url);

            var request = new HttpRequestMessage(HttpMethod.Get, url);
            AddHeaders(request);

            var response = await _httpClient.SendAsync(request);
            var content = await response.Content.ReadAsStringAsync();
            _logger.LogInformation("Z-API QR Code response: status={StatusCode}, body={Body}", 
                response.StatusCode, content);

            if (response.IsSuccessStatusCode && !string.IsNullOrEmpty(content))
            {
                var responseData = JsonSerializer.Deserialize<Dictionary<string, object>>(content);
                if (responseData?.TryGetValue("value", out var valueObj) == true)
                {
                    var fullValue = valueObj?.ToString();
                    _logger.LogInformation("Full value received: {Value}", 
                        fullValue?.Length > 100 ? $"{fullValue[..100]}..." : fullValue);

                    // Extract base64 part from data URL format (data:image/png;base64,XXXX)
                    string? base64Image = null;
                    if (!string.IsNullOrEmpty(fullValue) && fullValue.StartsWith("data:image/png;base64,"))
                    {
                        base64Image = fullValue["data:image/png;base64,".Length..];
                    }

                    _logger.LogInformation("Base64 image extracted: {Status}", 
                        base64Image != null ? $"present ({base64Image.Length} chars)" : "null");
                    return QrCodeResult.CreateSuccess(base64Image, "base64");
                }
            }

            _logger.LogWarning("Failed to get QR code: status={StatusCode}, body={Body}", 
                response.StatusCode, content);
            return QrCodeResult.CreateError("Failed to get QR code image");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error getting QR code image: {Message}", e.Message);
            return QrCodeResult.CreateError($"Error: {e.Message}");
        }
    }

    public async Task<PhoneCodeResult> GetPhoneCodeAsync(string phoneNumber)
    {
        try
        {
            var url = await BuildZApiUrlAsync($"phone-code/{phoneNumber}");
            var request = new HttpRequestMessage(HttpMethod.Get, url);
            AddHeaders(request);

            var response = await _httpClient.SendAsync(request);

            if (response.IsSuccessStatusCode)
            {
                var content = await response.Content.ReadAsStringAsync();
                var responseData = JsonSerializer.Deserialize<Dictionary<string, object>>(content);
                
                if (responseData?.TryGetValue("code", out var codeObj) == true)
                {
                    var code = codeObj?.ToString();
                    return PhoneCodeResult.CreateSuccess(code ?? string.Empty, phoneNumber);
                }
            }
            
            return PhoneCodeResult.CreateError("Failed to get phone code");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error getting phone code: {Message}", e.Message);
            return PhoneCodeResult.CreateError($"Error: {e.Message}");
        }
    }

    public async Task<bool> RestartInstanceAsync()
    {
        try
        {
            var url = await BuildZApiUrlAsync("restart");
            var request = new HttpRequestMessage(HttpMethod.Get, url);
            AddHeaders(request);

            var response = await _httpClient.SendAsync(request);
            return response.IsSuccessStatusCode;
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error restarting instance: {Message}", e.Message);
            return false;
        }
    }

    public async Task<bool> DisconnectInstanceAsync()
    {
        try
        {
            var url = await BuildZApiUrlAsync("disconnect");
            var request = new HttpRequestMessage(HttpMethod.Get, url);
            AddHeaders(request);

            var response = await _httpClient.SendAsync(request);
            return response.IsSuccessStatusCode;
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error disconnecting instance: {Message}", e.Message);
            return false;
        }
    }

    private void AddHeaders(HttpRequestMessage request)
    {
        var clientToken = _configuration["ZApi:ClientToken"];
        request.Headers.Add("client-token", clientToken);
        request.Headers.Add("Accept", "application/json");
    }

    private static ZApiStatus ParseStatusResponse(Dictionary<string, object> response)
    {
        var connected = ParseBoolean(response.GetValueOrDefault("connected"));
        var smartphoneConnected = ParseBoolean(response.GetValueOrDefault("smartphoneConnected"));
        var session = ParseString(response.GetValueOrDefault("session"));

        return new ZApiStatus
        {
            Connected = connected,
            Session = session,
            SmartphoneConnected = smartphoneConnected,
            NeedsQrCode = !connected,
            RawResponse = response
        };
    }

    private static bool ParseBoolean(object? value)
    {
        return value switch
        {
            bool boolValue => boolValue,
            string stringValue => string.Equals(stringValue, "true", StringComparison.OrdinalIgnoreCase),
            _ => false
        };
    }

    private static string? ParseString(object? value)
    {
        return value?.ToString();
    }
}