using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.Entities;
using Rubia.Server.Services;
using System.Text.Json;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/zapi/activation")]
[Authorize]
public class ZApiActivationController : ControllerBase
{
    private readonly ZApiActivationService _activationService;
    private readonly ILogger<ZApiActivationController> _logger;

    public ZApiActivationController(
        ZApiActivationService activationService,
        ILogger<ZApiActivationController> logger)
    {
        _activationService = activationService;
        _logger = logger;
    }

    [HttpGet("status")]
    public async Task<ActionResult<ZApiStatus>> GetStatus()
    {
        var status = await _activationService.GetInstanceStatusAsync();
        return Ok(status);
    }

    [HttpGet("qr-code/bytes")]
    public async Task<IActionResult> GetQrCodeBytes()
    {
        var result = await _activationService.GetQrCodeBytesAsync();

        if (result.Success && result.Data is byte[] bytes)
        {
            Response.Headers.Add("Content-Disposition", "attachment; filename=qrcode.png");
            return File(bytes, "image/png");
        }

        return BadRequest(new { error = result.Error });
    }

    [HttpGet("qr-code/image")]
    public async Task<ActionResult<QrCodeResult>> GetQrCodeImage()
    {
        var result = await _activationService.GetQrCodeImageAsync();
        return Ok(result);
    }

    [HttpGet("phone-code/{phone}")]
    public async Task<ActionResult<PhoneCodeResult>> GetPhoneCode(string phone)
    {
        var result = await _activationService.GetPhoneCodeAsync(phone);
        return Ok(result);
    }

    [HttpPost("restart")]
    public async Task<ActionResult<object>> RestartInstance()
    {
        var success = await _activationService.RestartInstanceAsync();

        return Ok(new
        {
            success,
            message = success ? "Instance restarted successfully" : "Failed to restart instance"
        });
    }

    [HttpPost("disconnect")]
    public async Task<ActionResult<object>> DisconnectInstance()
    {
        var success = await _activationService.DisconnectInstanceAsync();

        return Ok(new
        {
            success,
            message = success ? "Instance disconnected successfully" : "Failed to disconnect instance"
        });
    }

    [HttpPost("webhook/connected")]
    public ActionResult<string> HandleConnectedWebhook([FromBody] Dictionary<string, object> payload)
    {
        try
        {
            _logger.LogInformation("Z-API instance connected: {Payload}", 
                JsonSerializer.Serialize(payload));

            if (payload.TryGetValue("instanceId", out var instanceIdObj) &&
                payload.TryGetValue("connected", out var connectedObj))
            {
                var instanceId = instanceIdObj?.ToString();
                var connected = connectedObj is bool connectedBool && connectedBool;

                if (connected)
                {
                    _logger.LogInformation("Instance {InstanceId} successfully connected to WhatsApp", instanceId);
                }
            }

            return Ok("OK");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error processing connected webhook: {Message}", e.Message);
            return BadRequest($"Error: {e.Message}");
        }
    }

    [HttpPost("webhook/disconnected")]
    public ActionResult<string> HandleDisconnectedWebhook([FromBody] Dictionary<string, object> payload)
    {
        try
        {
            _logger.LogInformation("Z-API instance disconnected: {Payload}", 
                JsonSerializer.Serialize(payload));

            if (payload.TryGetValue("instanceId", out var instanceIdObj) &&
                payload.TryGetValue("error", out var errorObj))
            {
                var instanceId = instanceIdObj?.ToString();
                var error = errorObj?.ToString();

                _logger.LogWarning("Instance {InstanceId} disconnected. Error: {Error}", instanceId, error);
            }

            return Ok("OK");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error processing disconnected webhook: {Message}", e.Message);
            return BadRequest($"Error: {e.Message}");
        }
    }
}