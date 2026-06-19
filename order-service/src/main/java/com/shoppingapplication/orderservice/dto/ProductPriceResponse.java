package com.shoppingapplication.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductPriceResponse {
    private String id;
    private String skuCode;
    private String name;
    private String description;
    private BigDecimal price;
}
