package com.example.learning_spring_security.JWT;

import com.example.learning_spring_security.Security.UserDetailsImpl;
import io.jsonwebtoken.Claims;

import java.security.Key;

public interface JwtService {

    Claims extractClaims(String token);
    Key getKey();
    String generateToken(UserDetailsImpl customUserDetail);
    String refreshToken(UserDetailsImpl customUserDetail);
    boolean isValidToken(String token);
}