
package com.taskflow.dto;

public record AuthResponse(
        String token,
        String tokenType
) {
}
