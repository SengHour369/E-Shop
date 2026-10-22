package com.example.eshop.auth.config;

import com.example.eshop.common.audit.AuditSecurityHandlers;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import com.example.eshop.common.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Internal commerce callers are signed machine identities, not customer accounts. */
@Configuration
public class InternalCommerceSecurityConfig {
    @Bean
    @Order(2)
    SecurityFilterChain commerceSecurity(HttpSecurity http, JwtTokenValidator validator,
            JwtProperties jwt, AuditSecurityHandlers handlers) throws Exception {
        return http.securityMatcher("/internal/commerce/**")
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().hasAuthority("SERVICE_ORDER"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((r, s, x) -> handlers.reject(r, s, 401))
                        .accessDeniedHandler((r, s, x) -> handlers.reject(r, s, 403)))
                .addFilterBefore(new JwtAuthFilter(validator, jwt), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
