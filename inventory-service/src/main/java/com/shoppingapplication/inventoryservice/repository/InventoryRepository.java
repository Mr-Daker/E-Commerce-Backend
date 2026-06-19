package com.shoppingapplication.inventoryservice.repository;

import com.shoppingapplication.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    List<Inventory> findBySkuCodeIn(List<String> skuCode);

    boolean existsBySkuCode(String skuCode);
    Optional<Inventory> findBySkuCode(String skuCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from Inventory inventory where inventory.skuCode in :skuCodes")
    List<Inventory> findBySkuCodeInForUpdate(@Param("skuCodes") List<String> skuCodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select inventory from Inventory inventory where inventory.skuCode = :skuCode")
    Optional<Inventory> findBySkuCodeForUpdate(@Param("skuCode") String skuCode);

}
