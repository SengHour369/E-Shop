package com.example.eshop.auth.security.filter;

import com.example.eshop.auth.jwt.JwtConfig;
import com.example.eshop.auth.jwt.JwtService;
import com.example.eshop.auth.util.CustomMessageExceptionUtils;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
// ត្រួតពិនិត្យ Token
public class JwtAuthenticationInternalFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final JwtConfig jwtConfig;
    private final com.example.eshop.common.audit.AuditSecurityHandlers auditSecurityHandlers;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var accessToken = request.getHeader(jwtConfig.getHeader());
        log.info("Do filter uri {}", request.getRequestURI());

        if(accessToken !=null && !accessToken.isBlank() && accessToken.startsWith(jwtConfig.getPrefix())) {

            accessToken = accessToken.substring((jwtConfig.getPrefix()).length()).trim();


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
                        authenticationToken.setDetails(new java.util.HashMap<>(claims));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }catch (Exception ex){
                log.warn("Access token validation failed");
                auditSecurityHandlers.reject(request, response, 401);
                return;
            }
        }

        log.info("Do filter {}", request.getRequestURI());
        filterChain.doFilter(request, response);

    }

}
