package com.shoppingapplication.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeductionRequest {
    private String orderNumber;
    private List<InventoryDeductionItem> items;
}
