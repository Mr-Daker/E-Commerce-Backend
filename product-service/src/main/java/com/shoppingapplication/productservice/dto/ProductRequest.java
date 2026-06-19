package com.shoppingapplication.productservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Digits;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductRequest {
    @NotBlank
    private String skuCode;
    @NotBlank
    private String name;
    private String description;
    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 15, fraction = 2)
    private BigDecimal price;
}
