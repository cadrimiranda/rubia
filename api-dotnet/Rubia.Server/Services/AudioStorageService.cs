using Microsoft.Extensions.Options;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class AudioStorageOptions
{
    public string Path { get; set; } = "wwwroot/uploads/audio";
    public int MaxSizeMb { get; set; } = 16;
}

public interface IAudioStorageService
{
    Task<string> StoreAsync(string fileName, Stream audioStream, string? mimeType);
    Task<(Stream? Stream, string? ContentType, string? FileName)> RetrieveAsync(string filePath);
    Task<bool> DeleteAsync(string filePath);
    Task<bool> ExistsAsync(string filePath);
    Task<long> GetFileSizeAsync(string filePath);
}

public class AudioStorageService : IAudioStorageService
{
    private readonly AudioStorageOptions _options;
    private readonly ILogger<AudioStorageService> _logger;

    public AudioStorageService(IOptions<AudioStorageOptions> options, ILogger<AudioStorageService> logger)
    {
        _options = options.Value;
        _logger = logger;
        
        Directory.CreateDirectories(_options.Path);
        _logger.LogInformation("Audio storage directory created: {Path}", _options.Path);
    }

    public async Task<string> StoreAsync(string fileName, Stream audioStream, string? mimeType)
    {
        var uniqueFileName = $"{Guid.NewGuid()}_{SanitizeFileName(fileName)}";
        var filePath = Path.Combine(_options.Path, uniqueFileName);

        try
        {
            using (var fileStream = new FileStream(filePath, FileMode.Create))
            {
                await audioStream.CopyToAsync(fileStream);
            }

            var fileInfo = new FileInfo(filePath);
            var maxSizeBytes = _options.MaxSizeMb * 1024 * 1024;

            if (fileInfo.Length > maxSizeBytes)
            {
                File.Delete(filePath);
                throw new ArgumentException($"Audio file too large. Max size: {_options.MaxSizeMb}MB");
            }

            _logger.LogInformation("Audio file saved: {FilePath} ({Size} bytes)", filePath, fileInfo.Length);
            return filePath;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error saving audio file: {FileName}", fileName);
            if (File.Exists(filePath))
            {
                File.Delete(filePath);
            }
            throw new Exception("Failed to save audio file", ex);
        }
    }

    public async Task<(Stream? Stream, string? ContentType, string? FileName)> RetrieveAsync(string filePath)
    {
        try
        {
            if (File.Exists(filePath))
            {
                var stream = new FileStream(filePath, FileMode.Open, FileAccess.Read);
                var fileName = Path.GetFileName(filePath);
                var contentType = GetContentType(Path.GetExtension(filePath));
                return (stream, contentType, fileName);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error retrieving file: {FilePath}", filePath);
        }

        return (null, null, null);
    }

    public async Task<bool> DeleteAsync(string filePath)
    {
        try
        {
            if (File.Exists(filePath))
            {
                File.Delete(filePath);
                return true;
            }
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error deleting file: {FilePath}", filePath);
            return false;
        }
    }

    public async Task<bool> ExistsAsync(string filePath)
    {
        return File.Exists(filePath);
    }

    public async Task<long> GetFileSizeAsync(string filePath)
    {
        try
        {
            if (File.Exists(filePath))
            {
                var fileInfo = new FileInfo(filePath);
                return fileInfo.Length;
            }
            return 0;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error getting file size: {FilePath}", filePath);
            return 0;
        }
    }

    private string SanitizeFileName(string fileName)
    {
        if (string.IsNullOrEmpty(fileName)) 
            return "audio.ogg";
        
        return Path.GetInvalidFileNameChars()
            .Aggregate(fileName, (current, c) => current.Replace(c, '_'));
    }

    private string GetContentType(string extension)
    {
        return extension.ToLower() switch
        {
            ".ogg" => "audio/ogg",
            ".mp3" => "audio/mpeg",
            ".wav" => "audio/wav",
            ".m4a" => "audio/m4a",
            ".aac" => "audio/aac",
            _ => "audio/ogg"
        };
    }
}