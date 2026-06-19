package com.shoppingapplication.orderservice.controller;

import com.shoppingapplication.orderservice.dto.ApiResponse;
import com.shoppingapplication.orderservice.dto.OrderRequest;
import com.shoppingapplication.orderservice.dto.OrderResponse;
import com.shoppingapplication.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> placeOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody OrderRequest orderRequest){
        return ApiResponse.success("Order placed", orderService.placeOrder(orderRequest, idempotencyKey, userId));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        return ApiResponse.success("Orders fetched", orderService.getOrders(page, size));
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable String orderNumber){
        return ApiResponse.success("Order fetched", orderService.getOrder(orderNumber));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(@PathVariable String orderNumber){
        return ApiResponse.success("Order cancelled", orderService.cancelOrder(orderNumber));
    }


}
