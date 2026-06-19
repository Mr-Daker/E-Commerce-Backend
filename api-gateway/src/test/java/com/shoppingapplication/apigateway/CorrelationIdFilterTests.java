package com.shoppingapplication.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTests {

    @Test
    void filterCreatesAndPropagatesCorrelationId() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/product").build());
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        filter.filter(exchange, filteredExchange -> {
            capturedExchange.set(filteredExchange);
            return filteredExchange.getResponse().setComplete();
        }).block();

        String correlationId = capturedExchange.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(correlationId).isNotBlank();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(correlationId);
    }

    @Test
    void filterPreservesExistingCorrelationId() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/product")
                .header(CorrelationIdFilter.CORRELATION_ID_HEADER, "existing-id")
                .build());
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        filter.filter(exchange, filteredExchange -> {
            capturedExchange.set(filteredExchange);
            return filteredExchange.getResponse().setComplete();
        }).block();

        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("existing-id");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("existing-id");
    }
}
