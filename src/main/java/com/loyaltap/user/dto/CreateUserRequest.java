package com.loyaltap.user.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String id) {
}
