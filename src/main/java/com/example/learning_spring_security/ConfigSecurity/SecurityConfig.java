package com.example.learning_spring_security.ConfigSecurity;

import com.example.learning_spring_security.Exception.CustomDeniedHandler;

import com.example.learning_spring_security.JWT.JwtConfig;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Security.UserDetailsService;
import com.example.learning_spring_security.Security.filter.CustomAuthenticationProvider;
import com.example.learning_spring_security.Security.filter.JwtAuthenticationFilter;
import com.example.learning_spring_security.Security.filter.JwtAuthenticationInternalFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String[] PUBLIC_PATHS = {
            "/api/v1/public/**",
            "/api/v1/auth/**",
            "/swagger-ui/**",
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

    private final UserDetailsService customUserDetailService;
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authenticationProvider(customAuthenticationProvider)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()

                        .requestMatchers("/api/v1/user/**").authenticated()
                        .requestMatchers("/api/v1/cart/**").authenticated()
                        .requestMatchers("/api/v1/orders/**").authenticated()
                        .requestMatchers("/api/v1/addresses/**").authenticated()
                        .requestMatchers("/api/v1/payments/**").authenticated()


                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/inventory/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/attributes/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/v1/attribute-values/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                        .accessDeniedHandler(new CustomDeniedHandler())
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtService,
                                objectMapper,
                                jwtConfig,
                                authenticationManager,
                                customUserDetailService
                        ),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        new JwtAuthenticationInternalFilter(
                                jwtService,
                                objectMapper,
                                jwtConfig
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

}