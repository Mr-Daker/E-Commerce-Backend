package com.shoppingapplication.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeductionItem {
    private String skuCode;
    private Integer quantity;
}
