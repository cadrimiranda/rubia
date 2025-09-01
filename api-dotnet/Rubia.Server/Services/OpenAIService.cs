using Microsoft.Extensions.Configuration;
using Rubia.Server.DTOs;
using Rubia.Server.Services.Interfaces;
using System.Text;
using System.Text.Json;

namespace Rubia.Server.Services;

public class OpenAIService : IOpenAIService
{
    private readonly HttpClient _httpClient;
    private readonly IConfiguration _configuration;
    private readonly ILogger<OpenAIService> _logger;
    private readonly IMessageService? _messageService;
    private readonly string _apiKey;
    private readonly string _baseUrl;

    public OpenAIService(
        HttpClient httpClient,
        IConfiguration configuration,
        ILogger<OpenAIService> logger,
        IMessageService? messageService = null)
    {
        _httpClient = httpClient;
        _configuration = configuration;
        _logger = logger;
        _messageService = messageService;
        _apiKey = _configuration["OPENAI_API_KEY"] ?? _configuration["OpenAI:ApiKey"] ?? throw new InvalidOperationException("OpenAI API Key not configured");
        _baseUrl = _configuration["OPENAI_BASE_URL"] ?? "https://api.openai.com/v1";

        _httpClient.DefaultRequestHeaders.Authorization = 
            new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", _apiKey);
    }

    public async Task<string> EnhanceTemplateAsync(string prompt, string? modelName = null, double? temperature = null, int? maxTokens = null)
    {
        try
        {
            var result = await EnhanceTemplateWithPayloadAsync(prompt, modelName, temperature, maxTokens);
            return result.EnhancedMessage;
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error enhancing template: {Message}", e.Message);
            return $"Erro ao processar com OpenAI: {e.Message}";
        }
    }

    public async Task<AIEnhancementResult> EnhanceTemplateWithPayloadAsync(
        string prompt, 
        string? modelName = null, 
        double? temperature = null, 
        int? maxTokens = null)
    {
        try
        {
            var finalModel = modelName ?? _configuration["OpenAI:DefaultModel"] ?? "gpt-4o-mini";
            var finalTemperature = temperature ?? double.Parse(_configuration["OpenAI:DefaultTemperature"] ?? "0.7");
            var finalMaxTokens = maxTokens ?? int.Parse(_configuration["OpenAI:DefaultMaxTokens"] ?? "150");

            _logger.LogDebug("Enhancing template with OpenAI - Model: {Model}, Temperature: {Temperature}, MaxTokens: {MaxTokens}",
                finalModel, finalTemperature, finalMaxTokens);

            var systemMessage = "Você é um especialista em captação de doadores de sangue para centros de hematologia e hemoterapia. " +
                "Sua especialidade é criar mensagens persuasivas e eficazes que motivem pessoas a fazer doações de sangue. " +
                "Sempre mantenha um tom ético, respeitoso e focado no impacto social positivo da doação.";

            var requestPayload = new
            {
                model = finalModel,
                temperature = finalTemperature,
                max_tokens = finalMaxTokens,
                messages = new[]
                {
                    new { role = "system", content = systemMessage },
                    new { role = "user", content = prompt }
                }
            };

            var fullPayloadJson = JsonSerializer.Serialize(requestPayload, new JsonSerializerOptions { WriteIndented = true });
            var requestContent = new StringContent(JsonSerializer.Serialize(requestPayload), Encoding.UTF8, "application/json");

            var stopwatch = System.Diagnostics.Stopwatch.StartNew();
            var response = await _httpClient.PostAsync("https://api.openai.com/v1/chat/completions", requestContent);
            stopwatch.Stop();

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError("OpenAI API error: {StatusCode} - {Content}", response.StatusCode, errorContent);
                throw new Exception($"OpenAI API error: {response.StatusCode}");
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var openAiResponse = JsonSerializer.Deserialize<OpenAIResponse>(responseContent);

            if (openAiResponse?.Choices?.Length > 0 && !string.IsNullOrEmpty(openAiResponse.Choices[0].Message?.Content))
            {
                var enhancedContent = openAiResponse.Choices[0].Message.Content;
                var tokensUsed = openAiResponse.Usage?.TotalTokens;

                _logger.LogInformation("Template enhanced successfully using model: {Model} - Tokens used: {Tokens}",
                    finalModel, tokensUsed);

                return new AIEnhancementResult
                {
                    EnhancedMessage = enhancedContent,
                    SystemMessage = systemMessage,
                    UserMessage = prompt,
                    FullPayloadJson = fullPayloadJson,
                    ModelUsed = finalModel,
                    TemperatureUsed = finalTemperature,
                    MaxTokensUsed = finalMaxTokens,
                    TokensUsed = tokensUsed,
                    ResponseTimeMs = stopwatch.ElapsedMilliseconds
                };
            }

            _logger.LogWarning("OpenAI response contained no content");
            throw new Exception("Resposta vazia da OpenAI");
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error calling OpenAI API: {Message}", e.Message);
            throw new Exception($"Erro ao processar com OpenAI: {e.Message}");
        }
    }

