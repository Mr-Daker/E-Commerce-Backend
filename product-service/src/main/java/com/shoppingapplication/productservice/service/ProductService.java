package com.shoppingapplication.productservice.service;


import com.shoppingapplication.productservice.dto.PagedResponse;
import com.shoppingapplication.productservice.dto.ProductRequest;
import com.shoppingapplication.productservice.dto.ProductResponse;
import com.shoppingapplication.productservice.model.Product;
import com.shoppingapplication.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of("name", "skuCode", "price");
    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest productRequest){

        if (productRepository.existsBySkuCode(productRequest.getSkuCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists");
        }

        Product product = Product.builder()
                .skuCode(productRequest.getSkuCode())
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .build();

        Product savedProduct;
        try {
            savedProduct = productRepository.save(product);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists", exception);
        }
        log.info("Product {} is saved",product.getId());
        return mapToProductResponse(savedProduct);
    }

    public PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String query){
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater and size must be at least one");
        }
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortBy must be one of: name, skuCode, price");
        }

        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(sortBy).ascending());
        Page<Product> productPage = query == null || query.isBlank()
                ? productRepository.findAll(pageRequest)
                : productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageRequest);
        Page<ProductResponse> products = productPage
                .map(this::mapToProductResponse);
        return PagedResponse.from(products);
    }

    public ProductResponse getProductById(String id){
        return productRepository.findById(id)
                .map(this::mapToProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public ProductResponse getProductBySkuCode(String skuCode){
        return productRepository.findBySkuCode(skuCode)
                .map(this::mapToProductResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public ProductResponse updateProduct(String id, ProductRequest productRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        productRepository.findBySkuCode(productRequest.getSkuCode())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists");
                });

        product.setSkuCode(productRequest.getSkuCode());
        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        try {
            return mapToProductResponse(productRepository.save(product));
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A product with this SKU already exists", exception);
        }
    }

    public void deleteProduct(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
    }


    private ProductResponse mapToProductResponse(Product product){
        return ProductResponse.builder()
                .id(product.getId())
                .skuCode(product.getSkuCode())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();

    }


}
