using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Security.Claims;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/whatsapp")]
[Authorize]
public class WhatsAppController : ControllerBase
{
    private readonly IWhatsAppService _whatsAppService;
    private readonly ILogger<WhatsAppController> _logger;

    public WhatsAppController(IWhatsAppService whatsAppService, ILogger<WhatsAppController> logger)
    {
        _whatsAppService = whatsAppService;
        _logger = logger;
    }

    [HttpGet("status")]
    public async Task<ActionResult<ZApiStatusDto>> GetStatus()
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
                return NotFound("No active WhatsApp instance found");
            }

            var status = await _whatsAppService.GetInstanceStatusAsync(instance.InstanceId, instance.Token);
            return Ok(status);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting WhatsApp status");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("qr-code")]
    public async Task<ActionResult<QRCodeResponseDto>> GenerateQRCode()
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
                return NotFound("No active WhatsApp instance found");
            }

            var qrCode = await _whatsAppService.GenerateQRCodeAsync(instance.InstanceId, instance.Token);
            
            return Ok(new QRCodeResponseDto
            {
                QRCode = qrCode,
                Status = qrCode != null ? "success" : "error"
            });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error generating QR code");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("restart")]
    public async Task<IActionResult> RestartInstance()
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
                return NotFound("No active WhatsApp instance found");
            }

            var success = await _whatsAppService.RestartInstanceAsync(instance.InstanceId, instance.Token);
            
            if (success)
            {
                return Ok(new { message = "Instance restarted successfully" });
            }

            return BadRequest(new { message = "Failed to restart instance" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error restarting WhatsApp instance");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpPost("send-message")]
    public async Task<IActionResult> SendMessage([FromBody] SendMessageDto dto)
    {
        try
        {
            var companyId = GetCompanyId();
            if (!companyId.HasValue)
            {
                return BadRequest("Company context not found");
            }

            var success = await _whatsAppService.SendTextMessageAsync(dto.PhoneNumber, dto.Message, companyId.Value);
            
            if (success)
            {
                return Ok(new { message = "Message sent successfully" });
            }

            return BadRequest(new { message = "Failed to send message" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending WhatsApp message");
            return StatusCode(500, "Internal server error");
        }
    }

    private Guid? GetCompanyId()
    {
        var companyIdClaim = User.FindFirst("companyId")?.Value;
        return companyIdClaim != null && Guid.TryParse(companyIdClaim, out var companyId) ? companyId : null;
    }
}

public class SendMessageDto
{
    public string PhoneNumber { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
}