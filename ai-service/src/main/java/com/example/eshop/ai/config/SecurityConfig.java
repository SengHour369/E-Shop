package com.example.eshop.ai.config;
import com.example.eshop.common.jwt.*;
import com.example.eshop.common.security.JwtAuthFilter;
import com.example.eshop.common.audit.AuditSecurityHandlers;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain security(HttpSecurity http, JwtTokenValidator validator, JwtProperties jwt, AuditSecurityHandlers handlers) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.requestMatchers("/api/admin/audit-logs/**").hasAnyAuthority("ADMIN","AUDIT_READ").anyRequest().authenticated())
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->handlers.reject(r,s,401)).accessDeniedHandler((r,s,x)->handlers.reject(r,s,403)))
            .addFilterBefore(new JwtAuthFilter(validator,jwt),UsernamePasswordAuthenticationFilter.class).build();
    }
}
