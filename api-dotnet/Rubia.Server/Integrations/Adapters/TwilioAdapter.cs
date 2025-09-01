using Microsoft.Extensions.Options;
using Rubia.Server.DTOs;
using Rubia.Server.Enums;
using Rubia.Server.Integrations.Adapters;
using System.Text.Json;
using Twilio;
using Twilio.Rest.Api.V2010.Account;
using Twilio.Types;

namespace Rubia.Server.Integrations.Adapters;

public class TwilioAdapter : IMessagingAdapter
{
    private readonly IConfiguration _configuration;
    private readonly ILogger<TwilioAdapter> _logger;
    private readonly string _accountSid;
    private readonly string _authToken;
    private readonly string _fromNumber;

    public string ProviderName => "Twilio";

    public TwilioAdapter(
        IConfiguration configuration,
        ILogger<TwilioAdapter> logger)
    {
        _configuration = configuration;
        _logger = logger;
        
        _accountSid = _configuration["Twilio:AccountSid"] ?? throw new InvalidOperationException("Twilio AccountSid not configured");
        _authToken = _configuration["Twilio:AuthToken"] ?? throw new InvalidOperationException("Twilio AuthToken not configured");
        _fromNumber = _configuration["Twilio:FromNumber"] ?? throw new InvalidOperationException("Twilio FromNumber not configured");

        TwilioClient.Init(_accountSid, _authToken);
    }

    public async Task<bool> IsAvailableAsync(CancellationToken cancellationToken = default)
    {
        try
        {
            // Test Twilio connection by fetching account info
            var account = await AccountResource.FetchAsync(pathSid: _accountSid);
            return account.Status == AccountResource.StatusEnum.Active;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error checking Twilio availability");
            return false;
        }
    }

    public async Task<MessagingResult> SendTextMessageAsync(string recipientId, string content, CancellationToken cancellationToken = default)
    {
        try
        {
            var toPhoneNumber = new PhoneNumber($"whatsapp:{recipientId}");
            var fromPhoneNumber = new PhoneNumber($"whatsapp:{_fromNumber}");

            var message = await MessageResource.CreateAsync(
                body: content,
                from: fromPhoneNumber,
                to: toPhoneNumber
            );

            _logger.LogInformation("Twilio message sent: {MessageSid} to {Recipient}", message.Sid, recipientId);

            return new MessagingResult
            {
                Success = true,
                ExternalMessageId = message.Sid,
                Metadata = new Dictionary<string, object>
                {
                    ["status"] = message.Status.ToString(),
                    ["direction"] = message.Direction.ToString(),
                    ["price"] = message.Price ?? "0"
                }
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending Twilio message to {Recipient}", recipientId);
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
            var toPhoneNumber = new PhoneNumber($"whatsapp:{recipientId}");
            var fromPhoneNumber = new PhoneNumber($"whatsapp:{_fromNumber}");

            var message = await MessageResource.CreateAsync(
                body: caption,
                from: fromPhoneNumber,
                to: toPhoneNumber,
                mediaUrl: new List<Uri> { new Uri(mediaUrl) }
            );

            _logger.LogInformation("Twilio media message sent: {MessageSid} to {Recipient}", message.Sid, recipientId);

            return new MessagingResult
            {
                Success = true,
                ExternalMessageId = message.Sid,
                Metadata = new Dictionary<string, object>
                {
                    ["status"] = message.Status.ToString(),
                    ["mediaUrl"] = mediaUrl,
                    ["mediaType"] = mediaType.ToString()
                }
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending Twilio media message to {Recipient}", recipientId);
            return new MessagingResult
            {
                Success = false,
                ErrorMessage = ex.Message
            };
        }
    }

    public async Task<MessagingResult> SendTemplateMessageAsync(string recipientId, string templateName, Dictionary<string, string> parameters, CancellationToken cancellationToken = default)
    {
        // Twilio WhatsApp templates need to be pre-approved
        // For now, we'll send as regular text with parameter substitution
        try
        {
            var templateContent = await GetTemplateContentAsync(templateName, parameters);
            return await SendTextMessageAsync(recipientId, templateContent, cancellationToken);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending Twilio template message to {Recipient}", recipientId);
            return new MessagingResult
            {
                Success = false,
                ErrorMessage = ex.Message
            };
        }
    }

    public Task<QrCodeResult?> GetQrCodeAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        // Twilio doesn't use QR codes - it uses phone number verification
        _logger.LogWarning("QR Code not supported by Twilio adapter");
        return Task.FromResult<QrCodeResult?>(null);
    }

    public async Task<ConnectionStatus> GetConnectionStatusAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        try
        {
            var isAvailable = await IsAvailableAsync(cancellationToken);
            return isAvailable ? ConnectionStatus.Connected : ConnectionStatus.Disconnected;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting Twilio connection status");
            return ConnectionStatus.Error;
        }
    }

