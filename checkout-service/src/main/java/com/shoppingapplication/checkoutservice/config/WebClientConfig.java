package com.shoppingapplication.checkoutservice.config;
import org.springframework.cloud.client.loadbalancer.LoadBalanced; import org.springframework.context.annotation.*; import org.springframework.web.reactive.function.client.WebClient;
@Configuration public class WebClientConfig { @Bean @LoadBalanced WebClient.Builder webClientBuilder(){return WebClient.builder();} }
