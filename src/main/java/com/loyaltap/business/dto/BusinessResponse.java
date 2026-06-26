package com.loyaltap.business.dto;

import com.loyaltap.business.model.BusinessStatus;

import java.time.Instant;
import java.util.UUID;

public record BusinessResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String phone,
        String email,
        String websiteUrl,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        BusinessStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