    public Task<bool> DisconnectInstanceAsync(string instanceId, CancellationToken cancellationToken = default)
    {
        // Twilio doesn't have instance disconnect concept
        _logger.LogWarning("Disconnect not applicable for Twilio adapter");
        return Task.FromResult(true);
    }

    public Task<IncomingMessage?> ParseWebhookPayloadAsync(object payload, CancellationToken cancellationToken = default)
    {
        try
        {
            // Parse Twilio webhook format
            if (payload is Dictionary<string, string> twilioData)
            {
                var incomingMessage = new IncomingMessage
                {
                    ExternalMessageId = twilioData.GetValueOrDefault("MessageSid", ""),
                    SenderId = twilioData.GetValueOrDefault("From", "").Replace("whatsapp:", ""),
                    RecipientId = twilioData.GetValueOrDefault("To", "").Replace("whatsapp:", ""),
                    Content = twilioData.GetValueOrDefault("Body", ""),
                    MessageType = DetermineMessageType(twilioData),
                    Timestamp = DateTime.UtcNow,
                    SenderName = twilioData.GetValueOrDefault("ProfileName", ""),
                    Metadata = new Dictionary<string, object>(twilioData.Select(kvp => 
                        new KeyValuePair<string, object>(kvp.Key, kvp.Value)))
                };

                // Handle media
                if (twilioData.ContainsKey("MediaUrl0"))
                {
                    incomingMessage.Media = new MediaInfo
                    {
                        Url = twilioData.GetValueOrDefault("MediaUrl0"),
                        MimeType = twilioData.GetValueOrDefault("MediaContentType0"),
                        MediaType = MapTwilioMediaType(twilioData.GetValueOrDefault("MediaContentType0", ""))
                    };
                }

                return Task.FromResult<IncomingMessage?>(incomingMessage);
            }

            _logger.LogWarning("Invalid Twilio webhook payload format");
            return Task.FromResult<IncomingMessage?>(null);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error parsing Twilio webhook payload");
            return Task.FromResult<IncomingMessage?>(null);
        }
    }

    public Task<bool> ValidateWebhookAsync(string signature, string payload, string secret, CancellationToken cancellationToken = default)
    {
        try
        {
            // Implement Twilio webhook signature validation
            var twilioSignature = _configuration["Twilio:WebhookSecret"] ?? secret;
            
            // Twilio uses SHA256 HMAC for validation
            using var hmac = new System.Security.Cryptography.HMACSHA256(System.Text.Encoding.UTF8.GetBytes(twilioSignature));
            var computedHash = hmac.ComputeHash(System.Text.Encoding.UTF8.GetBytes(payload));
            var computedSignature = Convert.ToBase64String(computedHash);
            
            var isValid = signature.Equals(computedSignature, StringComparison.OrdinalIgnoreCase);
            
            if (!isValid)
            {
                _logger.LogWarning("Invalid Twilio webhook signature");
            }
            
            return Task.FromResult(isValid);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error validating Twilio webhook signature");
            return Task.FromResult(false);
        }
    }

    // Private helper methods
    private async Task<string> GetTemplateContentAsync(string templateName, Dictionary<string, string> parameters)
    {
        // This would typically fetch from a template repository
        // For now, return a simple substitution
        var template = $"Template {templateName}";
        
        foreach (var param in parameters)
        {
            template = template.Replace($"{{{param.Key}}}", param.Value);
        }
        
        return template;
    }

    private static MessageType DetermineMessageType(Dictionary<string, string> twilioData)
    {
        if (twilioData.ContainsKey("MediaUrl0"))
        {
            var contentType = twilioData.GetValueOrDefault("MediaContentType0", "").ToLower();
            return contentType switch
            {
                var ct when ct.StartsWith("image/") => MessageType.Image,
                var ct when ct.StartsWith("audio/") => MessageType.Audio,
                var ct when ct.StartsWith("video/") => MessageType.Video,
                var ct when ct.StartsWith("application/") => MessageType.Document,
                _ => MessageType.Document
            };
        }

        return MessageType.Text;
    }

    private static MediaType MapTwilioMediaType(string contentType)
    {
        return contentType.ToLower() switch
        {
            var ct when ct.StartsWith("image/") => MediaType.Image,
            var ct when ct.StartsWith("audio/") => MediaType.Audio,
            var ct when ct.StartsWith("video/") => MediaType.Video,
            var ct when ct.StartsWith("application/") => MediaType.Document,
            _ => MediaType.Document
        };
    }
}