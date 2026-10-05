package com.example.eshop.admin.config;

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

/**
 * admin-service trusts access tokens minted by auth-service. It does not look up users.
 */
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
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/admin/audit-logs/**").hasAnyAuthority("ADMIN", "AUDIT_READ")
                        .requestMatchers("/api/v1/admin/session", "/api/v1/admin/monitor").hasAnyAuthority("ADMIN", "MANAGER")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, error) -> handlers.reject(request, response, 401))
                        .accessDeniedHandler((request, response, error) -> handlers.reject(request, response, 403)))
                .addFilterBefore(new JwtAuthFilter(validator, jwt), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
