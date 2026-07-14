package com.loyaltap.stamprequest.dto;

import com.loyaltap.stamprequest.model.StampRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record StampRequestResponse(
        UUID id,
        UUID membershipId,
        UUID businessId,
        UUID nfcTagId,
        UUID approvedByEmployeeId,
        int pointsToAdd,
        StampRequestStatus status,
        String idempotencyKey,
        Instant createdAt,
        Instant expiresAt,
        Instant processedAt
) {
}
