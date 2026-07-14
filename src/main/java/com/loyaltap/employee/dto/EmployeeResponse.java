package com.loyaltap.employee.dto;

import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.model.EmployeeStatus;

import java.time.Instant;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        UUID businessId,
        String userId,
        EmployeeRole role,
        EmployeeStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
