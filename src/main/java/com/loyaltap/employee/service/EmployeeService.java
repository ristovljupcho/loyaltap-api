package com.loyaltap.employee.service;

import com.loyaltap.employee.dto.EmployeeResponse;
import com.loyaltap.employee.dto.CreateEmployeeRequest;
import com.loyaltap.employee.dto.UpdateEmployeeRequest;

import java.util.List;
import java.util.UUID;

public interface EmployeeService {

    EmployeeResponse createEmployee(UUID businessId, CreateEmployeeRequest request);

    List<EmployeeResponse> listEmployees(UUID businessId);

    EmployeeResponse getEmployee(UUID businessId, UUID employeeId);

    EmployeeResponse updateEmployee(
            UUID businessId,
            UUID employeeId,
            UpdateEmployeeRequest request
    );

    void removeEmployee(UUID businessId, UUID employeeId);
}
