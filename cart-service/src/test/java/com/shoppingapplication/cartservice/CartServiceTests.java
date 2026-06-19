package com.shoppingapplication.cartservice;
import com.shoppingapplication.cartservice.model.Cart; import com.shoppingapplication.cartservice.repository.CartRepository; import com.shoppingapplication.cartservice.service.CartService;
import org.junit.jupiter.api.Test; import org.springframework.http.*; import org.springframework.test.util.ReflectionTestUtils; import org.springframework.web.reactive.function.client.*; import reactor.core.publisher.Mono;
import java.util.Optional; import static org.assertj.core.api.Assertions.assertThat; import static org.mockito.ArgumentMatchers.any; import static org.mockito.Mockito.*;
class CartServiceTests {
 @Test void validatesProductAndStoresOwnedCartItem(){
  CartRepository repo=mock(CartRepository.class); when(repo.findById("alice")).thenReturn(Optional.empty()); when(repo.save(any(Cart.class))).thenAnswer(i->i.getArgument(0));
  WebClient.Builder client=WebClient.builder().exchangeFunction(r->Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type","application/json").body("{\"success\":true,\"data\":{\"skuCode\":\"SKU-1\",\"name\":\"Phone\",\"price\":10.00}}").build()));
  CartService service=new CartService(repo,client); ReflectionTestUtils.setField(service,"productServiceUrl","http://product-service");
  Cart cart=service.putItem("alice","SKU-1",2);
  assertThat(cart.getUserId()).isEqualTo("alice"); assertThat(cart.getItems()).singleElement().satisfies(i->{assertThat(i.getSkuCode()).isEqualTo("SKU-1");assertThat(i.getQuantity()).isEqualTo(2);});
 }
}
