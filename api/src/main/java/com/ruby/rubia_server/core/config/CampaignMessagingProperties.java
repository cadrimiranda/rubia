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
     * Otimizado para MVP: 30 mensagens por lote
     */
    private int batchSize = 30;
    
    /**
     * Tempo de pausa entre lotes em minutos
     * Otimizado para MVP: 30 minutos entre lotes
     */
    private int batchPauseMinutes = 30;
    
    /**
     * Delay mínimo entre mensagens em milissegundos
     * Otimizado para MVP: 15 segundos
     */
    private int minDelayMs = 15000; // 15 segundos
    
    /**
     * Delay máximo entre mensagens em milissegundos
     * Otimizado para MVP: 45 segundos
     */
    private int maxDelayMs = 45000; // 45 segundos
    
    /**
     * Timeout para envio de mensagem individual
     * Mantido em 30 segundos para segurança
     */
    private Duration messageTimeout = Duration.ofSeconds(30);
    
    /**
     * Timeout para processamento de lote completo
     * Otimizado para MVP: 15 minutos por lote
     */
    private Duration batchTimeout = Duration.ofMinutes(15);
    
    /**
     * Máximo de tentativas de reenvio por mensagem
     */
    private int maxRetries = 3;
    
    /**
     * Delay entre tentativas de reenvio em milissegundos
     */
    private int retryDelayMs = 5000; // 5 segundos
    
    /**
     * Ativar envio apenas em horário comercial
     */
    private boolean businessHoursOnly = true;
    
    /**
     * Hora de início do horário comercial (24h format)
     */
    private int businessStartHour = 9;
    
    /**
     * Hora de fim do horário comercial (24h format)
     */
    private int businessEndHour = 18;
    
    /**
     * Randomizar ordem das mensagens no lote
     */
    private boolean randomizeOrder = true;
}