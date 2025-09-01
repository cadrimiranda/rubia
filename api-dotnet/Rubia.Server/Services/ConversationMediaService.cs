using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class ConversationMediaService : IConversationMediaService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<ConversationMediaService> _logger;

    public ConversationMediaService(RubiaDbContext context, ILogger<ConversationMediaService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<ConversationMediaDto?> GetByIdAsync(Guid mediaId)
    {
        var media = await _context.ConversationMedia.FindAsync(mediaId);
        return media != null ? MapToDto(media) : null;
    }

    public async Task<IEnumerable<ConversationMediaDto>> GetByConversationAsync(Guid conversationId)
    {
        var mediaList = await _context.ConversationMedia
            .Where(cm => cm.Messages.Any(m => m.ConversationId == conversationId))
            .OrderByDescending(cm => cm.CreatedAt)
            .ToListAsync();

        return mediaList.Select(MapToDto);
    }

    public async Task<ConversationMediaDto> CreateAsync(CreateConversationMediaDto dto)
    {
        var media = new ConversationMedia
        {
            Id = Guid.NewGuid(),
            FileName = dto.FileName,
            FileUrl = dto.FileUrl,
            MediaType = dto.MediaType,
            FileSize = dto.FileSize,
            MimeType = dto.MimeType,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.ConversationMedia.Add(media);
        await _context.SaveChangesAsync();

        return MapToDto(media);
    }

    public async Task<ConversationMediaDto> UploadFileAsync(IFormFile file, MediaType mediaType)
    {
        // Validate file
        if (file == null || file.Length == 0)
            throw new ArgumentException("File is required");

        // Define upload directory
        var uploadsDir = Path.Combine("wwwroot", "uploads", "media");
        Directory.CreateDirectory(uploadsDir);

        // Generate unique filename
        var fileExtension = Path.GetExtension(file.FileName);
        var uniqueFileName = $"{Guid.NewGuid()}{fileExtension}";
        var filePath = Path.Combine(uploadsDir, uniqueFileName);

        // Save file
        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await file.CopyToAsync(stream);
        }

        // Create media record
        var media = new ConversationMedia
        {
            Id = Guid.NewGuid(),
            FileName = file.FileName,
            FileUrl = $"/uploads/media/{uniqueFileName}",
            MediaType = mediaType,
            FileSize = file.Length,
            MimeType = file.ContentType,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.ConversationMedia.Add(media);
        await _context.SaveChangesAsync();

        return MapToDto(media);
    }

    public async Task<bool> DeleteAsync(Guid mediaId)
    {
        var media = await _context.ConversationMedia.FindAsync(mediaId);
        if (media == null)
            return false;

        // Delete physical file
        try
        {
            var fullPath = Path.Combine("wwwroot", media.FileUrl.TrimStart('/'));
            if (File.Exists(fullPath))
            {
                File.Delete(fullPath);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to delete physical file: {FileUrl}", media.FileUrl);
        }

        // Delete database record
        _context.ConversationMedia.Remove(media);
        await _context.SaveChangesAsync();

        return true;
    }

    public async Task<byte[]?> GetFileContentAsync(Guid mediaId)
    {
        var media = await _context.ConversationMedia.FindAsync(mediaId);
        if (media == null)
            return null;

        try
        {
            var fullPath = Path.Combine("wwwroot", media.FileUrl.TrimStart('/'));
            if (File.Exists(fullPath))
            {
                return await File.ReadAllBytesAsync(fullPath);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error reading file content for media: {MediaId}", mediaId);
        }

        return null;
    }

    public async Task<(Stream? Stream, string? ContentType, string? FileName)> GetFileStreamAsync(Guid mediaId)
    {
        var media = await _context.ConversationMedia.FindAsync(mediaId);
        if (media == null)
            return (null, null, null);

        try
        {
            var fullPath = Path.Combine("wwwroot", media.FileUrl.TrimStart('/'));
            if (File.Exists(fullPath))
            {
                var stream = new FileStream(fullPath, FileMode.Open, FileAccess.Read);
                return (stream, media.MimeType, media.FileName);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error opening file stream for media: {MediaId}", mediaId);
        }

        return (null, null, null);
    }

    private static ConversationMediaDto MapToDto(ConversationMedia media)
    {
        return new ConversationMediaDto
        {
            Id = media.Id,
            FileName = media.FileName,
            FileUrl = media.FileUrl,
            MediaType = media.MediaType,
            FileSize = media.FileSize,
            MimeType = media.MimeType,
            CreatedAt = media.CreatedAt,
            UpdatedAt = media.UpdatedAt
        };
    }
}