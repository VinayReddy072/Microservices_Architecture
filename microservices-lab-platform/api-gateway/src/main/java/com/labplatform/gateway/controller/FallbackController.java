package com.labplatform.gateway.controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.Map;
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @GetMapping("/booking")
    public Mono<ResponseEntity<Map<String, Object>>> bookingFallback() {
        log.warn("Circuit breaker opened for booking-service — returning fallback response");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", "Booking service is temporarily unavailable. Please try again in a few moments.",
                "timestamp", LocalDateTime.now().toString(),
                "service", "booking-service"
        )));
    }
    @GetMapping("/equipment")
    public Mono<ResponseEntity<Map<String, Object>>> equipmentFallback() {
        log.warn("Circuit breaker opened for equipment-service — returning fallback response");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false,
                "message", "Equipment service is temporarily unavailable. Please try again in a few moments.",
                "timestamp", LocalDateTime.now().toString(),
                "service", "equipment-service"
        )));
    }
}
