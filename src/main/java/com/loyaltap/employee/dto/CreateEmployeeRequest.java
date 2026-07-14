package com.loyaltap.employee.dto;

import com.loyaltap.employee.model.EmployeeRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRequest(
        @NotBlank String userId,
        @NotNull EmployeeRole role
) {
}
