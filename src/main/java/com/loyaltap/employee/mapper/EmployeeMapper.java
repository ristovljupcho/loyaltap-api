package com.loyaltap.employee.mapper;

import com.loyaltap.business.model.Business;
import com.loyaltap.employee.dto.EmployeeResponse;
import com.loyaltap.employee.dto.CreateEmployeeRequest;
import com.loyaltap.employee.dto.UpdateEmployeeRequest;
import com.loyaltap.employee.model.Employee;
import com.loyaltap.employee.model.EmployeeStatus;
import com.loyaltap.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(CreateEmployeeRequest request, Business business, User user) {
        Employee employee = new Employee();
        employee.setBusiness(business);
        employee.setUser(user);
        employee.setRole(request.role());
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }

    public void updateEntity(Employee employee, UpdateEmployeeRequest request) {
        employee.setRole(request.role());
        employee.setStatus(request.status());
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getBusiness().getId(),
                employee.getUser().getId(),
                employee.getRole(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
