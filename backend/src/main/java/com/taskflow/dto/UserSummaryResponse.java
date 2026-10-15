package com.taskflow.dto;

import com.taskflow.entity.User;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        String createdAt
) {
    public static UserSummaryResponse fromEntity(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : "STUDENT",
                user.isActive(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        );
    }
}
