package com.taskflow.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String role,
        String name,
        String email
) {
    public AuthResponse(String token, String tokenType) {
        this(token, tokenType, null, null, null);
    }
}
