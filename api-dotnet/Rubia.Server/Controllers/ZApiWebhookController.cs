using Microsoft.AspNetCore.Cors;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Enums;
using Rubia.Server.Integrations.Adapters;
using Rubia.Server.Services.Interfaces;
using System.Text.Json;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/webhooks/zapi")]
[EnableCors("WebhookPolicy")]
public class ZApiWebhookController : ControllerBase
{
    private readonly ILogger<ZApiWebhookController> _logger;
    private readonly IMessagingService _messagingService;
    private readonly IConversationService _conversationService;
    private readonly ICustomerService _customerService;
    private readonly IZApiConnectionMonitorService _connectionMonitor;
    private readonly RubiaDbContext _context;
    private readonly ZApiAdapter _zApiAdapter;

    public ZApiWebhookController(
        ILogger<ZApiWebhookController> logger,
        IMessagingService messagingService,
        IConversationService conversationService,
        ICustomerService customerService,
        IZApiConnectionMonitorService connectionMonitor,
        RubiaDbContext context,
        ZApiAdapter zApiAdapter)
    {
        _logger = logger;
        _messagingService = messagingService;
        _conversationService = conversationService;
        _customerService = customerService;
        _connectionMonitor = connectionMonitor;
        _context = context;
        _zApiAdapter = zApiAdapter;
    }

