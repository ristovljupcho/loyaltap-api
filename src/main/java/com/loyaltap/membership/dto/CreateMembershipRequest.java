package com.loyaltap.membership.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateMembershipRequest(
        @NotBlank String userId,
        @NotNull UUID businessId
) {
}
