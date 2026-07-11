package com.loyaltap.reward.dto;

import com.loyaltap.reward.model.RewardStatus;

import java.util.UUID;

public record RewardResponse(
        UUID id,
        UUID businessId,
        String userId,
        String name,
        String description,
        int requiredPoints,
        Integer stockQuantity,
        int stockReserved,
        int stockRedeemed,
        Integer availableStock,
        RewardStatus status
) {
}
