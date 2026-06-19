package com.shoppingapplication.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeductionItem {
    @NotBlank
    private String skuCode;
    @NotNull
    @Min(1)
    private Integer quantity;
}
