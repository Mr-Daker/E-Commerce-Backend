package com.shoppingapplication.checkoutservice.controller;
import com.fasterxml.jackson.databind.JsonNode; import com.shoppingapplication.checkoutservice.service.CheckoutService; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/checkout") @RequiredArgsConstructor public class CheckoutController {
 private final CheckoutService service; @PostMapping public JsonNode checkout(@RequestHeader("X-User-Id") String user,@RequestHeader("Idempotency-Key") String key){return service.checkout(user,key);}
}
