package com.loyaltap.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateBusinessRequest(
        @Size(max = 160, message = "Business name must be at most 160 characters")
        String name,

        @Size(max = 120, message = "Business slug must be at most 120 characters")
        String slug,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Size(max = 50, message = "Phone must be at most 50 characters")
        String phone,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(max = 2048, message = "Website URL must be at most 2048 characters")
        String websiteUrl,

        @Size(max = 255, message = "Address line 1 must be at most 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must be at most 255 characters")
        String addressLine2,

        @Size(max = 120, message = "City must be at most 120 characters")
        String city,

        @Size(max = 120, message = "State must be at most 120 characters")
        String state,

        @Size(max = 40, message = "Postal code must be at most 40 characters")
        String postalCode,

        @Size(max = 120, message = "Country must be at most 120 characters")
        String country
) {
}
