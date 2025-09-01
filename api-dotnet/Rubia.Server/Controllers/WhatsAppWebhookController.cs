using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Events;
using Rubia.Server.Services.Interfaces;
using System.Text.Json;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/webhook/whatsapp")]
[AllowAnonymous] // Webhooks geralmente não têm autenticação via JWT
public class WhatsAppWebhookController : ControllerBase
{
    private readonly RubiaDbContext _context;
    private readonly IEventBusService _eventBus;
    private readonly IConfiguration _configuration;
    private readonly ILogger<WhatsAppWebhookController> _logger;

    public WhatsAppWebhookController(
        RubiaDbContext context,
        IEventBusService eventBus,
        IConfiguration configuration,
        ILogger<WhatsAppWebhookController> logger)
    {
        _context = context;
        _eventBus = eventBus;
        _configuration = configuration;
        _logger = logger;
    }

    [HttpPost("message")]
    public async Task<IActionResult> ReceiveMessage([FromBody] JsonElement payload)
    {
        try
        {
            _logger.LogDebug("Received WhatsApp webhook: {Payload}", payload.ToString());

            // Parse webhook payload
            var webhookData = ExtractWebhookData(payload);
            if (webhookData == null)
            {
                _logger.LogWarning("Failed to parse webhook payload");
                return BadRequest("Invalid payload");
            }

            // Find WhatsApp instance
            var instance = await _context.WhatsAppInstances
                .Include(w => w.Company)
                .FirstOrDefaultAsync(w => w.InstanceId == webhookData.InstanceId && w.IsActive);

            if (instance == null)
            {
                _logger.LogWarning("WhatsApp instance not found: {InstanceId}", webhookData.InstanceId);
                return NotFound("Instance not found");
            }

            // Find or create customer
            var customer = await FindOrCreateCustomerAsync(instance.CompanyId, webhookData.Phone, webhookData.SenderName);

            // Find or create conversation
            var conversation = await FindOrCreateConversationAsync(instance.CompanyId, customer.Id);

            // Create message
            var message = new Message
            {
                Id = Guid.NewGuid(),
                ConversationId = conversation.Id,
                Content = webhookData.Message,
                SenderType = SenderType.CUSTOMER,
                SenderId = customer.Id,
                Status = MessageStatus.Received,
                ExternalMessageId = webhookData.MessageId,
                CreatedAt = webhookData.Timestamp
            };

            _context.Messages.Add(message);
            await _context.SaveChangesAsync();

            // Create message DTO
            var messageDto = new MessageDto
            {
                Id = message.Id,
                ConversationId = message.ConversationId,
                Content = message.Content,
                SenderType = message.SenderType,
                SenderId = message.SenderId,
                Status = message.Status,
                ExternalMessageId = message.ExternalMessageId,
                CreatedAt = message.CreatedAt,
                UpdatedAt = message.UpdatedAt
            };

            // Publish event
            await _eventBus.PublishAsync(new MessageCreatedEvent
            {
                Message = messageDto,
                CompanyId = instance.CompanyId
            });

            _logger.LogInformation("WhatsApp message processed: {MessageId}", message.Id);
            return Ok(new { status = "success", messageId = message.Id });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing WhatsApp webhook");
            return StatusCode(500, "Internal server error");
        }
    }

    private WebhookMessageDto? ExtractWebhookData(JsonElement payload)
    {
        try
        {
            // Adapte este parsing conforme o formato real do webhook da Z-API
            var data = payload.GetProperty("data");
            
            return new WebhookMessageDto
            {
                Phone = data.GetProperty("phone").GetString() ?? string.Empty,
                Message = data.GetProperty("message").GetString() ?? string.Empty,
                MessageId = data.GetProperty("messageId").GetString() ?? string.Empty,
                SenderName = data.TryGetProperty("senderName", out var senderName) ? senderName.GetString() ?? "Unknown" : "Unknown",
                Timestamp = data.TryGetProperty("timestamp", out var timestamp) 
                    ? DateTimeOffset.FromUnixTimeSeconds(timestamp.GetInt64()).DateTime 
                    : DateTime.UtcNow,
                InstanceId = data.GetProperty("instanceId").GetString() ?? string.Empty
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error parsing webhook payload: {Payload}", payload.ToString());
            return null;
        }
    }

    private async Task<Customer> FindOrCreateCustomerAsync(Guid companyId, string phone, string name)
    {
        var customer = await _context.Customers
            .FirstOrDefaultAsync(c => c.CompanyId == companyId && c.Phone == phone);

        if (customer == null)
        {
            customer = new Customer
            {
                Id = Guid.NewGuid(),
                CompanyId = companyId,
                Name = name,
                Phone = phone,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.Customers.Add(customer);
            await _context.SaveChangesAsync();
        }

        return customer;
    }

    private async Task<Conversation> FindOrCreateConversationAsync(Guid companyId, Guid customerId)
    {
        // Procura por uma conversa ativa com este cliente
        var conversation = await _context.Conversations
            .Include(c => c.Participants)
            .FirstOrDefaultAsync(c => c.CompanyId == companyId 
                                    && c.Status != ConversationStatus.FINALIZADOS
                                    && c.Participants.Any(p => p.CustomerId == customerId));

        if (conversation == null)
        {
            // Cria nova conversa
            conversation = new Conversation
            {
                Id = Guid.NewGuid(),
                CompanyId = companyId,
                Channel = Channel.WHATSAPP,
                Status = ConversationStatus.ENTRADA,
                ConversationType = ConversationType.ONE_TO_ONE,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.Conversations.Add(conversation);

            // Adiciona participante
            var participant = new ConversationParticipant
            {
                Id = Guid.NewGuid(),
                ConversationId = conversation.Id,
                CompanyId = companyId,
                CustomerId = customerId,
                JoinedAt = DateTime.UtcNow
            };

            _context.ConversationParticipants.Add(participant);
            await _context.SaveChangesAsync();
        }

        return conversation;
    }
}