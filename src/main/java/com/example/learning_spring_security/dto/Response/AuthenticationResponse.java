package com.example.learning_spring_security.dto.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationResponse(
        Long id,
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        String email,
        String username,
        String role,
        String message
) {

    public AuthenticationResponse(Long id, String accessToken, String refreshToken) {
        this(id, accessToken, refreshToken, "Bearer", null, null, null, null);
    }


    public AuthenticationResponse(Long id, String accessToken, String refreshToken,
                                  String tokenType, String email, String username,
                                  String role, String message) {
        this.id = id;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.email = email;
        this.username = username;
        this.role = role;
        this.message = message;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private String email;
        private String username;
        private String role;
        private String message;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
        public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public AuthenticationResponse build() {
            return new AuthenticationResponse(id, accessToken, refreshToken, tokenType, email, username, role, message);
        }

    }
}