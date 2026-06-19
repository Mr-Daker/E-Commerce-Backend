package com.shoppingapplication.cartservice.repository;
import com.shoppingapplication.cartservice.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface CartRepository extends MongoRepository<Cart, String> {}
