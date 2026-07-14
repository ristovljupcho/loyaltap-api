package com.loyaltap.employee.dto;

import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.model.EmployeeStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRequest(
        @NotNull EmployeeRole role,
        @NotNull EmployeeStatus status
) {
}
