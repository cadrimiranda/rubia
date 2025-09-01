using Rubia.Server.DTOs;

namespace Rubia.Server.Services.Interfaces;

public interface IOpenAIService
{
    Task<string> GenerateResponseAsync(string prompt, string context = "", CancellationToken cancellationToken = default);
    Task<string> GenerateEnhancedMessageAsync(string originalMessage, string customerContext = "", CancellationToken cancellationToken = default);
    Task<bool> ShouldAutoRespondAsync(string message, string conversationContext = "", CancellationToken cancellationToken = default);
    Task<string> GenerateAutoResponseAsync(string incomingMessage, string conversationHistory = "", CancellationToken cancellationToken = default);
    Task<MessageDto> ProcessIncomingMessageAsync(MessageDto message, long conversationId, CancellationToken cancellationToken = default);
    Task<string> AnalyzeMessageSentimentAsync(string message, CancellationToken cancellationToken = default);
    Task<List<string>> ExtractKeywordsAsync(string message, CancellationToken cancellationToken = default);
    Task<string> TranslateMessageAsync(string message, string targetLanguage, CancellationToken cancellationToken = default);
}