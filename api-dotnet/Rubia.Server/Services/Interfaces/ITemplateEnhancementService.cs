using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface ITemplateEnhancementService
{
    Task<EnhancedTemplateResponseDto> EnhanceTemplateAsync(EnhanceTemplateDto request);
    Task<MessageTemplateRevisionDto> SaveTemplateWithAIMetadataAsync(SaveTemplateWithAiMetadataDto request);
}