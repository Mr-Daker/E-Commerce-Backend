package com.shoppingapplication.inventoryservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.CascadeType;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "t_inventory_deduction")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDeduction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String orderNumber;
    @Enumerated(EnumType.STRING)
    private InventoryReservationStatus status;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryDeductionLine> items;
}
