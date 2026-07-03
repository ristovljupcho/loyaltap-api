package com.loyaltap.reward.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateRewardRequest(
        @Size(max = 150) String name,
        String description,
        @Positive Integer requiredPoints,
        @PositiveOrZero Integer stockQuantity
) {
}
