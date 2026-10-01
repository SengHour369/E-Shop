package com.example.eshop.payment.config;

import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import com.example.eshop.common.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * payment-service is not the token issuer (auth-service is): this filter only validates the
 * JWT signature/expiry and rebuilds the Authentication from its claims, without any DB lookup.
 */
@Configuration
@RequiredArgsConstructor
@lombok.RequiredArgsConstructor
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
                        .requestMatchers("/api/v1/payments/**").authenticated()
                        .requestMatchers("/api/v1/payment-transactions/**").authenticated()
                        .requestMatchers("/api/v1/bakong/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                auditSecurityHandlers.reject(request, response, 401))
                )
                .addFilterBefore(
                        new JwtAuthFilter(jwtTokenValidator, jwtProperties),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
