package com.example.eshop.common.security;

import com.example.eshop.common.jwt.JwtProperties;
import com.example.eshop.common.jwt.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the JWT signature/expiry and rebuilds the Authentication purely from the
 * token claims (subject + authorities). No user lookup, no dependency on auth-service
 * being reachable at request time.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenValidator tokenValidator;
    private final JwtProperties properties;

    public JwtAuthFilter(JwtTokenValidator tokenValidator, JwtProperties properties) {
        this.tokenValidator = tokenValidator;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(properties.getHeader());
        String prefix = properties.getPrefix() + " ";

        if (header != null && header.startsWith(prefix)) {
            String token = header.substring(prefix.length());
            try {
                Claims claims = tokenValidator.parseClaims(token);
                if ("refresh".equals(claims.get("type")) || !Boolean.TRUE.equals(claims.get("isEnable")))
                    throw new IllegalArgumentException("An enabled access token is required");
                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> authorityClaims = claims.get("authorities", List.class);
                Collection<? extends GrantedAuthority> authorities = authorityClaims == null
                        ? List.of()
                        : authorityClaims.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                authentication.setDetails(new java.util.HashMap<>(claims));
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
