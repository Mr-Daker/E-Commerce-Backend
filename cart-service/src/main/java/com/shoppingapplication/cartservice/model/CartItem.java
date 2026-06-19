package com.shoppingapplication.cartservice.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Data @NoArgsConstructor @AllArgsConstructor
public class CartItem { private String skuCode; private String name; private BigDecimal unitPrice; private Integer quantity; }
