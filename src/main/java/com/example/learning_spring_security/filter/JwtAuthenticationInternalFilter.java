package com.example.learning_spring_security.filter;

import com.example.learning_spring_security.JWT.JwtConfig;
import com.example.learning_spring_security.JWT.JwtService;
import com.example.learning_spring_security.utils.CustomMessageExceptionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationInternalFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;

    // ✅ List of public paths that don't need JWT validation
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-ui.html",
            "/webjars",
            "/api/v1/public",
            "/health",
            "/"
    );

    // ✅ Add this method to skip filter for public paths
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean isPublicPath = PUBLIC_PATHS.stream().anyMatch(path::startsWith);

        if (isPublicPath) {
            log.info("Skipping JWT filter for public path: {}", path);
        }

        return isPublicPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var accessToken = request.getHeader(jwtConfig.getHeader());
        log.info("Processing JWT filter for uri: {}", request.getRequestURI());

        if(accessToken != null && !accessToken.isBlank() && accessToken.startsWith(jwtConfig.getPrefix())) {

            accessToken = accessToken.substring((jwtConfig.getPrefix()).length());

            try {
                if(jwtService.isValidToken(accessToken)){
                    Claims claims = jwtService.extractClaims(accessToken);
                    var username = claims.getSubject();

                    List<String> authorities = claims.get("authorities", List.class);
                    if(username != null) {
                        UsernamePasswordAuthenticationToken authenticationToken =
                                new UsernamePasswordAuthenticationToken(
                                        username, null,
                                        authorities.stream().map(SimpleGrantedAuthority::new)
                                                .collect(Collectors.toList())
                                );
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        log.info("Authentication set for user: {}", username);
                    }
                }
            } catch (Exception ex){
                log.error("JWT validation failed: {}", ex.getLocalizedMessage());

                var messageException = CustomMessageExceptionUtils.unauthorized();
                var msgJson = objectMapper.writeValueAsString(messageException);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(msgJson);
                return;
            }
        } else {
            log.info("No JWT token found in request for: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}