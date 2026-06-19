package com.shoppingapplication.cartservice.service;
import com.shoppingapplication.cartservice.dto.ApiResponse;
import com.shoppingapplication.cartservice.dto.ProductResponse;
import com.shoppingapplication.cartservice.model.Cart;
import com.shoppingapplication.cartservice.model.CartItem;
import com.shoppingapplication.cartservice.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
@Service @RequiredArgsConstructor
public class CartService {
 private final CartRepository repository; private final WebClient.Builder webClientBuilder;
 @Value("${product.service.url:http://product-service}") private String productServiceUrl;
 public Cart get(String userId){ return repository.findById(userId).orElseGet(() -> { Cart c=new Cart(); c.setUserId(userId); return c;}); }
 public Cart putItem(String userId,String sku,Integer quantity){
  ProductResponse product=fetchProduct(sku); Cart cart=get(userId); if(cart.getItems()==null) cart.setItems(new ArrayList<>());
  cart.getItems().removeIf(item -> item.getSkuCode().equals(sku)); cart.getItems().add(new CartItem(sku,product.getName(),product.getPrice(),quantity)); cart.setUpdatedAt(Instant.now()); return repository.save(cart);
 }
 public Cart removeItem(String userId,String sku){ Cart cart=get(userId); cart.getItems().removeIf(i->i.getSkuCode().equals(sku)); cart.setUpdatedAt(Instant.now()); return repository.save(cart); }
 public void clear(String userId){ repository.deleteById(userId); }
 private ProductResponse fetchProduct(String sku){
  try { ApiResponse<ProductResponse> response=webClientBuilder.build().get().uri(productServiceUrl+"/api/product/sku/{sku}",sku).retrieve().bodyToMono(new ParameterizedTypeReference<ApiResponse<ProductResponse>>(){}).timeout(Duration.ofSeconds(3)).block();
   if(response==null||response.getData()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Product not found"); return response.getData();
  } catch(ResponseStatusException e){throw e;} catch(RuntimeException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Product service unavailable",e);} }
}
