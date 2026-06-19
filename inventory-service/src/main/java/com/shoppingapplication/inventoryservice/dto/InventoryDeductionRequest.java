package com.shoppingapplication.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeductionRequest {
    @NotBlank
    private String orderNumber;
    @Valid
    @NotEmpty
    private List<InventoryDeductionItem> items;
}
