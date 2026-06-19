package com.shoppingapplication.orderservice.config;


import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder(){
        return WebClient.builder().filter((request, next) -> {
            String correlationId = null;
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                correlationId = attributes.getRequest().getHeader("X-Correlation-Id");
            }
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            String outboundCorrelationId = correlationId;
            return next.exchange(ClientRequest.from(request)
                    .header("X-Correlation-Id", outboundCorrelationId)
                    .build());
        });
    }

}
