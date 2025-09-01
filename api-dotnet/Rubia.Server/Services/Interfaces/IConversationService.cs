using Rubia.Server.DTOs;
using Rubia.Server.Enums;

namespace Rubia.Server.Services.Interfaces;

public interface IConversationService
{
    Task<IEnumerable<ConversationDto>> GetConversationsByStatusAsync(Guid companyId, ConversationStatus status);
    Task<ConversationDto?> GetByIdAsync(Guid conversationId);
    Task<ConversationDto> CreateAsync(CreateConversationDto dto);
    Task<ConversationDto?> UpdateStatusAsync(Guid conversationId, ConversationStatus status);
    Task<ConversationDto?> AssignUserAsync(Guid conversationId, Guid? userId);
}