package com.loyaltap.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BusinessRequest(
        @NotBlank String name,
        @NotBlank String slug,
        @NotBlank String description,
        @NotBlank String phone,
        @NotBlank @Email String email,
        @NotBlank String websiteUrl,
        @NotBlank String address,
        @NotBlank String city
) {
}
