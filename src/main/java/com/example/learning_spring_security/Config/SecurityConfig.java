package com.example.learning_spring_security.Config;

import com.example.learning_spring_security.Exception.CustomDeniedHandler;
import com.example.learning_spring_security.JWT.JwtConfig;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.Security.UserDetailsService;
import com.example.learning_spring_security.filter.CustomAuthenticationProvider;

import com.example.learning_spring_security.filter.JwtAuthenticationFilter;
import com.example.learning_spring_security.filter.JwtAuthenticationInternalFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService customUserDetailService;
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;

    //  Password encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    //  AuthenticationManager injected via Spring
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // Security filter chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {

        // Configure HttpSecurity
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(customAuthenticationProvider)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //  Public paths
                        .requestMatchers(
                                "/",
                                "/health",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/webjars/**",
                                "/api/v1/public/**"
                        ).permitAll()

                        .requestMatchers("/api/v1/user/**").hasAnyAuthority("USER", "ADMIN")
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )

                //  .authenticationProvider(customAuthenticationProvider) // Custom auth provider
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler(new CustomDeniedHandler())
                )
                // JWT filters
                // បន្ថែម Custom Filters
                //============================
                // ត្រួតពិនិត្យការចូល
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
                // ត្រួតពិនិត្យ Token
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

    @Configuration
    public class WebConfig implements WebMvcConfigurer {

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/swagger-ui/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/springdoc-openapi-ui/")
                    .resourceChain(false);

            registry.addResourceHandler("/webjars/**")
                    .addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
    }
}
