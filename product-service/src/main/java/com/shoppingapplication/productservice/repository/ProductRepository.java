package com.shoppingapplication.productservice.repository;

import com.shoppingapplication.productservice.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product,String> {
    Optional<Product> findBySkuCode(String skuCode);
    boolean existsBySkuCode(String skuCode);
    Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
}
