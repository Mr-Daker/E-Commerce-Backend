package com.shoppingapplication.orderservice.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;


@Data
public class OrderLineDto {
    private Long id;
    @NotBlank
    private String skuCode;
    // Ignored when supplied; order prices are resolved from product-service.
    private BigDecimal price;
    @NotNull
    @Min(1)
    private Integer quantity;

}
