package com.shoppingapplication.cartservice.model;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Document("carts") @Data
public class Cart { @Id private String userId; private List<CartItem> items = new ArrayList<>(); private Instant updatedAt; }
