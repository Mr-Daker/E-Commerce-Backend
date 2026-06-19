package com.shoppingapplication.inventoryservice.repository;

import com.shoppingapplication.inventoryservice.model.InventoryDeduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface InventoryDeductionRepository extends JpaRepository<InventoryDeduction, Long> {
    boolean existsByOrderNumber(String orderNumber);
    Optional<InventoryDeduction> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select deduction from InventoryDeduction deduction where deduction.orderNumber = :orderNumber")
    Optional<InventoryDeduction> findByOrderNumberForUpdate(@Param("orderNumber") String orderNumber);
}
