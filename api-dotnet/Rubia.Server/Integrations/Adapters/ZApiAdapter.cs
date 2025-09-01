using Microsoft.Extensions.Options;
using Rubia.Server.DTOs;
using Rubia.Server.Enums;
using Rubia.Server.Integrations.Adapters;
using System.Text;
using System.Text.Json;

namespace Rubia.Server.Integrations.Adapters;

public class ZApiAdapter : IMessagingAdapter
{
    private readonly HttpClient _httpClient;
    private readonly IConfiguration _configuration;
    private readonly ILogger<ZApiAdapter> _logger;
    private readonly string _baseUrl;
    private readonly string _token;
    private readonly string _instanceId;

    public string ProviderName => "Z-API";

    public ZApiAdapter(
        HttpClient httpClient,
        IConfiguration configuration,
        ILogger<ZApiAdapter> logger)
    {
        _httpClient = httpClient;
        _configuration = configuration;
        _logger = logger;
        
        _baseUrl = _configuration["ZApi:BaseUrl"] ?? throw new InvalidOperationException("Z-API BaseUrl not configured");
        _token = _configuration["ZApi:Token"] ?? throw new InvalidOperationException("Z-API Token not configured");
        _instanceId = _configuration["ZApi:InstanceId"] ?? throw new InvalidOperationException("Z-API InstanceId not configured");

        _httpClient.DefaultRequestHeaders.Authorization = 
            new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _token);
    }

    public async Task<bool> IsAvailableAsync(CancellationToken cancellationToken = default)
    {
        try
        {
            var status = await GetConnectionStatusAsync(_instanceId, cancellationToken);
            return status == ConnectionStatus.Connected;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking Z-API availability");
            return false;
        }
    }

    public async Task<MessagingResult> SendTextMessageAsync(string recipientId, string content, CancellationToken cancellationToken = default)
    {
        try
        {
            var payload = new
            {
                phone = recipientId.Replace("+", "").Replace("-", "").Replace(" ", ""),
                message = content
            };

            var json = JsonSerializer.Serialize(payload);
            var httpContent = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await _httpClient.PostAsync(
                $"{_baseUrl}/instances/{_instanceId}/token/{_token}/send-text", 
                httpContent, 
                cancellationToken);

            var responseContent = await response.Content.ReadAsStringAsync(cancellationToken);

            if (response.IsSuccessStatusCode)
            {
                var result = JsonSerializer.Deserialize<ZApiResponse>(responseContent);
                
                _logger.LogInformation("Z-API message sent successfully to {Recipient}: {MessageId}", 
                    recipientId, result?.MessageId);

                return new MessagingResult
                {
                    Success = true,
                    ExternalMessageId = result?.MessageId,
                    Metadata = new Dictionary<string, object>
                    {
                        ["response"] = responseContent,
                        ["phone"] = payload.phone
                    }
                };
            }
            else
            {
                _logger.LogError("Z-API error sending message to {Recipient}: {StatusCode} - {Response}", 
                    recipientId, response.StatusCode, responseContent);

                return new MessagingResult
                {
                    Success = false,
                    ErrorMessage = $"HTTP {response.StatusCode}: {responseContent}"
                };
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending Z-API message to {Recipient}", recipientId);
            return new MessagingResult
            {
                Success = false,
                ErrorMessage = ex.Message
            };
        }
    }

    public async Task<MessagingResult> SendMediaMessageAsync(string recipientId, string mediaUrl, MediaType mediaType, string? caption = null, CancellationToken cancellationToken = default)
    {
        try
        {
            var endpoint = mediaType switch
            {
                MediaType.Image => "send-image",
                MediaType.Video => "send-video",
                MediaType.Audio => "send-audio",
                MediaType.Document => "send-document",
                _ => "send-document"
            };

            var payload = new
            {
                phone = recipientId.Replace("+", "").Replace("-", "").Replace(" ", ""),
                image = mediaUrl, // Z-API uses generic 'image' field for media URL
                caption = caption ?? ""
            };

            var json = JsonSerializer.Serialize(payload);
            var httpContent = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await _httpClient.PostAsync(
                $"{_baseUrl}/instances/{_instanceId}/token/{_token}/{endpoint}", 
                httpContent, 
                cancellationToken);

            var responseContent = await response.Content.ReadAsStringAsync(cancellationToken);

            if (response.IsSuccessStatusCode)
            {
                var result = JsonSerializer.Deserialize<ZApiResponse>(responseContent);
                
                _logger.LogInformation("Z-API media message sent successfully to {Recipient}: {MessageId}", 
                    recipientId, result?.MessageId);

                return new MessagingResult
                {
                    Success = true,
                    ExternalMessageId = result?.MessageId,
                    Metadata = new Dictionary<string, object>
                    {
                        ["mediaUrl"] = mediaUrl,
                        ["mediaType"] = mediaType.ToString(),
                        ["response"] = responseContent
                    }
                };
            }
            else
            {
                return new MessagingResult
                {
                    Success = false,
                    ErrorMessage = $"HTTP {response.StatusCode}: {responseContent}"
                };
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending Z-API media message to {Recipient}", recipientId);
            return new MessagingResult
            {
                Success = false,
                ErrorMessage = ex.Message
            };
        }
    }

    public async Task<MessagingResult> SendTemplateMessageAsync(string recipientId, string templateName, Dictionary<string, string> parameters, CancellationToken cancellationToken = default)
    {
        try
        {
            // Z-API template format
            var payload = new
            {
                phone = recipientId.Replace("+", "").Replace("-", "").Replace(" ", ""),
                templateName = templateName,
                language = "pt_BR", // Default to Portuguese Brazil
                components = parameters.Select(p => new
                {
                    type = "body",
                    parameters = new[] { new { type = "text", text = p.Value } }
                }).ToArray()
            };

            var json = JsonSerializer.Serialize(payload);
            var httpContent = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await _httpClient.PostAsync(
                $"{_baseUrl}/instances/{_instanceId}/token/{_token}/send-template", 
                httpContent, 
                cancellationToken);

            var responseContent = await response.Content.ReadAsStringAsync(cancellationToken);

            if (response.IsSuccessStatusCode)
            {
                var result = JsonSerializer.Deserialize<ZApiResponse>(responseContent);
                
                return new MessagingResult
                {
                    Success = true,
                    ExternalMessageId = result?.MessageId,
                    Metadata = new Dictionary<string, object>
                    {
                        ["templateName"] = templateName,
                        ["parameters"] = parameters
                    }
                };
            }
            else
            {
                return new MessagingResult
                {
                    Success = false,
                    ErrorMessage = $"HTTP {response.StatusCode}: {responseContent}"
                };
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending Z-API template message to {Recipient}", recipientId);
            return new MessagingResult
            {
                Success = false,
                ErrorMessage = ex.Message
            };
        }
    }

    public async Task<QrCodeResult?> GetQrCodeAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            var response = await _httpClient.GetAsync(
                $"{_baseUrl}/instances/{instanceId}/token/{_token}/qr-code", 
                cancellationToken);

            if (response.IsSuccessStatusCode)
            {
                var responseContent = await response.Content.ReadAsStringAsync(cancellationToken);
                var qrResult = JsonSerializer.Deserialize<ZApiQrResponse>(responseContent);

                if (qrResult?.QrCode != null)
                {
                    return new QrCodeResult
                    {
                        QrCodeData = qrResult.QrCode,
                        QrCodeImageUrl = qrResult.QrCodeImage,
                        ExpiresAt = DateTime.UtcNow.AddMinutes(2) // Z-API QR codes typically expire in 2 minutes
                    };
                }
            }

            _logger.LogWarning("Failed to get QR code from Z-API: {StatusCode}", response.StatusCode);
            return null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting Z-API QR code");
            return null;
        }
    }

    public async Task<ConnectionStatus> GetConnectionStatusAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            var response = await _httpClient.GetAsync(
                $"{_baseUrl}/instances/{instanceId}/token/{_token}/status", 
                cancellationToken);

            if (response.IsSuccessStatusCode)
            {
                var responseContent = await response.Content.ReadAsStringAsync(cancellationToken);
                var status = JsonSerializer.Deserialize<ZApiStatusResponse>(responseContent);

                return status?.Connected == true ? ConnectionStatus.Connected :
                       status?.QrCode == true ? ConnectionStatus.QrCodeRequired :
                       ConnectionStatus.Disconnected;
            }

            return ConnectionStatus.Error;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting Z-API connection status");
            return ConnectionStatus.Error;
        }
    }

    public async Task<bool> DisconnectInstanceAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            var response = await _httpClient.DeleteAsync(
                $"{_baseUrl}/instances/{instanceId}/token/{_token}/disconnect", 
                cancellationToken);

            var success = response.IsSuccessStatusCode;
            _logger.LogInformation("Z-API disconnect result for instance {InstanceId}: {Success}", instanceId, success);
            
            return success;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error disconnecting Z-API instance {InstanceId}", instanceId);
            return false;
        }
    }

    public Task<IncomingMessage?> ParseWebhookPayloadAsync(object payload, CancellationToken cancellationToken = default)
    {
        try
        {
            var json = JsonSerializer.Serialize(payload);
            var webhookData = JsonSerializer.Deserialize<ZApiWebhook>(json);

            if (webhookData?.Data?.MessageId != null)
            {
                var incomingMessage = new IncomingMessage
                {
                    ExternalMessageId = webhookData.Data.MessageId,
                    SenderId = webhookData.Data.Phone ?? "",
                    SenderName = webhookData.Data.SenderName ?? "",
                    RecipientId = webhookData.Data.InstanceId ?? _instanceId,
                    Content = webhookData.Data.Message?.Text ?? "",
                    MessageType = MapZApiMessageType(webhookData.Data.Message?.MessageType ?? ""),
                    Timestamp = webhookData.Data.MessageTimestamp > 0 ? 
                        DateTimeOffset.FromUnixTimeSeconds(webhookData.Data.MessageTimestamp).DateTime : 
                        DateTime.UtcNow,
                    Metadata = new Dictionary<string, object>
                    {
                        ["chatId"] = webhookData.Data.ChatId ?? "",
                        ["isGroup"] = webhookData.Data.IsGroup ?? false,
                        ["fromMe"] = webhookData.Data.FromMe ?? false
                    }
                };

                // Handle media
                if (webhookData.Data.Message?.Image != null || 
                    webhookData.Data.Message?.Video != null || 
                    webhookData.Data.Message?.Audio != null ||
                    webhookData.Data.Message?.Document != null)
                {
                    incomingMessage.Media = new MediaInfo
                    {
                        Url = webhookData.Data.Message.Image ?? 
                              webhookData.Data.Message.Video ?? 
                              webhookData.Data.Message.Audio ?? 
                              webhookData.Data.Message.Document,
                        MediaType = MapZApiMessageType(webhookData.Data.Message.MessageType ?? "")
                    };
                }

                return Task.FromResult<IncomingMessage?>(incomingMessage);
            }

            return Task.FromResult<IncomingMessage?>(null);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error parsing Z-API webhook payload");
            return Task.FromResult<IncomingMessage?>(null);
        }
    }

    public Task<bool> ValidateWebhookAsync(string signature, string payload, string secret, CancellationToken cancellationToken = default)
    {
        try
        {
            // Z-API webhook validation (if implemented by provider)
            var webhookSecret = _configuration["ZApi:WebhookSecret"] ?? secret;
            
            if (string.IsNullOrEmpty(webhookSecret))
            {
                // No validation configured - accept all webhooks (not recommended for production)
                _logger.LogWarning("Z-API webhook validation disabled - no secret configured");
                return Task.FromResult(true);
            }

            // Implement HMAC validation if Z-API supports it
            using var hmac = new System.Security.Cryptography.HMACSHA256(System.Text.Encoding.UTF8.GetBytes(webhookSecret));
            var computedHash = hmac.ComputeHash(System.Text.Encoding.UTF8.GetBytes(payload));
            var computedSignature = Convert.ToHexString(computedHash).ToLower();
            
            var isValid = signature.Equals(computedSignature, StringComparison.OrdinalIgnoreCase);
            
            if (!isValid)
            {
                _logger.LogWarning("Invalid Z-API webhook signature");
            }
            
            return Task.FromResult(isValid);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error validating Z-API webhook signature");
            return Task.FromResult(false);
        }
    }

    // Private helper methods and DTOs
    private static MessageType MapZApiMessageType(string messageType)
    {
        return messageType.ToLower() switch
        {
            "text" => MessageType.Text,
            "image" => MessageType.Image,
            "audio" => MessageType.Audio,
            "video" => MessageType.Video,
            "document" => MessageType.Document,
            "location" => MessageType.Location,
            "contact" => MessageType.Contact,
            _ => MessageType.Text
        };
    }

    private static MediaType MapZApiMessageType(string messageType)
    {
        return messageType.ToLower() switch
        {
            "image" => MediaType.Image,
            "audio" => MediaType.Audio,
            "video" => MediaType.Video,
            "document" => MediaType.Document,
            _ => MediaType.Document
        };
    }

    // Z-API Response DTOs
    private class ZApiResponse
    {
        public string? MessageId { get; set; }
        public bool? Success { get; set; }
        public string? Error { get; set; }
    }

    private class ZApiQrResponse
    {
        public string? QrCode { get; set; }
        public string? QrCodeImage { get; set; }
    }

    private class ZApiStatusResponse
    {
        public bool? Connected { get; set; }
        public bool? QrCode { get; set; }
        public string? Status { get; set; }
    }

    private class ZApiWebhook
    {
        public ZApiWebhookData? Data { get; set; }
    }

    private class ZApiWebhookData
    {
        public string? MessageId { get; set; }
        public string? Phone { get; set; }
        public string? SenderName { get; set; }
        public string? InstanceId { get; set; }
        public string? ChatId { get; set; }
        public bool? IsGroup { get; set; }
        public bool? FromMe { get; set; }
        public long MessageTimestamp { get; set; }
        public ZApiMessage? Message { get; set; }
    }

    private class ZApiMessage
    {
        public string? Text { get; set; }
        public string? Image { get; set; }
        public string? Video { get; set; }
        public string? Audio { get; set; }
        public string? Document { get; set; }
        public string? MessageType { get; set; }
    }
}