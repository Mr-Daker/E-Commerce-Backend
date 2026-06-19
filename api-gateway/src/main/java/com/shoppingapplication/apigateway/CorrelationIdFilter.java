package com.shoppingapplication.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incomingCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        String correlationId = (incomingCorrelationId == null || incomingCorrelationId.isBlank())
                ? UUID.randomUUID().toString()
                : incomingCorrelationId;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();

        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);
        log.info("correlationId={} method={} path={}", correlationId, request.getMethod(), request.getURI().getPath());

        return chain.filter(exchange.mutate().request(request).build())
                .doOnSuccess(unused -> log.info("correlationId={} status={}", correlationId, exchange.getResponse().getStatusCode()))
                .doOnError(error -> log.warn("correlationId={} error={}", correlationId, error.getMessage()));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
