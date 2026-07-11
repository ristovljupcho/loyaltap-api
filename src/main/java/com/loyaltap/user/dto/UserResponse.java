package com.loyaltap.user.dto;

import com.loyaltap.user.model.UserStatus;

import java.time.Instant;

public record UserResponse(
        String id,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
