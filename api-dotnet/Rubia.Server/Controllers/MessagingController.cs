using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/messaging")]
[Authorize]
public class MessagingController : ControllerBase
{
    private readonly IWhatsAppService _whatsAppService;
    private readonly IMessageService _messageService;
    private readonly IConversationService _conversationService;
    private readonly ILogger<MessagingController> _logger;

    public MessagingController(
        IWhatsAppService whatsAppService,
        IMessageService messageService,
        IConversationService conversationService,
        ILogger<MessagingController> logger)
    {
        _whatsAppService = whatsAppService;
        _messageService = messageService;
        _conversationService = conversationService;
        _logger = logger;
    }

    [HttpPost("send-text")]
    public async Task<ActionResult<MessageDto>> SendTextMessage([FromBody] SendTextMessageDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            // Send via WhatsApp
            var success = await _whatsAppService.SendTextMessageAsync(dto.PhoneNumber, dto.Message, companyId.Value);
            if (!success)
            {
                return BadRequest(new { message = "Failed to send WhatsApp message" });
            }

            // Create message record
            var createMessageDto = new CreateMessageDto
            {
                ConversationId = dto.ConversationId,
                Content = dto.Message,
                SenderType = Rubia.Server.Enums.SenderType.USER,
                SenderId = GetUserId()
            };

            var messageDto = await _messageService.CreateAsync(createMessageDto);

            return Ok(messageDto);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending text message");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("send-template")]
    public async Task<ActionResult<MessageDto>> SendTemplateMessage([FromBody] SendTemplateMessageDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            // Here you would process template variables and render the final message
            var processedMessage = ProcessTemplate(dto.TemplateContent, dto.Variables);

            // Send via WhatsApp
            var success = await _whatsAppService.SendTextMessageAsync(dto.PhoneNumber, processedMessage, companyId.Value);
            if (!success)
            {
                return BadRequest(new { message = "Failed to send WhatsApp message" });
            }

            // Create message record
            var createMessageDto = new CreateMessageDto
            {
                ConversationId = dto.ConversationId,
                Content = processedMessage,
                SenderType = Rubia.Server.Enums.SenderType.USER,
                SenderId = GetUserId(),
                MessageTemplateId = dto.TemplateId
            };

            var messageDto = await _messageService.CreateAsync(createMessageDto);

            return Ok(messageDto);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending template message");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("broadcast")]
    public async Task<IActionResult> BroadcastMessage([FromBody] BroadcastMessageDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var results = new List<BroadcastResult>();

            foreach (var phoneNumber in dto.PhoneNumbers)
            {
                try
                {
                    var success = await _whatsAppService.SendTextMessageAsync(phoneNumber, dto.Message, companyId.Value);
                    results.Add(new BroadcastResult 
                    { 
                        PhoneNumber = phoneNumber, 
                        Success = success,
                        Error = success ? null : "Failed to send message"
                    });
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to send broadcast message to {PhoneNumber}", phoneNumber);
                    results.Add(new BroadcastResult 
                    { 
                        PhoneNumber = phoneNumber, 
                        Success = false,
                        Error = ex.Message
                    });
                }
            }

            var summary = new BroadcastSummaryDto
            {
                TotalSent = results.Count(r => r.Success),
                TotalFailed = results.Count(r => !r.Success),
                Results = results
            };

            return Ok(summary);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error broadcasting message");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("status/{phoneNumber}")]
    public async Task<ActionResult<ContactStatusDto>> GetContactStatus(string phoneNumber)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var instance = await _whatsAppService.GetActiveInstanceAsync(companyId.Value);
            if (instance == null)
            {
                return NotFound("No active WhatsApp instance");
            }

            // In a real implementation, you'd check WhatsApp API for contact status
            return Ok(new ContactStatusDto 
            { 
                PhoneNumber = phoneNumber, 
                IsWhatsAppUser = true, // This would be determined by the API
                LastSeen = null 
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting contact status for {PhoneNumber}", phoneNumber);
            return StatusCode(500, "Internal server error");
        }
    }

    private string ProcessTemplate(string template, Dictionary<string, string>? variables)
    {
        if (variables == null || !variables.Any())
            return template;

        var result = template;
        foreach (var variable in variables)
        {
            result = result.Replace($"{{{variable.Key}}}", variable.Value);
        }

        return result;
    }

    private Guid? GetUserId()
    {
        var userIdClaim = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
        return userIdClaim != null && Guid.TryParse(userIdClaim, out var userId) ? userId : null;
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = User.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }
}

public class SendTextMessageDto
{
    public Guid ConversationId { get; set; }
    public string PhoneNumber { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
}

public class SendTemplateMessageDto
{
    public Guid ConversationId { get; set; }
    public string PhoneNumber { get; set; } = string.Empty;
    public Guid? TemplateId { get; set; }
    public string TemplateContent { get; set; } = string.Empty;
    public Dictionary<string, string>? Variables { get; set; }
}

public class BroadcastMessageDto
{
    public List<string> PhoneNumbers { get; set; } = new();
    public string Message { get; set; } = string.Empty;
}

public class BroadcastResult
{
    public string PhoneNumber { get; set; } = string.Empty;
    public bool Success { get; set; }
    public string? Error { get; set; }
}

public class BroadcastSummaryDto
{
    public int TotalSent { get; set; }
    public int TotalFailed { get; set; }
    public List<BroadcastResult> Results { get; set; } = new();
}

public class ContactStatusDto
{
    public string PhoneNumber { get; set; } = string.Empty;
    public bool IsWhatsAppUser { get; set; }
    public DateTime? LastSeen { get; set; }
}