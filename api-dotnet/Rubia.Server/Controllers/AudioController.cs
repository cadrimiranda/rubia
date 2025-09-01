using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Repositories;
using Rubia.Server.Services;

namespace Rubia.Server.Controllers;

[ApiController]
[Route("api/audio")]
[Authorize]
public class AudioController : ControllerBase
{
    private readonly IAudioProcessingService _audioProcessingService;
    private readonly IAudioMessageRepository _audioMessageRepository;
    private readonly IAudioStorageService _audioStorageService;
    private readonly ILogger<AudioController> _logger;

    public AudioController(
        IAudioProcessingService audioProcessingService,
        IAudioMessageRepository audioMessageRepository,
        IAudioStorageService audioStorageService,
        ILogger<AudioController> logger)
    {
        _audioProcessingService = audioProcessingService;
        _audioMessageRepository = audioMessageRepository;
        _audioStorageService = audioStorageService;
        _logger = logger;
    }

    [HttpPost("send")]
    public async Task<IActionResult> SendAudio([FromBody] SendAudioRequestDto request)
    {
        try
        {
            if (string.IsNullOrEmpty(request.ToNumber) || string.IsNullOrEmpty(request.AudioUrl))
            {
                return BadRequest(new { error = "Missing required parameters: toNumber and audioUrl" });
            }

            var messageId = await _audioProcessingService.SendAudioAsync(request.ToNumber, request.AudioUrl);

            return Ok(new { status = "success", messageId });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending audio: {Error}", ex.Message);
            return StatusCode(500, new { error = ex.Message });
        }
    }

    [HttpGet("messages")]
    public async Task<ActionResult<IEnumerable<AudioMessageDto>>> ListMessages(
        [FromQuery] string? fromNumber,
        [FromQuery] ProcessingStatus? status,
        [FromQuery] int page = 0,
        [FromQuery] int size = 20)
    {
        try
        {
            IEnumerable<AudioMessage> messages;

            if (!string.IsNullOrEmpty(fromNumber))
            {
                messages = await _audioMessageRepository.GetByFromNumberAsync(fromNumber);
            }
            else if (status.HasValue)
            {
                messages = await _audioMessageRepository.GetByStatusAsync(status.Value);
            }
            else
            {
                messages = await _audioMessageRepository.GetAllAsync(page, size);
            }

            var dtos = messages.Select(MapToDto);
            return Ok(dtos);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error listing audio messages");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("messages/{id:guid}")]
    public async Task<ActionResult<AudioMessageDto>> GetMessage(Guid id)
    {
        try
        {
            var message = await _audioMessageRepository.GetByIdAsync(id);
            if (message == null)
            {
                return NotFound();
            }

            return Ok(MapToDto(message));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting audio message {Id}", id);
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpGet("messages/{id:guid}/download")]
    public async Task<IActionResult> DownloadAudio(Guid id)
    {
        try
        {
            var audioMessage = await _audioMessageRepository.GetByIdAsync(id);
            if (audioMessage == null)
            {
                return NotFound();
            }

            if (string.IsNullOrEmpty(audioMessage.FilePath))
            {
                return BadRequest(new { error = "Audio file not available" });
            }

            var (stream, contentType, fileName) = await _audioStorageService.RetrieveAsync(audioMessage.FilePath);
            if (stream == null)
            {
                return NotFound();
            }

            var downloadFileName = $"{audioMessage.MessageId}.ogg";
            var mimeType = audioMessage.MimeType ?? "audio/ogg";

            return File(stream, mimeType, downloadFileName);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error downloading audio {Id}", id);
            return StatusCode(500, new { error = "Failed to download audio file" });
        }
    }

    [HttpGet("stats")]
    public async Task<ActionResult<AudioStatsDto>> GetStats()
    {
        try
        {
            var totalMessages = await _audioMessageRepository.CountAsync();
            var receivedCount = await _audioMessageRepository.CountByStatusAsync(ProcessingStatus.RECEIVED);
            var processingCount = await _audioMessageRepository.CountByStatusAsync(ProcessingStatus.DOWNLOADING) +
                                 await _audioMessageRepository.CountByStatusAsync(ProcessingStatus.PROCESSING);
            var completedCount = await _audioMessageRepository.CountByStatusAsync(ProcessingStatus.COMPLETED);
            var failedCount = await _audioMessageRepository.CountByStatusAsync(ProcessingStatus.FAILED);

            var stats = new AudioStatsDto
            {
                Total = totalMessages,
                Received = receivedCount,
                Processing = processingCount,
                Completed = completedCount,
                Failed = failedCount
            };

            return Ok(stats);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting audio stats");
            return StatusCode(500, "Internal server error");
        }
    }

    [HttpDelete("messages/{id:guid}")]
    public async Task<IActionResult> DeleteAudioMessage(Guid id)
    {
        try
        {
            var audioMessage = await _audioMessageRepository.GetByIdAsync(id);
            if (audioMessage == null)
            {
                return NotFound();
            }

            // Delete file if exists
            if (!string.IsNullOrEmpty(audioMessage.FilePath))
            {
                await _audioStorageService.DeleteAsync(audioMessage.FilePath);
            }

            // Delete database record
            await _audioMessageRepository.DeleteAsync(id);

            return Ok(new { status = "deleted" });
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting audio message {Id}", id);
            return StatusCode(500, new { error = "Failed to delete audio message" });
        }
    }

    private static AudioMessageDto MapToDto(AudioMessage audioMessage)
    {
        return new AudioMessageDto
        {
            Id = audioMessage.Id,
            MessageId = audioMessage.MessageId,
            FromNumber = audioMessage.FromNumber,
            ToNumber = audioMessage.ToNumber,
            Direction = audioMessage.Direction,
            AudioUrl = audioMessage.AudioUrl,
            FilePath = audioMessage.FilePath,
            MimeType = audioMessage.MimeType,
            DurationSeconds = audioMessage.DurationSeconds,
            FileSizeBytes = audioMessage.FileSizeBytes,
            Status = audioMessage.Status,
            CreatedAt = audioMessage.CreatedAt,
            ProcessedAt = audioMessage.ProcessedAt,
            ErrorMessage = audioMessage.ErrorMessage,
            ConversationId = audioMessage.ConversationId
        };
    }
}