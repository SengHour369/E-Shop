package com.example.eshop.auth.jwt;

import com.example.eshop.auth.security.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Key;

public interface JwtService {

    Claims extractClaims(String token);
    Key getKey();
    String generateToken(UserDetailsImpl customUserDetail);
    String refreshToken(UserDetailsImpl customUserDetail);
    boolean isValidToken(String token);
}