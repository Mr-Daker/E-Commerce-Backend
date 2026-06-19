package com.shoppingapplication.productservice.controller;


import com.shoppingapplication.productservice.dto.ApiResponse;
import com.shoppingapplication.productservice.dto.PagedResponse;
import com.shoppingapplication.productservice.dto.ProductRequest;
import com.shoppingapplication.productservice.dto.ProductResponse;
import com.shoppingapplication.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest){
        return ApiResponse.success("Product created", productService.createProduct(productRequest));

    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PagedResponse<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "") String query){
       return ApiResponse.success("Products fetched", productService.getAllProducts(page, size, sortBy, query));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductResponse> getProductById(@PathVariable String id){
        return ApiResponse.success("Product fetched", productService.getProductById(id));
    }

    @GetMapping("/sku/{skuCode}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ProductResponse> getProductBySkuCode(@PathVariable String skuCode){
        return ApiResponse.success("Product fetched", productService.getProductBySkuCode(skuCode));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(@PathVariable String id, @Valid @RequestBody ProductRequest productRequest){
        return ApiResponse.success("Product updated", productService.updateProduct(id, productRequest));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String id){
        productService.deleteProduct(id);
    }

}
