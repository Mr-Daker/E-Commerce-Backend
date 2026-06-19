package com.shoppingapplication.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void contextLoads() {
	}

	@Test
	void fallbackReturnsServiceUnavailableResponse() {
		webTestClient.get()
				.uri("/fallback/order-service")
				.headers(headers -> headers.setBasicAuth("shop-user", "change-me"))
				.exchange()
				.expectStatus().isEqualTo(503)
				.expectBody()
				.jsonPath("$.status").isEqualTo(503)
				.jsonPath("$.message").isEqualTo("order-service is temporarily unavailable");
	}

	@Test
	void fallbackSupportsPutDeleteAndPatch() {
		webTestClient.put().uri("/fallback/product-service").headers(h -> h.setBasicAuth("shop-user", "change-me")).exchange().expectStatus().isEqualTo(503);
		webTestClient.delete().uri("/fallback/product-service").headers(h -> h.setBasicAuth("shop-user", "change-me")).exchange().expectStatus().isEqualTo(503);
		webTestClient.patch().uri("/fallback/inventory-service").headers(h -> h.setBasicAuth("shop-user", "change-me")).exchange().expectStatus().isEqualTo(503);
	}

	@Test
	void gatewayDoesNotExposeInventoryMutationEndpoints() {
		webTestClient.post().uri("/api/inventory/reserve").headers(h -> h.setBasicAuth("shop-user", "change-me")).exchange().expectStatus().isNotFound();
	}

	@Test
	void applicationRoutesRequireAuthentication() {
		webTestClient.get().uri("/api/product").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void openApiContractIsServed() {
		webTestClient.get()
				.uri("/openapi.yaml")
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.value(body -> org.assertj.core.api.Assertions.assertThat(body)
						.contains("Shopping Application API")
						.contains("/api/order/{orderNumber}/cancel:")
						.contains("Ignored when supplied; the product service provides the authoritative price."));
	}
}
