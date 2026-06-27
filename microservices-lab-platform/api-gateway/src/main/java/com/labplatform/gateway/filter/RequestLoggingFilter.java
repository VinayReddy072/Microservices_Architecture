package com.labplatform.gateway.filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.InetSocketAddress;
import java.time.Instant;
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)  
public class RequestLoggingFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = Instant.now().toEpochMilli();
        var request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getPath().toString();
        InetSocketAddress remoteAddr = request.getRemoteAddress();
        String remoteIp = (remoteAddr != null && remoteAddr.getAddress() != null)
                ? remoteAddr.getAddress().getHostAddress()
                : "unknown";
        log.info("GATEWAY_REQUEST correlationId={} method={} path={} remoteAddr={}",
                correlationId, method, path, remoteIp);
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long latency = Instant.now().toEpochMilli() - startTime;
                    HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
                    int status = (statusCode != null) ? statusCode.value() : -1;
                    log.info("GATEWAY_RESPONSE correlationId={} method={} path={} status={} latencyMs={}",
                            correlationId, method, path, status, latency);
                }));
    }
}
