package com.shoppingapplication.checkoutservice;
import com.fasterxml.jackson.databind.JsonNode; import com.shoppingapplication.checkoutservice.service.CheckoutService; import org.junit.jupiter.api.Test; import org.springframework.http.*; import org.springframework.test.util.ReflectionTestUtils; import org.springframework.web.reactive.function.client.*; import reactor.core.publisher.Mono;
import java.util.*; import static org.assertj.core.api.Assertions.assertThat;
class CheckoutServiceTests {
 @Test void submitsCartWithIdentityAndIdempotencyThenClearsIt(){
  List<ClientRequest> requests=new ArrayList<>(); WebClient.Builder client=WebClient.builder().exchangeFunction(r->{requests.add(r);String p=r.url().getPath();String body=p.equals("/api/cart")&&r.method()==HttpMethod.GET?"{\"data\":{\"items\":[{\"skuCode\":\"SKU-1\",\"quantity\":2}]}}":p.equals("/api/order")?"{\"data\":{\"orderNumber\":\"order-1\",\"status\":\"CONFIRMED\"}}":"";return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type","application/json").body(body).build());});
  CheckoutService service=new CheckoutService(client); ReflectionTestUtils.setField(service,"cartUrl","http://cart-service"); ReflectionTestUtils.setField(service,"orderUrl","http://order-service");
  JsonNode result=service.checkout("alice","key-1"); assertThat(result.path("data").path("status").asText()).isEqualTo("CONFIRMED"); assertThat(requests).hasSize(3); assertThat(requests.get(1).headers().getFirst("Idempotency-Key")).isEqualTo("key-1"); assertThat(requests.get(1).headers().getFirst("X-User-Id")).isEqualTo("alice");
 }
}
