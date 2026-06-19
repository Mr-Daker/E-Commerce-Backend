package com.shoppingapplication.checkoutservice.service;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; import org.springframework.http.*; import org.springframework.stereotype.Service; import org.springframework.web.reactive.function.client.WebClient; import org.springframework.web.server.ResponseStatusException;
import java.time.Duration; import java.util.*;
@Service @RequiredArgsConstructor
public class CheckoutService {
 private final WebClient.Builder webClientBuilder;
 @Value("${cart.service.url:http://cart-service}") private String cartUrl; @Value("${order.service.url:http://order-service}") private String orderUrl;
 public JsonNode checkout(String user,String key){
  JsonNode cart=webClientBuilder.build().get().uri(cartUrl+"/api/cart").header("X-User-Id",user).retrieve().bodyToMono(JsonNode.class).timeout(Duration.ofSeconds(5)).block();
  JsonNode items=cart==null?null:cart.path("data").path("items"); if(items==null||!items.isArray()||items.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cart is empty");
  List<Map<String,Object>> lines=new ArrayList<>(); items.forEach(item->lines.add(Map.of("skuCode",item.path("skuCode").asText(),"quantity",item.path("quantity").asInt())));
  JsonNode order=webClientBuilder.build().post().uri(orderUrl+"/api/order").header("X-User-Id",user).header("Idempotency-Key",key).bodyValue(Map.of("orderLineDtoList",lines)).retrieve().bodyToMono(JsonNode.class).timeout(Duration.ofSeconds(15)).block();
  webClientBuilder.build().delete().uri(cartUrl+"/api/cart").header("X-User-Id",user).retrieve().toBodilessEntity().timeout(Duration.ofSeconds(5)).block(); return order;
 }
}
