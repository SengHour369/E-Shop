package com.example.eshop.order.config;

import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import com.example.eshop.common.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * order-service is not the token issuer (auth-service is) so it only ever needs to verify a
 * JWT's signature/claims — that's what common-lib's stateless {@link JwtAuthFilter} does.
 */
@Configuration
@RequiredArgsConstructor
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/webjars/**"
    };

    private final JwtTokenValidator jwtTokenValidator;
    private final JwtProperties jwtProperties;

    private final com.example.eshop.common.audit.AuditSecurityHandlers auditSecurityHandlers;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/admin/audit-logs/**").hasAnyAuthority("ADMIN", "AUDIT_READ")
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/api/v1/cancelations/**").authenticated()
                        .requestMatchers("/api/v1/returns/**").authenticated()
                        .requestMatchers("/api/v1/refunds/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                auditSecurityHandlers.reject(request, response, 401))
                        .accessDeniedHandler((request, response, exception) -> auditSecurityHandlers.reject(request, response, 403))
                )
                .addFilterBefore(
                        new JwtAuthFilter(jwtTokenValidator, jwtProperties),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
