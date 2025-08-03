package com.ruby.rubia_server.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "campaign.messaging")
public class CampaignMessagingProperties {
    
    /**
     * Tamanho do lote para processamento de mensagens
     * Baseado nas limitações da API WHAPI para evitar rate limiting
     */
    private int batchSize = 20;
    
    /**
     * Tempo de pausa entre lotes em minutos
     * Recomendado pela WHAPI para evitar bloqueios
     */
    private int batchPauseMinutes = 60;
    
    /**
     * Delay mínimo entre mensagens em milissegundos
     * Configuração conservadora baseada nas diretrizes WHAPI
     */
    private int minDelayMs = 30000; // 30 segundos
    
    /**
     * Delay máximo entre mensagens em milissegundos
     * Configuração conservadora baseada nas diretrizes WHAPI
     */
    private int maxDelayMs = 60000; // 60 segundos
    
    /**
     * Timeout para envio de mensagem individual
     * Tempo máximo para aguardar resposta da API
     */
    private Duration messageTimeout = Duration.ofSeconds(30);
    
    /**
     * Timeout para processamento de lote completo
     * Tempo máximo para processar um lote inteiro
     */
    private Duration batchTimeout = Duration.ofMinutes(10);
}