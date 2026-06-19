package com.shoppingapplication.e2e;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ShoppingApplicationIT {
    @Container
    static final ComposeContainer ENV = new ComposeContainer(new File("../docker-compose.yml"))
            .withExposedService("api-gateway-1", 9003, Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(8)))
            .withLocalCompose(true);

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void checkoutFlowsThroughMongoMysqlRabbitAndNotificationService() throws Exception {
        assertStatus(request("POST", "/api/product", "{\"skuCode\":\"Iphone_13\",\"name\":\"iPhone 13\",\"price\":699.99}"), 201);
        assertStatus(request("PUT", "/api/cart/items/Iphone_13", "{\"quantity\":2}"), 200);
        HttpResponse<String> checkout = request("POST", "/api/checkout", "", "Idempotency-Key", "e2e-checkout-1");
        assertThat(checkout.statusCode()).isEqualTo(200);
        assertThat(checkout.body()).contains("CONFIRMED");

        String notifications = eventually("/api/notification?page=0&size=20", "ORDER_CONFIRMED", Duration.ofSeconds(30));
        assertThat(notifications).contains("ORDER_CONFIRMED").doesNotContain("\"totalElements\":0");
        assertThat(request("GET", "/api/cart", null).body()).contains("\"items\":[]");
    }

    @Test
    void gatewayRejectsOversizedRequests() throws Exception {
        String body = "{\"skuCode\":\"BIG\",\"name\":\"" + "x".repeat(1_100_000) + "\",\"price\":1}";
        assertThat(request("POST", "/api/product", body).statusCode()).isEqualTo(413);
    }

    private String eventually(String path, String expected, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            HttpResponse<String> response = request("GET", path, null);
            if (response.statusCode() == 200 && response.body().contains(expected)) return response.body();
            Thread.sleep(500);
        }
        throw new AssertionError("Timed out waiting for " + expected);
    }

    private HttpResponse<String> request(String method, String path, String body, String... headers) throws Exception {
        int port = ENV.getServicePort("api-gateway-1", 9003);
        String auth = Base64.getEncoder().encodeToString("shop-user:change-me".getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20)).header("Authorization", "Basic " + auth);
        for (int i=0;i<headers.length;i+=2) builder.header(headers[i],headers[i+1]);
        builder.header("Content-Type", "application/json");
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private void assertStatus(HttpResponse<String> response,int status){assertThat(response.statusCode()).as(response.body()).isEqualTo(status);}
}
