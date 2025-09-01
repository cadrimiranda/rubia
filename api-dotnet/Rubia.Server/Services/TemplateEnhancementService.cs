using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.DTOs;
using Rubia.Server.Entities;
using Rubia.Server.Enums;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class TemplateEnhancementService : ITemplateEnhancementService
{
    private readonly RubiaDbContext _context;
    private readonly ILogger<TemplateEnhancementService> _logger;
    private readonly HttpClient _httpClient;

    public TemplateEnhancementService(
        RubiaDbContext context, 
        ILogger<TemplateEnhancementService> logger,
        HttpClient httpClient)
    {
        _context = context;
        _logger = logger;
        _httpClient = httpClient;
    }

    public async Task<EnhancedTemplateResponseDto> EnhanceTemplateAsync(EnhanceTemplateDto request)
    {
        _logger.LogInformation("Enhancing template for company: {CompanyId}", request.CompanyId);

        try
        {
            // Simulate AI enhancement process
            // In a real implementation, this would call an AI service like OpenAI
            var enhancedContent = await SimulateAIEnhancement(request.OriginalContent, request.EnhancementType);

            var response = new EnhancedTemplateResponseDto
            {
                OriginalContent = request.OriginalContent,
                EnhancedContent = enhancedContent,
                EnhancementType = request.EnhancementType,
                AiExplanation = GenerateAIExplanation(request.EnhancementType),
                TokensUsed = CalculateTokensUsed(request.OriginalContent, enhancedContent),
                CreditsConsumed = 1, // Simulate credit consumption
                ModelUsed = "gpt-4o-mini",
                EnhancedAt = DateTime.UtcNow
            };

            return response;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error enhancing template: {Error}", ex.Message);
            throw new Exception("Failed to enhance template", ex);
        }
    }

    public async Task<MessageTemplateRevisionDto> SaveTemplateWithAIMetadataAsync(SaveTemplateWithAiMetadataDto request)
    {
        _logger.LogInformation("Saving template with AI metadata for template: {TemplateId}", request.TemplateId);

        try
        {
            var template = await _context.MessageTemplates.FindAsync(request.TemplateId);
            if (template == null)
            {
                throw new ArgumentException("Template not found");
            }

            // Get the next revision number
            var lastRevision = await _context.MessageTemplateRevisions
                .Where(r => r.TemplateId == request.TemplateId)
                .OrderByDescending(r => r.RevisionNumber)
                .FirstOrDefaultAsync();

            var nextRevisionNumber = (lastRevision?.RevisionNumber ?? 0) + 1;

            // Create new revision
            var revision = new MessageTemplateRevision
            {
                Id = Guid.NewGuid(),
                TemplateId = request.TemplateId,
                RevisionNumber = nextRevisionNumber,
                Content = request.EnhancedContent,
                RevisionType = RevisionType.AI_ENHANCED,
                RevisionTimestamp = DateTime.UtcNow,
                AiAgentId = request.AiAgentId,
                AiEnhancementType = request.EnhancementType,
                AiTokensUsed = request.TokensUsed,
                AiCreditsConsumed = request.CreditsConsumed,
                AiModelUsed = request.ModelUsed,
                AiExplanation = request.AiExplanation,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.MessageTemplateRevisions.Add(revision);

            // Update template content
            template.Content = request.EnhancedContent;
            template.UpdatedAt = DateTime.UtcNow;

            await _context.SaveChangesAsync();

            return MapToDto(revision, template.Name);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error saving template with AI metadata: {Error}", ex.Message);
            throw;
        }
    }

    private async Task<string> SimulateAIEnhancement(string originalContent, string enhancementType)
    {
        // Simulate AI processing delay
        await Task.Delay(100);

        return enhancementType.ToLower() switch
        {
            "friendly" => $"Olá! {originalContent} 😊 Tenha um ótimo dia!",
            "professional" => $"Prezado(a) cliente, {originalContent} Atenciosamente,",
            "empathetic" => $"Entendemos sua situação. {originalContent} Estamos aqui para ajudar.",
            "urgent" => $"🚨 URGENTE: {originalContent} Por favor, responda o mais rápido possível.",
            "motivational" => $"✨ {originalContent} Você consegue! Estamos torcendo por você!",
            _ => $"[{enhancementType.ToUpper()}] {originalContent}"
        };
    }

    private string GenerateAIExplanation(string enhancementType)
    {
        return enhancementType.ToLower() switch
        {
            "friendly" => "Adicionei elementos calorosos e um emoji para tornar a mensagem mais acolhedora e amigável.",
            "professional" => "Estruturei a mensagem com linguagem formal e saudações profissionais adequadas ao ambiente corporativo.",
            "empathetic" => "Incluí palavras de compreensão e apoio para demonstrar empatia com a situação do cliente.",
            "urgent" => "Adicionei indicadores visuais e linguagem que transmite urgência de forma clara e respeitosa.",
            "motivational" => "Incorporei elementos motivacionais e encorajadores para inspirar o destinatário.",
            _ => $"Aplicado estilo de comunicação: {enhancementType}"
        };
    }

    private int CalculateTokensUsed(string originalContent, string enhancedContent)
    {
        // Simple token estimation (roughly 1 token per 4 characters)
        var totalChars = originalContent.Length + enhancedContent.Length;
        return (int)Math.Ceiling(totalChars / 4.0);
    }

    private static MessageTemplateRevisionDto MapToDto(MessageTemplateRevision revision, string templateName)
    {
        return new MessageTemplateRevisionDto
        {
            Id = revision.Id,
            TemplateId = revision.TemplateId,
            TemplateName = templateName,
            RevisionNumber = revision.RevisionNumber,
            Content = revision.Content,
            EditedByUserId = revision.EditedByUserId,
            RevisionType = revision.RevisionType,
            RevisionTimestamp = revision.RevisionTimestamp,
            CreatedAt = revision.CreatedAt,
            UpdatedAt = revision.UpdatedAt,
            AiAgentId = revision.AiAgentId,
            AiEnhancementType = revision.AiEnhancementType,
            AiTokensUsed = revision.AiTokensUsed,
            AiCreditsConsumed = revision.AiCreditsConsumed,
            AiModelUsed = revision.AiModelUsed,
            AiExplanation = revision.AiExplanation
        };
    }
}