package com.labplatform.gateway.filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.util.UUID;
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)  
public class CorrelationIdFilter implements GlobalFilter {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new Correlation ID: {}", correlationId);
        } else {
            log.debug("Propagating existing Correlation ID: {}", correlationId);
        }
        final String finalCorrelationId = correlationId;
        var mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }
}