    [HttpPost("message")]
    public async Task<IActionResult> ReceiveMessage([FromBody] object payload)
    {
        try
        {
            _logger.LogInformation("Z-API webhook received: {Payload}", JsonSerializer.Serialize(payload));

            // Validate webhook if signature is provided
            var signature = Request.Headers["X-Webhook-Signature"].FirstOrDefault();
            if (!string.IsNullOrEmpty(signature))
            {
                var payloadString = await ReadRequestBodyAsync();
                var isValid = await _zApiAdapter.ValidateWebhookAsync(signature, payloadString, "", HttpContext.RequestAborted);
                
                if (!isValid)
                {
                    _logger.LogWarning("Invalid Z-API webhook signature");
                    return Unauthorized("Invalid webhook signature");
                }
            }

            // Parse incoming message
            var incomingMessage = await _zApiAdapter.ParseWebhookPayloadAsync(payload, HttpContext.RequestAborted);
            if (incomingMessage == null)
            {
                _logger.LogWarning("Failed to parse Z-API webhook payload");
                return BadRequest("Invalid payload format");
            }

            // Skip messages from ourselves (fromMe = true)
            if (incomingMessage.Metadata?.GetValueOrDefault("fromMe") as bool? == true)
            {
                _logger.LogDebug("Skipping outbound message: {MessageId}", incomingMessage.ExternalMessageId);
                return Ok(new { status = "ignored", reason = "outbound_message" });
            }

            // Find or create customer
            var customer = await GetOrCreateCustomerAsync(incomingMessage);
            if (customer == null)
            {
                _logger.LogError("Failed to find or create customer for phone: {Phone}", incomingMessage.SenderId);
                return BadRequest("Failed to process customer");
            }

            // Find or create conversation
            var conversation = await GetOrCreateConversationAsync(customer, incomingMessage);
            if (conversation == null)
            {
                _logger.LogError("Failed to find or create conversation for customer: {CustomerId}", customer.Id);
                return BadRequest("Failed to process conversation");
            }

            // Create message DTO
            var messageDto = new MessageDto
            {
                Id = Guid.NewGuid(),
                ConversationId = conversation.Id.Value,
                Content = incomingMessage.Content,
                SenderType = SenderType.Customer,
                ExternalMessageId = incomingMessage.ExternalMessageId,
                Status = MessageStatus.Received,
                CreatedAt = incomingMessage.Timestamp,
                IsAiGenerated = false
            };

            // Process the message through the messaging service
            var processedMessage = await _messagingService.ProcessIncomingMessageAsync(messageDto, HttpContext.RequestAborted);

            _logger.LogInformation("Z-API message processed successfully: {MessageId} for conversation {ConversationId}", 
                processedMessage.Id, conversation.Id);

            return Ok(new 
            { 
                status = "success", 
                messageId = processedMessage.Id,
                conversationId = conversation.Id 
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing Z-API webhook");
            return StatusCode(500, new { status = "error", message = ex.Message });
        }
    }

    [HttpPost("status")]
    public async Task<IActionResult> ReceiveStatus([FromBody] object payload)
    {
        try
        {
            _logger.LogInformation("Z-API status webhook received: {Payload}", JsonSerializer.Serialize(payload));

            var payloadJson = JsonSerializer.Serialize(payload);
            var statusData = JsonSerializer.Deserialize<ZApiStatusWebhook>(payloadJson);

            if (statusData?.Data?.MessageId != null && statusData.Data.Status != null)
            {
                var messageStatus = MapZApiStatus(statusData.Data.Status);
                var success = await _messagingService.DeliveryStatusUpdateAsync(
                    statusData.Data.MessageId, 
                    messageStatus, 
                    HttpContext.RequestAborted);

                if (success)
                {
                    _logger.LogInformation("Message status updated: {MessageId} to {Status}", 
                        statusData.Data.MessageId, messageStatus);
                }
            }

            return Ok(new { status = "success" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing Z-API status webhook");
            return StatusCode(500, new { status = "error", message = ex.Message });
        }
    }

    [HttpPost("connection")]
    public async Task<IActionResult> ReceiveConnectionStatus([FromBody] object payload)
    {
        try
        {
            _logger.LogInformation("Z-API connection webhook received: {Payload}", JsonSerializer.Serialize(payload));

            var payloadJson = JsonSerializer.Serialize(payload);
            var connectionData = JsonSerializer.Deserialize<ZApiConnectionWebhook>(payloadJson);

            if (connectionData?.Data?.InstanceId != null)
            {
                // Find the instance by external ID
                var instance = await _context.WhatsAppInstances
                    .FirstOrDefaultAsync(i => i.ExternalInstanceId == connectionData.Data.InstanceId);

                if (instance != null)
                {
                    // Update connection status through the monitor service
                    await _connectionMonitor.GetConnectionStatusAsync(instance.Id, HttpContext.RequestAborted);
                    
                    _logger.LogInformation("Instance connection status updated: {InstanceId} - {Status}", 
                        instance.Id, connectionData.Data.Connected);
                }
            }

            return Ok(new { status = "success" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing Z-API connection webhook");
            return StatusCode(500, new { status = "error", message = ex.Message });
        }
    }

    [HttpGet("health")]
    public IActionResult Health()
    {
        return Ok(new { status = "healthy", timestamp = DateTime.UtcNow });
    }

    // Private helper methods
    private async Task<CustomerDto?> GetOrCreateCustomerAsync(IncomingMessage incomingMessage)
    {
        try
        {
            // Try to find existing customer by phone
            var existingCustomer = await _context.Customers
                .FirstOrDefaultAsync(c => c.Phone == incomingMessage.SenderId);

            if (existingCustomer != null)
            {
                return new CustomerDto
                {
                    Id = existingCustomer.Id,
                    Name = existingCustomer.Name,
                    Phone = existingCustomer.Phone,
                    Email = existingCustomer.Email,
                    CompanyId = existingCustomer.CompanyId
                };
            }

            // Create new customer
            var newCustomerDto = new CreateCustomerDto
            {
                Name = incomingMessage.SenderName ?? incomingMessage.SenderId,
                Phone = incomingMessage.SenderId,
                CompanyId = await GetCompanyIdFromInstanceAsync(incomingMessage.RecipientId)
            };

            return await _customerService.CreateAsync(newCustomerDto);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting or creating customer for phone: {Phone}", incomingMessage.SenderId);
            return null;
        }
    }

    private async Task<ConversationDto?> GetOrCreateConversationAsync(CustomerDto customer, IncomingMessage incomingMessage)
    {
        try
        {
            var chatId = incomingMessage.Metadata?.GetValueOrDefault("chatId")?.ToString() ?? 
                        $"{customer.Phone}@c.us";

            // Try to find existing conversation
            var existingConversation = await _context.Conversations
                .FirstOrDefaultAsync(c => 
                    c.CustomerId == customer.Id && 
                    c.ExternalId == chatId &&
                    c.Channel == Channel.WhatsApp);

            if (existingConversation != null)
            {
                return new ConversationDto
                {
                    Id = existingConversation.Id,
                    CustomerId = existingConversation.CustomerId,
                    ExternalId = existingConversation.ExternalId,
                    Channel = existingConversation.Channel,
                    Status = existingConversation.Status,
                    CompanyId = existingConversation.CompanyId
                };
            }

            // Create new conversation
            var newConversation = new ConversationDto
            {
                Id = Guid.NewGuid(),
                CustomerId = customer.Id.Value,
                ExternalId = chatId,
                Channel = Channel.WhatsApp,
                Status = ConversationStatus.Waiting,
                CompanyId = customer.CompanyId,
                CreatedAt = DateTime.UtcNow
            };

            return await _conversationService.CreateAsync(newConversation);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting or creating conversation for customer: {CustomerId}", customer.Id);
            return null;
        }
    }

    private async Task<Guid> GetCompanyIdFromInstanceAsync(string instanceId)
    {
        try
        {
            var instance = await _context.WhatsAppInstances
                .FirstOrDefaultAsync(i => i.ExternalInstanceId == instanceId);
            
            return instance?.CompanyId ?? Guid.Empty;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting company ID for instance: {InstanceId}", instanceId);
            return Guid.Empty;
        }
    }

    private static MessageStatus MapZApiStatus(string status)
    {
        return status.ToLower() switch
        {
            "sent" => MessageStatus.Sent,
            "delivered" => MessageStatus.Delivered,
            "read" => MessageStatus.Read,
            "failed" => MessageStatus.Failed,
            _ => MessageStatus.Sent
        };
    }

    private async Task<string> ReadRequestBodyAsync()
    {
        Request.EnableBuffering();
        using var reader = new StreamReader(Request.Body, leaveOpen: true);
        var body = await reader.ReadToEndAsync();
        Request.Body.Position = 0;
        return body;
    }

    // Webhook DTOs
    private class ZApiStatusWebhook
    {
        public ZApiStatusData? Data { get; set; }
    }

    private class ZApiStatusData
    {
        public string? MessageId { get; set; }
        public string? Status { get; set; }
        public string? Phone { get; set; }
        public DateTime Timestamp { get; set; }
    }

    private class ZApiConnectionWebhook
    {
        public ZApiConnectionData? Data { get; set; }
    }

    private class ZApiConnectionData
    {
        public string? InstanceId { get; set; }
        public bool Connected { get; set; }
        public DateTime Timestamp { get; set; }
    }
}