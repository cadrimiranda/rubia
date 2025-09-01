using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface IConversationMediaService
{
    Task<ConversationMediaDto?> GetByIdAsync(Guid mediaId);
    Task<IEnumerable<ConversationMediaDto>> GetByConversationAsync(Guid conversationId);
    Task<ConversationMediaDto> CreateAsync(CreateConversationMediaDto dto);
    Task<ConversationMediaDto> UploadFileAsync(IFormFile file, MediaType mediaType);
    Task<bool> DeleteAsync(Guid mediaId);
    Task<byte[]?> GetFileContentAsync(Guid mediaId);
    Task<(Stream? Stream, string? ContentType, string? FileName)> GetFileStreamAsync(Guid mediaId);
}