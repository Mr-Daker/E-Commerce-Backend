package com.shoppingapplication.inventoryservice;

import com.shoppingapplication.inventoryservice.dto.InventoryDeductionItem;
import com.shoppingapplication.inventoryservice.dto.InventoryDeductionRequest;
import com.shoppingapplication.inventoryservice.model.Inventory;
import com.shoppingapplication.inventoryservice.repository.InventoryRepository;
import com.shoppingapplication.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class InventoryServiceReliabilityTests {

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void deductInventoryIsIdempotentByOrderNumber() {
        Inventory inventory = new Inventory();
        inventory.setSkuCode("TEST-SKU-1");
        inventory.setQuantity(5);
        inventoryRepository.save(inventory);

        InventoryDeductionRequest request = new InventoryDeductionRequest(
                "order-1",
                List.of(new InventoryDeductionItem("TEST-SKU-1", 2))
        );

        inventoryService.deductInventory(request);
        inventoryService.deductInventory(request);

        Inventory updatedInventory = inventoryRepository.findBySkuCodeIn(List.of("TEST-SKU-1")).get(0);
        assertThat(updatedInventory.getQuantity()).isEqualTo(3);
    }

    @Test
    void deductInventoryRejectsInsufficientStock() {
        Inventory inventory = new Inventory();
        inventory.setSkuCode("TEST-SKU-2");
        inventory.setQuantity(1);
        inventoryRepository.save(inventory);

        InventoryDeductionRequest request = new InventoryDeductionRequest(
                "order-2",
                List.of(new InventoryDeductionItem("TEST-SKU-2", 2))
        );

        assertThatThrownBy(() -> inventoryService.deductInventory(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void releasingUnknownReservationIsIdempotent() {
        inventoryService.releaseReservation("unknown-order");
    }

    @Test
    void releasedReservationCannotBeReused() {
        Inventory inventory = new Inventory();
        inventory.setSkuCode("TEST-SKU-RELEASED");
        inventory.setQuantity(5);
        inventoryRepository.save(inventory);

        InventoryDeductionRequest request = new InventoryDeductionRequest(
                "released-order",
                List.of(new InventoryDeductionItem("TEST-SKU-RELEASED", 2))
        );
        inventoryService.reserveInventory(request);
        inventoryService.releaseReservation("released-order");

        assertThatThrownBy(() -> inventoryService.reserveInventory(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Released inventory reservation cannot be reused");

        Inventory unchanged = inventoryRepository.findBySkuCode("TEST-SKU-RELEASED").orElseThrow();
        assertThat(unchanged.getQuantity()).isEqualTo(5);
    }
}
