package com.ruby.rubia_server.core.service;

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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    private final OpenAiChatModel chatModel;
    
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