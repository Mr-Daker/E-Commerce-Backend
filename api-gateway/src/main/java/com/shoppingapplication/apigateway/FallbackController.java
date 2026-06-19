package com.shoppingapplication.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/{serviceName}")
    public ResponseEntity<Map<String, Object>> fallback(@PathVariable String serviceName) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                        "error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                        "message", serviceName + " is temporarily unavailable"
                ));
    }
}
