using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Repositories;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public interface IAudioProcessingService
{
    Task<AudioMessage> ProcessIncomingAudioAsync(string messageId, string fromNumber, string audioUrl, string? mimeType, int? durationSeconds);
    Task<string> SendAudioAsync(string toNumber, string audioUrl);
    Task ProcessAudioAsync(AudioMessage audioMessage);
    Task<bool> HasAudioMessageAsync(string messageId);
    Task<AudioMessage?> GetAudioMessageAsync(string messageId);
}

public class AudioProcessingService : IAudioProcessingService
{
    private readonly IAudioMessageRepository _audioMessageRepository;
    private readonly IAudioStorageService _audioStorageService;
    private readonly IWhatsAppService _whatsAppService;
    private readonly ILogger<AudioProcessingService> _logger;
    private readonly HttpClient _httpClient;

    public AudioProcessingService(
        IAudioMessageRepository audioMessageRepository,
        IAudioStorageService audioStorageService,
        IWhatsAppService whatsAppService,
        ILogger<AudioProcessingService> logger,
        HttpClient httpClient)
    {
        _audioMessageRepository = audioMessageRepository;
        _audioStorageService = audioStorageService;
        _whatsAppService = whatsAppService;
        _logger = logger;
        _httpClient = httpClient;
    }

    public async Task<AudioMessage> ProcessIncomingAudioAsync(string messageId, string fromNumber, string audioUrl, string? mimeType, int? durationSeconds)
    {
        if (await _audioMessageRepository.ExistsByMessageIdAsync(messageId))
        {
            _logger.LogInformation("Audio message already processed: {MessageId}", messageId);
            return await _audioMessageRepository.GetByMessageIdAsync(messageId) ?? throw new InvalidOperationException("Message exists but cannot be retrieved");
        }

        var audioMessage = new AudioMessage
        {
            Id = Guid.NewGuid(),
            MessageId = messageId,
            FromNumber = fromNumber,
            Direction = MessageDirection.INCOMING,
            AudioUrl = audioUrl,
            MimeType = mimeType,
            DurationSeconds = durationSeconds,
            Status = ProcessingStatus.RECEIVED
        };

        audioMessage = await _audioMessageRepository.CreateAsync(audioMessage);
        _logger.LogInformation("Audio message saved: {MessageId} from {FromNumber}", messageId, fromNumber);

        // Process asynchronously
        _ = Task.Run(async () => await ProcessAudioAsync(audioMessage));

        return audioMessage;
    }

    public async Task<string> SendAudioAsync(string toNumber, string audioUrl)
    {
        try
        {
            // Use WhatsApp service to send audio - this would need to be implemented in WhatsAppService
            // For now, we'll create a placeholder messageId
            var messageId = Guid.NewGuid().ToString();

            var audioMessage = new AudioMessage
            {
                Id = Guid.NewGuid(),
                MessageId = messageId,
                ToNumber = toNumber,
                Direction = MessageDirection.OUTGOING,
                AudioUrl = audioUrl,
                Status = ProcessingStatus.RECEIVED
            };

            audioMessage = await _audioMessageRepository.CreateAsync(audioMessage);
            _logger.LogInformation("Outgoing audio message saved: {MessageId}", messageId);

            // Process asynchronously
            _ = Task.Run(async () => await ProcessAudioAsync(audioMessage));

            return messageId;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error sending audio to {ToNumber}: {Error}", toNumber, ex.Message);
            throw new Exception("Failed to send audio", ex);
        }
    }

    public async Task ProcessAudioAsync(AudioMessage audioMessage)
    {
        try
        {
            _logger.LogInformation("Starting async processing for audio: {MessageId}", audioMessage.MessageId);

            audioMessage.Status = ProcessingStatus.DOWNLOADING;
            await _audioMessageRepository.UpdateAsync(audioMessage);

            // Download audio from URL
            using var response = await _httpClient.GetAsync(audioMessage.AudioUrl);
            response.EnsureSuccessStatusCode();

            using var audioStream = await response.Content.ReadAsStreamAsync();
            
            var fileName = $"{audioMessage.MessageId}.ogg";
            var filePath = await _audioStorageService.StoreAsync(fileName, audioStream, audioMessage.MimeType);

            var fileSize = await _audioStorageService.GetFileSizeAsync(filePath);

            audioMessage.FilePath = filePath;
            audioMessage.FileSizeBytes = fileSize;
            audioMessage.Status = ProcessingStatus.COMPLETED;
            audioMessage.ProcessedAt = DateTime.UtcNow;

            _logger.LogInformation("Audio processing completed: {MessageId} ({FileSize} bytes)", audioMessage.MessageId, fileSize);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing audio {MessageId}: {Error}", audioMessage.MessageId, ex.Message);
            audioMessage.Status = ProcessingStatus.FAILED;
            audioMessage.ErrorMessage = ex.Message;
        }

        await _audioMessageRepository.UpdateAsync(audioMessage);
    }

    public async Task<bool> HasAudioMessageAsync(string messageId)
    {
        return await _audioMessageRepository.ExistsByMessageIdAsync(messageId);
    }

    public async Task<AudioMessage?> GetAudioMessageAsync(string messageId)
    {
        return await _audioMessageRepository.GetByMessageIdAsync(messageId);
    }
}