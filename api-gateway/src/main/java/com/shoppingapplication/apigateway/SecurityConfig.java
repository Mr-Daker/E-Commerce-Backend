package com.shoppingapplication.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/openapi.yaml").permitAll()
                        .anyExchange().authenticated())
                .httpBasic().and()
                .build();
    }

    @Bean
    MapReactiveUserDetailsService users(
            @Value("${app.security.username:shop-user}") String username,
            @Value("${app.security.password:change-me}") String password) {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new MapReactiveUserDetailsService(User.withUsername(username)
                .password(encoder.encode(password))
                .roles("USER")
                .build());
    }
}
