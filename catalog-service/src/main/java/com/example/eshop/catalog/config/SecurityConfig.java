package com.example.eshop.catalog.config;

import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import com.example.eshop.common.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * catalog-service is not the token issuer (auth-service is) — it only needs to trust and
 * verify JWTs minted elsewhere. common-lib's {@link JwtAuthFilter} does exactly that: it
 * validates signature/expiry and rebuilds the Authentication purely from token claims, with
 * no database lookup.
 *
 * common-lib beans (JwtProperties, JwtTokenValidator, GlobalExceptionHandler) are picked up via
 * CatalogServiceApplication's @ComponentScan, which covers both com.example.eshop.catalog and
 * com.example.eshop.common.
 */
@Configuration
@EnableMethodSecurity
@lombok.RequiredArgsConstructor
public class SecurityConfig {

    // Same public path list as the monolith's SecurityConfig.PUBLIC_PATHS, filtered down to the
    // catalog/product/category/subcategory browsing endpoints that live in this service.
    public static final String[] PUBLIC_PATHS = {
            "/internal/ai/products",
            "/internal/ai/products/*",
            "/internal/ai/skus/*",
            "/swagger-ui/**",
            "/api/v1/promotions/**",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/api/v1/products/get/all",
            "/api/v1/products/active",
            "/api/v1/products/search",
            "/api/v1/products/subcategory/**",
            "/api/v1/products/category/**",
            "/api/v1/categories/get/all",
            "/api/v1/categories/id/get/",
            "/api/v1/categories/name/",
            "/api/v1/categories/with-subcategories",
            "/api/v1/subcategories/get/all"
    };

    private final com.example.eshop.common.audit.AuditSecurityHandlers auditSecurityHandlers;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            JwtTokenValidator jwtTokenValidator,
                                            JwtProperties jwtProperties) throws Exception {

        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtTokenValidator, jwtProperties);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/admin/audit-logs/**").hasAnyAuthority("ADMIN", "AUDIT_READ")
                        .requestMatchers("/api/v1/inventory/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/attributes/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/attribute-values/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                auditSecurityHandlers.reject(request, response, 401))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                auditSecurityHandlers.reject(request, response, 403))
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
