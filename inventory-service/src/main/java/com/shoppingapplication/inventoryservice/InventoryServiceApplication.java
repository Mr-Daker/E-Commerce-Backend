package com.shoppingapplication.inventoryservice;

import com.shoppingapplication.inventoryservice.model.Inventory;
import com.shoppingapplication.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}


	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository){
		return args -> {
			createInventoryIfMissing(inventoryRepository, "Iphone_13", 100);
			createInventoryIfMissing(inventoryRepository, "Iphone_14", 10);
		};
	}

	private void createInventoryIfMissing(InventoryRepository inventoryRepository, String skuCode, Integer quantity) {
		if (inventoryRepository.existsBySkuCode(skuCode)) {
			return;
		}

		Inventory inventory = new Inventory();
		inventory.setSkuCode(skuCode);
		inventory.setQuantity(quantity);
        try {
            inventoryRepository.saveAndFlush(inventory);
        } catch (DataIntegrityViolationException ignored) {
            // Another replica inserted the seed row concurrently.
        }
	}

}
