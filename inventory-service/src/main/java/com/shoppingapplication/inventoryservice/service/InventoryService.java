package com.shoppingapplication.inventoryservice.service;

import com.shoppingapplication.inventoryservice.dto.InventoryDeductionItem;
import com.shoppingapplication.inventoryservice.dto.InventoryDeductionRequest;
import com.shoppingapplication.inventoryservice.dto.InventoryResponse;
import com.shoppingapplication.inventoryservice.dto.InventoryUpsertRequest;
import com.shoppingapplication.inventoryservice.model.InventoryDeduction;
import com.shoppingapplication.inventoryservice.model.InventoryDeductionLine;
import com.shoppingapplication.inventoryservice.model.Inventory;
import com.shoppingapplication.inventoryservice.model.InventoryReservationStatus;
import com.shoppingapplication.inventoryservice.repository.InventoryDeductionRepository;
import com.shoppingapplication.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {


    private final InventoryRepository inventoryRepository;
    private final InventoryDeductionRepository inventoryDeductionRepository;
    @Transactional(readOnly =true)
    public List<InventoryResponse> isInStock(List<String> skuCode){
        return inventoryRepository.findBySkuCodeIn(skuCode).stream().map(x->

            toResponse(x)
        ).toList();


    }

    @Transactional
    public void deductInventory(InventoryDeductionRequest inventoryDeductionRequest) {
        reserveInventory(inventoryDeductionRequest);
        commitReservation(inventoryDeductionRequest.getOrderNumber());
    }

    @Transactional
    public void reserveInventory(InventoryDeductionRequest inventoryDeductionRequest) {
        if (inventoryDeductionRequest == null || inventoryDeductionRequest.getItems() == null || inventoryDeductionRequest.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory deduction request cannot be empty");
        }

        InventoryDeduction existingDeduction = inventoryDeductionRepository
                .findByOrderNumberForUpdate(inventoryDeductionRequest.getOrderNumber())
                .orElse(null);
        if (existingDeduction != null && existingDeduction.getStatus() == InventoryReservationStatus.RELEASED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Released inventory reservation cannot be reused");
        }
        if (existingDeduction != null) {
            return;
        }

        List<String> skuCodes = inventoryDeductionRequest.getItems().stream()
                .map(InventoryDeductionItem::getSkuCode)
                .toList();
        Map<String, Inventory> inventoryBySkuCode = inventoryRepository.findBySkuCodeInForUpdate(skuCodes).stream()
                .collect(Collectors.toMap(Inventory::getSkuCode, Function.identity(), (current, duplicate) -> current));

        for (InventoryDeductionItem request : inventoryDeductionRequest.getItems()) {
            Inventory inventory = inventoryBySkuCode.get(request.getSkuCode());
            if (inventory == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is not in stock, please try again later");
            }

            if (inventory.getQuantity() < request.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product is not in stock, please try again later");
            }

            inventory.setQuantity(inventory.getQuantity() - request.getQuantity());
        }

        InventoryDeduction inventoryDeduction = new InventoryDeduction();
        inventoryDeduction.setOrderNumber(inventoryDeductionRequest.getOrderNumber());
        inventoryDeduction.setStatus(InventoryReservationStatus.RESERVED);
        inventoryDeduction.setItems(inventoryDeductionRequest.getItems().stream().map(item -> {
            InventoryDeductionLine line = new InventoryDeductionLine();
            line.setSkuCode(item.getSkuCode());
            line.setQuantity(item.getQuantity());
            return line;
        }).toList());
        inventoryDeductionRepository.save(inventoryDeduction);
    }

    @Transactional
    public void commitReservation(String orderNumber) {
        InventoryDeduction reservation = getReservation(orderNumber);
        if (reservation.getStatus() == InventoryReservationStatus.RELEASED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Released inventory reservation cannot be committed");
        }
        reservation.setStatus(InventoryReservationStatus.COMMITTED);
    }

    @Transactional
    public void releaseReservation(String orderNumber) {
        InventoryDeduction reservation = inventoryDeductionRepository.findByOrderNumberForUpdate(orderNumber).orElse(null);
        // A reserve call can time out before the caller sees its outcome. Treat a
        // missing reservation as already released so compensation remains safe.
        if (reservation == null) {
            return;
        }
        if (reservation.getStatus() == InventoryReservationStatus.RELEASED) {
            return;
        }

        Map<String, Inventory> inventoryBySkuCode = inventoryRepository.findBySkuCodeInForUpdate(
                        reservation.getItems().stream().map(InventoryDeductionLine::getSkuCode).toList())
                .stream().collect(Collectors.toMap(Inventory::getSkuCode, Function.identity(), (current, duplicate) -> current));

        for (InventoryDeductionLine item : reservation.getItems()) {
            Inventory inventory = inventoryBySkuCode.get(item.getSkuCode());
            if (inventory != null) {
                inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            }
        }
        reservation.setStatus(InventoryReservationStatus.RELEASED);
    }

    @Transactional
    public InventoryResponse upsertInventory(InventoryUpsertRequest request) {
        Inventory inventory = inventoryRepository.findBySkuCodeForUpdate(request.getSkuCode()).orElseGet(Inventory::new);
        inventory.setSkuCode(request.getSkuCode());
        inventory.setQuantity(request.getQuantity());
        return toResponse(inventoryRepository.save(inventory));
    }

    @Transactional
    public InventoryResponse restock(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCodeForUpdate(skuCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
        inventory.setQuantity(inventory.getQuantity() + quantity);
        return toResponse(inventory);
    }

    private InventoryDeduction getReservation(String orderNumber) {
        return inventoryDeductionRepository.findByOrderNumberForUpdate(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory reservation not found"));
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .quantity(inventory.getQuantity())
                .isInStock(inventory.getQuantity() > 0)
                .build();
    }

}
