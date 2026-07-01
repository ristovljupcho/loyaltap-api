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
        String address,
        String city,
        BusinessStatus status
) {
}
