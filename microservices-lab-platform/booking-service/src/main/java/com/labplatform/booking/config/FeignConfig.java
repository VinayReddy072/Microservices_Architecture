package com.labplatform.booking.config;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import com.labplatform.booking.exception.EquipmentNotFoundException;
import com.labplatform.booking.exception.EquipmentUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Slf4j
@Configuration
public class FeignConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            log.error("Feign error on {}: status={}", methodKey, response.status());
            return switch (response.status()) {
                case 404 -> new EquipmentNotFoundException("Equipment not found in equipment service");
                case 409 -> new EquipmentUnavailableException("Equipment is not available for booking");
                default -> new RuntimeException("Equipment service error: " + response.status());
            };
        };
    }
    @Bean
    public RequestInterceptor correlationIdInterceptor() {
        return requestTemplate -> {
            // Prefer the X-Correlation-Id set by the Gateway CorrelationIdFilter
            // Fall back to the OTel/Micrometer traceId in MDC
            String correlationId = org.slf4j.MDC.get("correlationId");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = org.slf4j.MDC.get("traceId");
            }
            if (correlationId != null && !correlationId.isBlank()) {
                requestTemplate.header("X-Correlation-Id", correlationId);
                log.debug("Feign propagating X-Correlation-Id={} to {}", correlationId,
                        requestTemplate.url());
            }
        };
    }
}
