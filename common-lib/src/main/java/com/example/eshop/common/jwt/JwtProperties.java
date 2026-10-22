package com.example.eshop.common.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${jwt.header:Authorization}")
    private String header;

    @Value("${jwt.prefix:Bearer}")
    private String prefix;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${authorization.auth-url:${AUTHORIZATION_AUTH_URL:http://auth-service:8081}}")
    private String authorizationUrl;
    public String getAuthorizationUrl() { return authorizationUrl; }

    public String getHeader() { return header; }
    public String getPrefix() { return prefix; }
    public String getSecret() { return secret; }
}
