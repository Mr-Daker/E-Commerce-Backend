package com.shoppingapplication.orderservice;

import com.shoppingapplication.orderservice.dto.OrderLineDto;
import com.shoppingapplication.orderservice.dto.OrderRequest;
import com.shoppingapplication.orderservice.dto.OrderResponse;
import com.shoppingapplication.orderservice.model.Order;
import com.shoppingapplication.orderservice.model.OrderStatus;
import com.shoppingapplication.orderservice.repository.OrderRepository;
import com.shoppingapplication.orderservice.service.OrderService;
import com.shoppingapplication.orderservice.service.OrderPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceReliabilityTests {

    @Test
    void placeOrderDeductsInventoryAndReturnsOrderNumber() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderPersistenceService persistenceService = mock(OrderPersistenceService.class);
        AtomicReference<Order> savedOrder = new AtomicReference<>();
        when(orderRepository.findByIdempotencyKey("test-key"))
                .thenAnswer(invocation -> Optional.ofNullable(savedOrder.get()));
        when(persistenceService.saveWithEvent(any(Order.class), any(String.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            savedOrder.set(saved);
            return saved;
        });

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> {
                    ClientResponse.Builder response = ClientResponse.create(HttpStatus.OK);
                    if (request.url().getPath().contains("/api/product/sku/")) {
                        response.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"success\":true,\"message\":\"Product fetched\",\"data\":{\"id\":\"product-id\",\"skuCode\":\"Iphone_13\",\"name\":\"iPhone 13\",\"description\":\"Apple smartphone\",\"price\":699.99}}");
                    } else {
                        response.header("Content-Type", MediaType.TEXT_PLAIN_VALUE).body("");
                    }
                    return Mono.just(response.build());
                });
        OrderService orderService = new OrderService(orderRepository, webClientBuilder, persistenceService);
        ReflectionTestUtils.setField(orderService, "inventoryClientTimeoutMs", 3000L);
        ReflectionTestUtils.setField(orderService, "inventoryClientRetryAttempts", 0L);
        ReflectionTestUtils.setField(orderService, "inventoryServiceUrl", "http://inventory-service");
        ReflectionTestUtils.setField(orderService, "productServiceUrl", "http://product-service");
        ReflectionTestUtils.setField(orderService, "pendingOrderMaxAgeMs", 300000L);

        OrderLineDto orderLineDto = new OrderLineDto();
        orderLineDto.setSkuCode("Iphone_13");
        orderLineDto.setPrice(BigDecimal.valueOf(699.99));
        orderLineDto.setQuantity(1);

        OrderResponse response = orderService.placeOrder(new OrderRequest(List.of(orderLineDto)), "test-key", "test-user");

        assertThat(response.getOrderNumber()).isNotBlank();
        assertThat(response.getMessage()).isEqualTo("Order placed successfully");
        OrderResponse replay = orderService.placeOrder(new OrderRequest(List.of(orderLineDto)), "test-key", "test-user");
        assertThat(replay.getOrderNumber()).isEqualTo(response.getOrderNumber());
        assertThat(replay.getMessage()).isEqualTo("Order request already processed");
        verify(persistenceService, times(2)).saveWithEvent(any(Order.class), any(String.class));
    }

    @Test
    void cancelOrderRejectsPendingOrder() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderPersistenceService persistenceService = mock(OrderPersistenceService.class);
        Order order = new Order();
        order.setOrderNumber("pending-order");
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByOrderNumber("pending-order")).thenReturn(Optional.of(order));

        OrderService orderService = new OrderService(orderRepository, WebClient.builder(), persistenceService);

        assertThatThrownBy(() -> orderService.cancelOrder("pending-order"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Only confirmed orders can be cancelled");
    }

    @Test
    void recoveryCompensatesStalePendingOrder() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderPersistenceService persistenceService = mock(OrderPersistenceService.class);
        when(persistenceService.saveWithEvent(any(Order.class), any(String.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order order = new Order();
        order.setOrderNumber("stale-order");
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now().minusSeconds(600));

        when(orderRepository.findByStatus(OrderStatus.COMPENSATION_REQUIRED)).thenReturn(List.of());
        when(orderRepository.findByStatus(OrderStatus.CANCELLATION_PENDING)).thenReturn(List.of());
        when(orderRepository.findRecoverablePending(any(OrderStatus.class), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(order));

        WebClient.Builder webClientBuilder = WebClient.builder()
                .exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK).build()));
        OrderService orderService = new OrderService(orderRepository, webClientBuilder, persistenceService);
        ReflectionTestUtils.setField(orderService, "inventoryClientTimeoutMs", 3000L);
        ReflectionTestUtils.setField(orderService, "inventoryClientRetryAttempts", 0L);
        ReflectionTestUtils.setField(orderService, "inventoryServiceUrl", "http://inventory-service");
        ReflectionTestUtils.setField(orderService, "pendingOrderMaxAgeMs", 300000L);

        orderService.retryPendingCompensations();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        verify(persistenceService).saveWithEvent(order, "ORDER_FAILED");
    }
}
