package com.shoppingapplication.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderResponse {
    private String orderNumber;
    private String status;
    private java.time.Instant createdAt;
    private java.util.List<OrderLineDto> items;
    private String message;
}
