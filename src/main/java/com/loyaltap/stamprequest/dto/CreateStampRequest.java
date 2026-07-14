package com.loyaltap.stamprequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStampRequest(
        @NotNull UUID membershipId,
        @NotBlank String userId,
        @NotBlank String idempotencyKey
) {
}
