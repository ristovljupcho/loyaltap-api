package com.loyaltap.business.dto;

public record UpdateBusinessRequest(
        String name,
        String slug,
        String description,
        String phone,
        String email,
        String websiteUrl,
        String address,
        String city
) {
}
