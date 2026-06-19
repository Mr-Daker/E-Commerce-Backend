package com.shoppingapplication.orderservice.service;

import com.shoppingapplication.orderservice.dto.ApiResponse;
import com.shoppingapplication.orderservice.dto.InventoryDeductionItem;
import com.shoppingapplication.orderservice.dto.InventoryDeductionRequest;
import com.shoppingapplication.orderservice.dto.OrderLineDto;
import com.shoppingapplication.orderservice.dto.OrderRequest;
import com.shoppingapplication.orderservice.dto.OrderResponse;
import com.shoppingapplication.orderservice.dto.ProductPriceResponse;
import com.shoppingapplication.orderservice.model.Order;
import com.shoppingapplication.orderservice.model.OrderLineItems;
import com.shoppingapplication.orderservice.model.OrderStatus;
import com.shoppingapplication.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final OrderPersistenceService orderPersistenceService;
    @Value("${inventory.client.timeout-ms:3000}")
    private Long inventoryClientTimeoutMs;
    @Value("${inventory.client.retry-attempts:2}")
    private Long inventoryClientRetryAttempts;
    @Value("${inventory.service.url:http://inventory-service}")
    private String inventoryServiceUrl;
    @Value("${product.service.url:http://product-service}")
    private String productServiceUrl;
    @Value("${order.pending.max-age-ms:300000}")
    private Long pendingOrderMaxAgeMs;

    public OrderResponse placeOrder(OrderRequest orderRequest, String idempotencyKey, String userId){
        validateOrder(orderRequest);
        if (userId == null || userId.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must contain 1 to 100 characters");
        }
        String requestHash = requestHash(orderRequest);
        Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingOrder != null) {
            return replayOrder(existingOrder, requestHash, userId);
        }

        Order order= new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setIdempotencyKey(idempotencyKey);
        order.setRequestHash(requestHash);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(Instant.now());
        order.setRecoveryAfter(Instant.now().plusMillis(pendingOrderMaxAgeMs));

        Map<String, ProductPriceResponse> productsBySkuCode = resolveProducts(orderRequest.getOrderLineDtoList());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineDtoList().stream()
                .map(item -> mapToOrderLine(item, productsBySkuCode))
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        // Persist the saga before the first remote mutation so an ambiguous
        // inventory timeout always has a durable recovery record.
        try {
            orderPersistenceService.saveWithEvent(order, "ORDER_PENDING");
        } catch (DataIntegrityViolationException exception) {
            Order concurrentOrder = orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
            return replayOrder(concurrentOrder, requestHash, userId);
        }
        try {
            reserveInventory(order.getOrderNumber(), orderLineItems);
            commitInventory(order.getOrderNumber());
            order.setStatus(OrderStatus.CONFIRMED);
            orderPersistenceService.saveWithEvent(order, "ORDER_CONFIRMED");
        } catch (RuntimeException exception) {
            compensateFailedOrder(order);
            throw exception;
        }

        return toResponse(order, "Order placed successfully");
    }

    public List<OrderResponse> getOrders(int page, int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater and size must be at least one");
        }
        return orderRepository.findAll(PageRequest.of(page, Math.min(size, 100))).stream()
                .map(order -> toResponse(order, null))
                .toList();
    }

    public OrderResponse getOrder(String orderNumber) {
        return toResponse(findOrder(orderNumber), null);
    }

    public OrderResponse cancelOrder(String orderNumber) {
        Order order = findOrder(orderNumber);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toResponse(order, "Order was already cancelled");
        }
        if (order.getStatus() == OrderStatus.CANCELLATION_PENDING) {
            return finishCancellation(order);
        }
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only confirmed orders can be cancelled");
        }

        order.setStatus(OrderStatus.CANCELLATION_PENDING);
        orderPersistenceService.saveWithEvent(order, "ORDER_CANCELLATION_REQUESTED");
        return finishCancellation(order);
    }

    private OrderLineItems mapToOrderLine(OrderLineDto orderLineDto, Map<String, ProductPriceResponse> productsBySkuCode){
        ProductPriceResponse product = productsBySkuCode.get(orderLineDto.getSkuCode());
        if (product == null || product.getPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found for SKU " + orderLineDto.getSkuCode());
        }
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setQuantity(orderLineDto.getQuantity());
        orderLineItems.setPrice(product.getPrice());
        orderLineItems.setSkuCode(orderLineDto.getSkuCode());

        return orderLineItems;
    }

    private Map<String, ProductPriceResponse> resolveProducts(List<OrderLineDto> orderLineItems) {
        return orderLineItems.stream()
                .map(OrderLineDto::getSkuCode)
                .distinct()
                .map(this::fetchProduct)
                .collect(Collectors.toMap(ProductPriceResponse::getSkuCode, Function.identity()));
    }

    private ProductPriceResponse fetchProduct(String skuCode) {
        try {
            ApiResponse<ProductPriceResponse> response = webClientBuilder.build()
                    .get()
                    .uri(productServiceUrl + "/api/product/sku/{skuCode}", skuCode)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<ProductPriceResponse>>() {})
                    .timeout(Duration.ofMillis(inventoryClientTimeoutMs))
                    .retryWhen(Retry.backoff(inventoryClientRetryAttempts, Duration.ofMillis(200))
                            .filter(this::isRetryableInventoryException))
                    .block();
            if (response == null || response.getData() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found for SKU " + skuCode);
            }
            return response.getData();
        } catch (WebClientResponseException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found for SKU " + skuCode, exception);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product service is temporarily unavailable", exception);
        }
    }

    private InventoryDeductionRequest inventoryRequest(String orderNumber, List<OrderLineItems> orderLineItems) {
        Map<String, Integer> quantitiesBySkuCode = orderLineItems.stream()
                .collect(Collectors.groupingBy(OrderLineItems::getSkuCode, Collectors.summingInt(OrderLineItems::getQuantity)));

        List<InventoryDeductionItem> inventoryDeductionItems = quantitiesBySkuCode.entrySet().stream()
                .map(entry -> new InventoryDeductionItem(entry.getKey(), entry.getValue()))
                .toList();
        return new InventoryDeductionRequest(orderNumber, inventoryDeductionItems);
    }

    private void reserveInventory(String orderNumber, List<OrderLineItems> orderLineItems) {
        callInventory("/api/inventory/reserve", inventoryRequest(orderNumber, orderLineItems));
    }

    private void commitInventory(String orderNumber) {
        callInventory("/api/inventory/commit/" + orderNumber, null);
    }

    private void releaseInventory(String orderNumber) {
        callInventory("/api/inventory/release/" + orderNumber, null);
    }

    private void compensateFailedOrder(Order order) {
        try {
            releaseInventory(order.getOrderNumber());
            order.setStatus(OrderStatus.FAILED);
        } catch (RuntimeException compensationError) {
            order.setStatus(OrderStatus.COMPENSATION_REQUIRED);
            log.error("Inventory compensation failed for order {}; it will be retried", order.getOrderNumber(), compensationError);
        }
        orderPersistenceService.saveWithEvent(order,
                order.getStatus() == OrderStatus.FAILED ? "ORDER_FAILED" : "ORDER_COMPENSATION_REQUIRED");
    }

    @Scheduled(fixedDelayString = "${order.compensation.retry-delay-ms:30000}")
    public void retryPendingCompensations() {
        orderRepository.findByStatus(OrderStatus.COMPENSATION_REQUIRED).forEach(order -> {
            try {
                releaseInventory(order.getOrderNumber());
                order.setStatus(OrderStatus.FAILED);
                orderPersistenceService.saveWithEvent(order, "ORDER_FAILED");
                log.info("Inventory compensation completed for order {}", order.getOrderNumber());
            } catch (RuntimeException exception) {
                log.warn("Inventory compensation still pending for order {}", order.getOrderNumber());
            }
        });

        orderRepository.findByStatus(OrderStatus.CANCELLATION_PENDING).forEach(order -> {
            try {
                finishCancellation(order);
            } catch (RuntimeException exception) {
                log.warn("Cancellation still pending for order {}", order.getOrderNumber());
            }
        });

        Instant now = Instant.now();
        orderRepository.findRecoverablePending(OrderStatus.PENDING, now, now.minusMillis(pendingOrderMaxAgeMs))
                .forEach(order -> {
                    try {
                        log.warn("Recovering stale pending order {}", order.getOrderNumber());
                        compensateFailedOrder(order);
                    } catch (RuntimeException exception) {
                        log.error("Could not recover stale pending order {}", order.getOrderNumber(), exception);
                    }
                });
    }

    private void callInventory(String path, InventoryDeductionRequest request) {
        try {
            WebClient.RequestBodySpec requestSpec = webClientBuilder.build()
                    .post()
                    .uri(inventoryServiceUrl + path);
            WebClient.RequestHeadersSpec<?> headersSpec = request == null ? requestSpec : requestSpec.bodyValue(request);
            headersSpec
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(inventoryClientTimeoutMs))
                    .retryWhen(Retry.backoff(inventoryClientRetryAttempts, Duration.ofMillis(200))
                            .filter(this::isRetryableInventoryException))
                    .block();
        } catch (WebClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ResponseStatusException(exception.getStatusCode(), "Inventory request was rejected", exception);
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventory service is temporarily unavailable", exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventory service is temporarily unavailable", exception);
        }
    }

    private Order findOrder(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private OrderResponse finishCancellation(Order order) {
        releaseInventory(order.getOrderNumber());
        order.setStatus(OrderStatus.CANCELLED);
        orderPersistenceService.saveWithEvent(order, "ORDER_CANCELLED");
        return toResponse(order, "Order cancelled successfully");
    }

    private OrderResponse toResponse(Order order, String message) {
        List<OrderLineDto> items = order.getOrderLineItemsList().stream().map(item -> {
            OrderLineDto dto = new OrderLineDto();
            dto.setId(item.getId());
            dto.setSkuCode(item.getSkuCode());
            dto.setPrice(item.getPrice());
            dto.setQuantity(item.getQuantity());
            return dto;
        }).toList();

        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus() == null ? "UNKNOWN" : order.getStatus().name())
                .createdAt(order.getCreatedAt())
                .items(items)
                .message(message)
                .build();
    }

    private void validateOrder(OrderRequest orderRequest) {
        if (orderRequest == null || orderRequest.getOrderLineDtoList() == null || orderRequest.getOrderLineDtoList().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order must contain at least one item");
        }
    }

    private OrderResponse replayOrder(Order order, String requestHash, String userId) {
        if (!requestHash.equals(order.getRequestHash())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key was already used for a different order");
        }
        if (order.getUserId() != null && !userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key belongs to another user");
        }
        return toResponse(order, "Order request already processed");
    }

    private String requestHash(OrderRequest request) {
        String canonical = request.getOrderLineDtoList().stream()
                .collect(Collectors.groupingBy(OrderLineDto::getSkuCode, Collectors.summingLong(OrderLineDto::getQuantity)))
                .entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey().length() + ":" + entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private boolean isRetryableInventoryException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException responseException) {
            return responseException.getStatusCode().is5xxServerError();
        }
        return true;
    }
}
