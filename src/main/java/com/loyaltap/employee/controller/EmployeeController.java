package com.loyaltap.employee.controller;

import com.loyaltap.employee.dto.EmployeeResponse;
import com.loyaltap.employee.dto.CreateEmployeeRequest;
import com.loyaltap.employee.dto.UpdateEmployeeRequest;
import com.loyaltap.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/business/{businessId}/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @PathVariable UUID businessId,
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.createEmployee(businessId, request));
    }

    @GetMapping
    public List<EmployeeResponse> listEmployees(@PathVariable UUID businessId) {
        return employeeService.listEmployees(businessId);
    }

    @GetMapping("/{employeeId}")
    public EmployeeResponse getEmployee(
            @PathVariable UUID businessId,
            @PathVariable UUID employeeId
    ) {
        return employeeService.getEmployee(businessId, employeeId);
    }

    @PutMapping("/{employeeId}")
    public EmployeeResponse updateEmployee(
            @PathVariable UUID businessId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return employeeService.updateEmployee(businessId, employeeId, request);
    }

    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> removeEmployee(
            @PathVariable UUID businessId,
            @PathVariable UUID employeeId
    ) {
        employeeService.removeEmployee(businessId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
