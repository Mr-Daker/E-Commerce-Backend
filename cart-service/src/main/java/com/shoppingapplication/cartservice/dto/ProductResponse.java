package com.shoppingapplication.cartservice.dto;
import lombok.Data;
import java.math.BigDecimal;
@Data public class ProductResponse { private String skuCode; private String name; private BigDecimal price; }
