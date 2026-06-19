package com.shoppingapplication.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoppingapplication.productservice.dto.ProductRequest;
import com.shoppingapplication.productservice.model.Product;
import com.shoppingapplication.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@MockBean
	private ProductRepository productRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void createProductReturnsCreatedProduct() throws Exception {
		Product product = Product.builder()
				.id("product-id")
				.skuCode("Iphone_13")
				.name("iPhone 13")
				.description("Apple smartphone")
				.price(BigDecimal.valueOf(699.99))
				.build();

		when(productRepository.save(any(Product.class))).thenReturn(product);

		ProductRequest request = ProductRequest.builder()
				.skuCode("Iphone_13")
				.name("iPhone 13")
				.description("Apple smartphone")
				.price(BigDecimal.valueOf(699.99))
				.build();

		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.skuCode").value("Iphone_13"));
	}

	@Test
	void createProductRejectsInvalidPayload() throws Exception {
		ProductRequest request = ProductRequest.builder()
				.name("")
				.price(BigDecimal.ZERO)
				.build();

		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void getProductBySkuCodeReturnsProduct() throws Exception {
		Product product = Product.builder()
				.id("product-id")
				.skuCode("Iphone_13")
				.name("iPhone 13")
				.description("Apple smartphone")
				.price(BigDecimal.valueOf(699.99))
				.build();

		when(productRepository.findBySkuCode("Iphone_13")).thenReturn(Optional.of(product));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/product/sku/Iphone_13"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.skuCode").value("Iphone_13"));
	}

	@Test
	void getAllProductsReturnsPagedResponse() throws Exception {
		Product product = Product.builder()
				.id("product-id")
				.skuCode("Iphone_13")
				.name("iPhone 13")
				.description("Apple smartphone")
				.price(BigDecimal.valueOf(699.99))
				.build();

		when(productRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/product?page=0&size=10&sortBy=name"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.content[0].skuCode").value("Iphone_13"))
				.andExpect(jsonPath("$.data.page").value(0));
	}

	@Test
	void getAllProductsRejectsUnknownSortField() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/product?sortBy=unknown"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("sortBy must be one of: name, skuCode, price"));
	}
}