    public async Task<string?> TranscribeAudioAsync(byte[] audioData, string language = "pt")
    {
        try
        {
            _logger.LogDebug("Transcribing audio with OpenAI Whisper - Language: {Language}, Size: {Size} bytes",
                language, audioData.Length);

            using var formContent = new MultipartFormDataContent();
            formContent.Add(new ByteArrayContent(audioData), "file", "audio.ogg");
            formContent.Add(new StringContent("whisper-1"), "model");
            formContent.Add(new StringContent(language), "language");
            formContent.Add(new StringContent("json"), "response_format");

            var response = await _httpClient.PostAsync("https://api.openai.com/v1/audio/transcriptions", formContent);

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                _logger.LogError("OpenAI Whisper API error: {StatusCode} - {Content}", response.StatusCode, errorContent);
                return null;
            }

            var responseContent = await response.Content.ReadAsStringAsync();
            var transcriptionResponse = JsonSerializer.Deserialize<TranscriptionResponse>(responseContent);

            if (!string.IsNullOrEmpty(transcriptionResponse?.Text))
            {
                _logger.LogInformation("Audio transcribed successfully: '{Text}'", transcriptionResponse.Text);
                return transcriptionResponse.Text;
            }

            _logger.LogWarning("OpenAI Whisper response was empty");
            return null;
        }
        catch (Exception e)
        {
            _logger.LogError(e, "Error transcribing audio with OpenAI Whisper: {Message}", e.Message);
            return null;
        }
    }

    public int EstimateTokens(string text)
    {
        return text.Length / 4;
    }

    public async Task<bool> IsServiceAvailableAsync()
    {
        try
        {
            await EnhanceTemplateAsync("test");
            return true;
        }
        catch (Exception e)
        {
            _logger.LogWarning("OpenAI service is not available: {Message}", e.Message);
            return false;
        }
    }

    // Implementação da interface IOpenAIService
    public async Task<string> GenerateResponseAsync(string prompt, string context = "", CancellationToken cancellationToken = default)
    {
        try
        {
            var requestBody = new
            {
                model = _configuration["OPENAI_MODEL"] ?? "gpt-3.5-turbo",
                messages = new[]
                {
                    new { role = "system", content = context },
                    new { role = "user", content = prompt }
                }.Where(m => !string.IsNullOrEmpty(m.content)),
                max_tokens = int.Parse(_configuration["OPENAI_MAX_TOKENS"] ?? "1000"),
                temperature = double.Parse(_configuration["OPENAI_TEMPERATURE"] ?? "0.7")
            };

            var json = JsonSerializer.Serialize(requestBody);
            var content = new StringContent(json, Encoding.UTF8, "application/json");

            var response = await _httpClient.PostAsync($"{_baseUrl}/chat/completions", content, cancellationToken);
            response.EnsureSuccessStatusCode();

            var responseContent = await response.Content.ReadAsStringAsync(cancellationToken);
            var responseJson = JsonDocument.Parse(responseContent);
            
            return responseJson.RootElement
                .GetProperty("choices")[0]
                .GetProperty("message")
                .GetProperty("content")
                .GetString() ?? "";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error generating OpenAI response for prompt: {Prompt}", prompt);
            return "Desculpe, não foi possível gerar uma resposta no momento.";
        }
    }

    public async Task<string> GenerateEnhancedMessageAsync(string originalMessage, string customerContext = "", CancellationToken cancellationToken = default)
    {
        var context = $@"Você é um assistente especializado em melhorar mensagens de atendimento ao cliente.
        Contexto do cliente: {customerContext}
        
        Sua tarefa é melhorar a mensagem mantendo:
        1. O tom profissional e amigável
        2. A clareza da comunicação
        3. A personalização quando apropriada
        4. A linguagem em português brasileiro";

        var prompt = $"Melhore esta mensagem de atendimento: {originalMessage}";
        
        return await GenerateResponseAsync(prompt, context, cancellationToken);
    }

    public async Task<bool> ShouldAutoRespondAsync(string message, string conversationContext = "", CancellationToken cancellationToken = default)
    {
        var context = @"Você é um analisador de mensagens para decidir se uma resposta automática é apropriada.
        Responda apenas 'SIM' ou 'NAO'.
        
        Critérios para resposta automática:
        - Saudações simples
        - Perguntas sobre horário de funcionamento
        - Perguntas sobre localização
        - Solicitações de informações básicas
        
        NÃO responder automaticamente para:
        - Reclamações
        - Problemas complexos
        - Solicitações específicas
        - Mensagens emocionais";

        var prompt = $@"Contexto da conversa: {conversationContext}
        Mensagem recebida: {message}
        
        Deve responder automaticamente?";

        var response = await GenerateResponseAsync(prompt, context, cancellationToken);
        return response.Trim().ToUpper().Contains("SIM");
    }

    public async Task<string> GenerateAutoResponseAsync(string incomingMessage, string conversationHistory = "", CancellationToken cancellationToken = default)
    {
        var context = $@"Você é um assistente de atendimento ao cliente automatizado.
        Histórico da conversa: {conversationHistory}
        
        Regras:
        1. Seja sempre educado e profissional
        2. Use português brasileiro
        3. Mantenha respostas concisas (máximo 2 parágrafos)
        4. Se não souber responder, direcione para atendimento humano
        5. Use emojis moderadamente quando apropriado";

        var prompt = $"Responda esta mensagem de cliente: {incomingMessage}";
        
        return await GenerateResponseAsync(prompt, context, cancellationToken);
    }

    public async Task<MessageDto> ProcessIncomingMessageAsync(MessageDto message, long conversationId, CancellationToken cancellationToken = default)
    {
        if (_messageService == null)
        {
            _logger.LogWarning("MessageService not available for processing incoming message");
            return message;
        }

        try
        {
            var recentMessages = await _messageService.GetRecentMessagesAsync(conversationId, 10, cancellationToken);
            var conversationHistory = string.Join("\n", recentMessages.Select(m => $"{m.SenderType}: {m.Content}"));

            var shouldAutoRespond = await ShouldAutoRespondAsync(message.Content, conversationHistory, cancellationToken);

            if (shouldAutoRespond)
            {
                var autoResponse = await GenerateAutoResponseAsync(message.Content, conversationHistory, cancellationToken);
                
                var responseMessage = new MessageDto
                {
                    ConversationId = conversationId,
                    Content = autoResponse,
                    SenderType = "AI",
                    CreatedAt = DateTime.UtcNow
                };

                await _messageService.CreateAsync(responseMessage, cancellationToken);
                
                _logger.LogInformation("Auto-response generated for conversation {ConversationId}", conversationId);
            }

            message.Sentiment = await AnalyzeMessageSentimentAsync(message.Content, cancellationToken);
            var keywords = await ExtractKeywordsAsync(message.Content, cancellationToken);
            message.Keywords = string.Join(", ", keywords);

            return message;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error processing incoming message for conversation {ConversationId}", conversationId);
            return message;
        }
    }

    public async Task<string> AnalyzeMessageSentimentAsync(string message, CancellationToken cancellationToken = default)
    {
        var context = @"Você é um analisador de sentimentos. Analise o sentimento da mensagem e responda apenas uma palavra:
        - POSITIVO
        - NEGATIVO
        - NEUTRO";

        var prompt = $"Analise o sentimento desta mensagem: {message}";
        
        var response = await GenerateResponseAsync(prompt, context, cancellationToken);
        return response.Trim().ToUpper() switch
        {
            var s when s.Contains("POSITIVO") => "POSITIVO",
            var s when s.Contains("NEGATIVO") => "NEGATIVO",
            _ => "NEUTRO"
        };
    }

    public async Task<List<string>> ExtractKeywordsAsync(string message, CancellationToken cancellationToken = default)
    {
        var context = @"Você é um extrator de palavras-chave. Extraia até 5 palavras-chave mais importantes da mensagem.
        Responda apenas as palavras separadas por vírgula, sem explicações.";

        var prompt = $"Extraia palavras-chave desta mensagem: {message}";
        
        var response = await GenerateResponseAsync(prompt, context, cancellationToken);
        return response.Split(',', StringSplitOptions.RemoveEmptyEntries)
                      .Select(k => k.Trim())
                      .Where(k => !string.IsNullOrEmpty(k))
                      .Take(5)
                      .ToList();
    }

    public async Task<string> TranslateMessageAsync(string message, string targetLanguage, CancellationToken cancellationToken = default)
    {
        var context = $@"Você é um tradutor especializado. Traduza a mensagem para {targetLanguage}.
        Mantenha o tom e contexto originais.";

        var prompt = $"Traduza esta mensagem: {message}";
        
        return await GenerateResponseAsync(prompt, context, cancellationToken);
    }
}

public class AIEnhancementResult
{
    public string EnhancedMessage { get; set; } = string.Empty;
    public string SystemMessage { get; set; } = string.Empty;
    public string UserMessage { get; set; } = string.Empty;
    public string? FullPayloadJson { get; set; }
    public string ModelUsed { get; set; } = string.Empty;
    public double TemperatureUsed { get; set; }
    public int MaxTokensUsed { get; set; }
    public int? TokensUsed { get; set; }
    public long ResponseTimeMs { get; set; }
}

public class OpenAIResponse
{
    public Choice[]? Choices { get; set; }
    public Usage? Usage { get; set; }
}

public class Choice
{
    public Message? Message { get; set; }
}

public class Message
{
    public string? Content { get; set; }
}

public class Usage
{
    public int TotalTokens { get; set; }
}

public class TranscriptionResponse
{
    public string? Text { get; set; }
}