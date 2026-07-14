package com.loyaltap.stamprequest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmployeeStampRequestAction(@NotNull UUID employeeId) {
}
