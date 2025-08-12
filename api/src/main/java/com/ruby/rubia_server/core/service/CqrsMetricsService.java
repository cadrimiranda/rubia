package com.ruby.rubia_server.core.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class CqrsMetricsService {

    private final MeterRegistry meterRegistry;
    
    private static final String CQRS_OPERATION_TIMER = "cqrs.conversation_last_message.operation.duration";
    private static final String CQRS_OPERATION_COUNTER = "cqrs.conversation_last_message.operation.count";
    private static final String CQRS_ERROR_COUNTER = "cqrs.conversation_last_message.error.count";
    private static final String CQRS_RETRY_COUNTER = "cqrs.conversation_last_message.retry.count";

    public void recordOperationDuration(String operation, long durationMs) {
        Timer.builder(CQRS_OPERATION_TIMER)
            .tag("operation", operation.toLowerCase())
            .register(meterRegistry)
            .record(Duration.ofMillis(durationMs));
    }

    public void incrementOperationCounter(String operation, String result) {
        Counter.builder(CQRS_OPERATION_COUNTER)
            .tag("operation", operation.toLowerCase())
            .tag("result", result.toLowerCase())
            .register(meterRegistry)
            .increment();
    }

    public void incrementErrorCounter(String operation, String errorType) {
        Counter.builder(CQRS_ERROR_COUNTER)
            .tag("operation", operation.toLowerCase())
            .tag("error_type", errorType.toLowerCase())
            .register(meterRegistry)
            .increment();
    }

    public void incrementRetryCounter(String operation, int attempt) {
        Counter.builder(CQRS_RETRY_COUNTER)
            .tag("operation", operation.toLowerCase())
            .tag("attempt", String.valueOf(attempt))
            .register(meterRegistry)
            .increment();
    }
}