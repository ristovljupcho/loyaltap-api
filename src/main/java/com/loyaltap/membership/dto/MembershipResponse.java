package com.loyaltap.membership.dto;

import com.loyaltap.membership.model.MembershipStatus;

import java.time.Instant;
import java.util.UUID;

public record MembershipResponse(
        UUID id,
        String userId,
        UUID businessId,
        int pointsBalance,
        int reservedPoints,
        int totalPointsEarned,
        int totalPointsSpent,
        int totalRewardsRedeemed,
        MembershipStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
