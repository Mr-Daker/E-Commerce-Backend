package com.shoppingapplication.cartservice.controller;
import com.shoppingapplication.cartservice.dto.ApiResponse;
import com.shoppingapplication.cartservice.dto.CartItemRequest;
import com.shoppingapplication.cartservice.model.Cart;
import com.shoppingapplication.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
@RestController @RequestMapping("/api/cart") @RequiredArgsConstructor
public class CartController {
 private final CartService service;
 @GetMapping public ApiResponse<Cart> get(@RequestHeader("X-User-Id") String user){return ApiResponse.success("Cart fetched",service.get(valid(user)));}
 @PutMapping("/items/{sku}") public ApiResponse<Cart> put(@RequestHeader("X-User-Id") String user,@PathVariable String sku,@Valid @RequestBody CartItemRequest request){return ApiResponse.success("Cart updated",service.putItem(valid(user),sku,request.getQuantity()));}
 @DeleteMapping("/items/{sku}") public ApiResponse<Cart> remove(@RequestHeader("X-User-Id") String user,@PathVariable String sku){return ApiResponse.success("Cart updated",service.removeItem(valid(user),sku));}
 @DeleteMapping public void clear(@RequestHeader("X-User-Id") String user){service.clear(valid(user));}
 private String valid(String user){if(user==null||user.isBlank())throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Missing user identity");return user;}
}
