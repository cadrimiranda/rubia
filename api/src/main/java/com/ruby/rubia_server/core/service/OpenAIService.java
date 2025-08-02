package com.ruby.rubia_server.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruby.rubia_server.core.dto.AIEnhancementResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    
    @Value("${ai.default-model:gpt-4o-mini}")
    private String defaultModel;
    
    @Value("${ai.default-max-tokens:150}")
    private Integer defaultMaxTokens;
    
    @Value("${ai.default-temperature:0.7}")
    private Double defaultTemperature;

    public String enhanceTemplate(String prompt, String modelName, Double temperature, Integer maxTokens) {
        try {
            log.debug("Enhancing template with OpenAI - Model: {}, Temperature: {}, MaxTokens: {}", 
                     modelName, temperature, maxTokens);
            
            SystemMessage systemMessage = new SystemMessage(
                "Você é um especialista em captação de doadores de sangue para centros de hematologia e hemoterapia. " +
                "Sua especialidade é criar mensagens persuasivas e eficazes que motivem pessoas a fazer doações de sangue. " +
                "Sempre mantenha um tom ético, respeitoso e focado no impacto social positivo da doação.");

            UserMessage userMessage = new UserMessage(prompt);

            Double finalTemperature = temperature != null ? temperature : defaultTemperature;
            Integer finalMaxTokens = maxTokens != null ? maxTokens : defaultMaxTokens;
            String finalModel = modelName != null ? modelName : defaultModel;
            
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(finalModel)
                    .withTemperature(finalTemperature)
                    .withMaxTokens(finalMaxTokens)
                    .build();

            Prompt chatPrompt = new Prompt(List.of(systemMessage, userMessage), options);
            ChatResponse response = chatModel.call(chatPrompt);
            
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String enhancedContent = response.getResult().getOutput().getContent();
                
                log.info("Template enhanced successfully using model: {} - Tokens used: {}", 
                        modelName != null ? modelName : defaultModel, 
                        response.getMetadata() != null ? response.getMetadata().getUsage() : "unknown");
                
                return enhancedContent;
            }
            
            log.warn("OpenAI response contained no content");
            return "Erro: Resposta vazia da OpenAI";
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return "Erro ao processar com OpenAI: " + e.getMessage();
        }
    }

    public String enhanceTemplate(String prompt) {
        return enhanceTemplate(prompt, null, null, null);
    }

    /**
     * Versão do método que retorna resultado completo com payload para auditoria
     */
    public AIEnhancementResult enhanceTemplateWithPayload(String prompt, String modelName, Double temperature, Integer maxTokens) {
        try {
            log.debug("Enhancing template with OpenAI (with payload tracking) - Model: {}, Temperature: {}, MaxTokens: {}", 
                     modelName, temperature, maxTokens);
            
            String systemMessageContent = "Você é um especialista em captação de doadores de sangue para centros de hematologia e hemoterapia. " +
                "Sua especialidade é criar mensagens persuasivas e eficazes que motivem pessoas a fazer doações de sangue. " +
                "Sempre mantenha um tom ético, respeitoso e focado no impacto social positivo da doação.";

            SystemMessage systemMessage = new SystemMessage(systemMessageContent);
            UserMessage userMessage = new UserMessage(prompt);

            Double finalTemperature = temperature != null ? temperature : defaultTemperature;
            Integer finalMaxTokens = maxTokens != null ? maxTokens : defaultMaxTokens;
            String finalModel = modelName != null ? modelName : defaultModel;
            
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(finalModel)
                    .withTemperature(finalTemperature)
                    .withMaxTokens(finalMaxTokens)
                    .build();

            Prompt chatPrompt = new Prompt(List.of(systemMessage, userMessage), options);
            
            // Criar JSON do payload para auditoria
            String fullPayloadJson = createPayloadJson(finalModel, finalTemperature, finalMaxTokens, systemMessageContent, prompt);
            
            ChatResponse response = chatModel.call(chatPrompt);
            
            if (response.getResult() != null && response.getResult().getOutput() != null) {
                String enhancedContent = response.getResult().getOutput().getContent();
                
                log.info("Template enhanced successfully using model: {} - Tokens used: {}", 
                        finalModel, 
                        response.getMetadata() != null ? response.getMetadata().getUsage() : "unknown");
                
                return AIEnhancementResult.builder()
                        .enhancedMessage(enhancedContent)
                        .systemMessage(systemMessageContent)
                        .userMessage(prompt)
                        .fullPayloadJson(fullPayloadJson)
                        .modelUsed(finalModel)
                        .temperatureUsed(finalTemperature)
                        .maxTokensUsed(finalMaxTokens)
                        .tokensUsed(extractTokensUsed(response))
                        .build();
            }
            
            log.warn("OpenAI response contained no content");
            throw new RuntimeException("Resposta vazia da OpenAI");
            
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar com OpenAI: " + e.getMessage());
        }
    }

    /**
     * Cria JSON do payload enviado para OpenAI para fins de auditoria
     */
    private String createPayloadJson(String model, Double temperature, Integer maxTokens, String systemMessage, String userMessage) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", model);
            payload.put("temperature", temperature);
            payload.put("max_tokens", maxTokens);
            
            List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemMessage),
                Map.of("role", "user", "content", userMessage)
            );
            payload.put("messages", messages);
            
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.warn("Failed to serialize payload to JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrai número de tokens usados da resposta (se disponível)
     */
    private Integer extractTokensUsed(ChatResponse response) {
        try {
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
                Long totalTokens = response.getMetadata().getUsage().getTotalTokens();
                return totalTokens != null ? totalTokens.intValue() : null;
            }
        } catch (Exception e) {
            log.debug("Could not extract token usage from response: {}", e.getMessage());
        }
        return null;
    }

    public int estimateTokens(String text) {
        return text.length() / 4;
    }

    public boolean isServiceAvailable() {
        try {
            ChatClient.create(chatModel).prompt("test").call().content();
            return true;
        } catch (Exception e) {
            log.warn("OpenAI service is not available: {}", e.getMessage());
            return false;
        }
    }
}