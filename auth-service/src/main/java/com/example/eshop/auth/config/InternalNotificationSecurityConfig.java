package com.example.eshop.auth.config;
import com.example.eshop.common.jwt.*;
import com.example.eshop.common.security.JwtAuthFilter;
import com.example.eshop.common.audit.AuditSecurityHandlers;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
/** Machine identities use the existing signed-claim filter; they are not human auth database accounts. */
@Configuration
public class InternalNotificationSecurityConfig {
    @Bean @Order(1)
    SecurityFilterChain notificationDeliverySecurity(HttpSecurity http, JwtTokenValidator validator,
            JwtProperties jwt, AuditSecurityHandlers handlers) throws Exception {
        return http.securityMatcher("/internal/notifications/**")
            .csrf(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a->a.anyRequest().hasAuthority("SERVICE_NOTIFICATION"))
            .exceptionHandling(e->e.authenticationEntryPoint((r,s,x)->handlers.reject(r,s,401)).accessDeniedHandler((r,s,x)->handlers.reject(r,s,403)))
            .addFilterBefore(new JwtAuthFilter(validator,jwt),UsernamePasswordAuthenticationFilter.class).build();
    }
}
