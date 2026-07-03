package com.loyaltap.reward.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateRewardRequest(
        @NotBlank @Size(max = 150) String userId,
        @NotBlank @Size(max = 150) String name,
        String description,
        @Positive int requiredPoints,
        @PositiveOrZero Integer stockQuantity
) {
}
