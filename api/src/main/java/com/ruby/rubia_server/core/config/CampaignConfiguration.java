package com.ruby.rubia_server.core.config;

import java.util.concurrent.ThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuração específica para processamento de campanhas
 * Implementa limitadores de concorrência e backpressure
 */
@Configuration
@EnableScheduling
public class CampaignConfiguration {
    
    @Bean(name = "campaignExecutor")
    public ThreadPoolTaskExecutor campaignExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100); // Fila pequena para forçar backpressure
        executor.setThreadNamePrefix("campaign-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "scheduledExecutor")
    public ScheduledThreadPoolExecutor scheduledExecutor() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
        executor.setThreadFactory(new ThreadFactory() {
            private int counter = 0;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "scheduler-" + counter++);
                t.setDaemon(true);
                return t;
            }
        });
        executor.setRemoveOnCancelPolicy(true);
        executor.setMaximumPoolSize(8);
        return executor;
    }
    
    @Bean(name = "campaignConcurrencyLimiter")
    public Semaphore concurrencyLimiter() {
        // Limita quantas mensagens podem estar sendo processadas simultaneamente
        // Previne sobrecarga do sistema e garante controle de recursos
        return new Semaphore(50);
    }
    
    @Bean(name = "queueProcessingLimiter")
    public Semaphore queueProcessingLimiter() {
        // Limita quantos itens podem ser retirados da fila por vez
        // Implementa backpressure no nível da fila
        return new Semaphore(10);
    }
}