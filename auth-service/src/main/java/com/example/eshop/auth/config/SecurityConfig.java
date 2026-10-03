package com.example.eshop.auth.config;

import com.example.eshop.auth.exception.CustomDeniedHandler;

import com.example.eshop.auth.jwt.JwtConfig;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.security.UserDetailsService;
import com.example.eshop.auth.security.filter.CustomAuthenticationProvider;
import com.example.eshop.auth.security.filter.JwtAuthenticationFilter;
import com.example.eshop.auth.security.filter.JwtAuthenticationInternalFilter;
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
            "/webjars/**"
    };

    private final UserDetailsService customUserDetailService;
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;
    private final com.example.eshop.common.audit.AuditLogService audit;


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    private final com.example.eshop.common.audit.AuditSecurityHandlers auditSecurityHandlers;

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
                        .requestMatchers("/api/admin/audit-logs/**").hasAnyAuthority("ADMIN", "AUDIT_READ")

                        .requestMatchers("/api/v1/user/**").authenticated()
                        .requestMatchers("/api/v1/addresses/**").authenticated()
                        .requestMatchers("/api/v1/groups/**").authenticated()
                        .requestMatchers("/api/v1/permissions/**").authenticated()
                        .requestMatchers("/api/v1/user-groups/**").authenticated()
                        .requestMatchers("/api/v1/user-permissions/**").authenticated()
                        .requestMatchers("/api/v1/group-permissions/**").authenticated()
                        .requestMatchers("/api/v1/functions/**").authenticated()

                        .requestMatchers("/api/v1/admin/**").hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                auditSecurityHandlers.reject(request, response, 401))
                        .accessDeniedHandler((request, response, exception) -> auditSecurityHandlers.reject(request, response, 403))
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtService,
                                objectMapper,
                                jwtConfig,
                                authenticationManager,
                                customUserDetailService,
                                audit
                        ),
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        new JwtAuthenticationInternalFilter(
                                jwtService,
                                objectMapper,
                                jwtConfig,
                                auditSecurityHandlers
                        ),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

}