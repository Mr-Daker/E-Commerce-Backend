package com.shoppingapplication.apigateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedUserFilter implements GlobalFilter, Ordered {
    public static final String USER_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .flatMap(principal -> chain.filter(exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .headers(headers -> {
                                    headers.remove(USER_HEADER);
                                    headers.add(USER_HEADER, principal.getName());
                                }).build())
                        .build()))
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
