package com.shoppingapplication.inventoryservice.controller;

import com.shoppingapplication.inventoryservice.dto.ApiResponse;
import com.shoppingapplication.inventoryservice.dto.InventoryDeductionRequest;
import com.shoppingapplication.inventoryservice.dto.InventoryResponse;
import com.shoppingapplication.inventoryservice.dto.InventoryUpsertRequest;
import com.shoppingapplication.inventoryservice.dto.RestockRequest;
import com.shoppingapplication.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping(value = "/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<InventoryResponse>> isInStock(@RequestParam List<String> skuCode){

        return ApiResponse.success("Inventory fetched", inventoryService.isInStock(skuCode));
    }

    @PostMapping("/deduct")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deductInventory(@Valid @RequestBody InventoryDeductionRequest inventoryDeductionRequest){
        inventoryService.deductInventory(inventoryDeductionRequest);
        return ApiResponse.success("Inventory deducted", null);
    }

    @PostMapping("/reserve")
    public ApiResponse<Void> reserveInventory(@Valid @RequestBody InventoryDeductionRequest request){
        inventoryService.reserveInventory(request);
        return ApiResponse.success("Inventory reserved", null);
    }

    @PostMapping("/commit/{orderNumber}")
    public ApiResponse<Void> commitReservation(@PathVariable String orderNumber){
        inventoryService.commitReservation(orderNumber);
        return ApiResponse.success("Inventory reservation committed", null);
    }

    @PostMapping("/release/{orderNumber}")
    public ApiResponse<Void> releaseReservation(@PathVariable String orderNumber){
        inventoryService.releaseReservation(orderNumber);
        return ApiResponse.success("Inventory reservation released", null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InventoryResponse> upsertInventory(@Valid @RequestBody InventoryUpsertRequest request){
        return ApiResponse.success("Inventory saved", inventoryService.upsertInventory(request));
    }

    @PatchMapping("/{skuCode}/restock")
    public ApiResponse<InventoryResponse> restock(@PathVariable String skuCode, @Valid @RequestBody RestockRequest request){
        return ApiResponse.success("Inventory restocked", inventoryService.restock(skuCode, request.getQuantity()));
    }

}
