package com.loyaltap.stamprequest.dto;

import jakarta.validation.constraints.NotBlank;

public record UserStampRequestAction(@NotBlank String userId) {
}
