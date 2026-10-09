package com.example.eshop.ai.config;

import com.example.eshop.common.audit.AuditSecurityHandlers;
import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import com.example.eshop.common.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, JwtTokenValidator validator, JwtProperties jwt,
            AuditSecurityHandlers handlers) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/ai/chat").permitAll()
                        .requestMatchers("/api/admin/audit-logs/**").hasAnyAuthority("ADMIN", "AUDIT_READ")
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> handlers.reject(request, response, 401))
                        .accessDeniedHandler((request, response, ex) -> handlers.reject(request, response, 403)))
                .addFilterBefore(new JwtAuthFilter(validator, jwt), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
