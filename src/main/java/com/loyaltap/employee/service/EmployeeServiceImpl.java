package com.loyaltap.employee.service;

import com.loyaltap.business.model.Business;
import com.loyaltap.business.model.BusinessStatus;
import com.loyaltap.business.repository.BusinessRepository;
import com.loyaltap.common.error.DuplicateResourceException;
import com.loyaltap.common.error.InvalidRequestException;
import com.loyaltap.common.error.ResourceNotFoundException;
import com.loyaltap.employee.dto.EmployeeResponse;
import com.loyaltap.employee.dto.CreateEmployeeRequest;
import com.loyaltap.employee.dto.UpdateEmployeeRequest;
import com.loyaltap.employee.mapper.EmployeeMapper;
import com.loyaltap.employee.model.Employee;
import com.loyaltap.employee.model.EmployeeStatus;
import com.loyaltap.employee.repository.EmployeeRepository;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final EmployeeMapper employeeMapper;

    @Override
    @Transactional
    public EmployeeResponse createEmployee(UUID businessId, @Valid CreateEmployeeRequest request) {
        Business business = findActiveBusiness(businessId);
        User user = findActiveUser(request.userId());
        if (employeeRepository.existsByBusinessIdAndUserId(businessId, user.getId())) {
            throw new DuplicateResourceException(
                    "Employee already exists for business " + businessId + " and user " + user.getId()
            );
        }

        return employeeMapper.toResponse(
                employeeRepository.save(employeeMapper.toEntity(request, business, user))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees(UUID businessId) {
        findBusiness(businessId);
        return employeeRepository.findAllByBusinessIdOrderByCreatedAtAsc(businessId)
                .stream()
                .map(employeeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(UUID businessId, UUID employeeId) {
        findBusiness(businessId);
        return employeeMapper.toResponse(findEmployee(businessId, employeeId));
    }

    @Override
    @Transactional
    public EmployeeResponse updateEmployee(
            UUID businessId,
            UUID employeeId,
            @Valid UpdateEmployeeRequest request
    ) {
        findActiveBusiness(businessId);
        Employee employee = findEmployee(businessId, employeeId);
        employeeMapper.updateEntity(employee, request);
        return employeeMapper.toResponse(employee);
    }

    @Override
    @Transactional
    public void removeEmployee(UUID businessId, UUID employeeId) {
        findBusiness(businessId);
        Employee employee = findEmployee(businessId, employeeId);
        if (employee.getStatus() != EmployeeStatus.REMOVED) {
            employee.setStatus(EmployeeStatus.REMOVED);
        }
    }

    private Business findActiveBusiness(UUID businessId) {
        Business business = findBusiness(businessId);
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new InvalidRequestException("Business is not active: " + businessId);
        }
        return business;
    }

    private Business findBusiness(UUID businessId) {
        return businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + businessId));
    }

    private User findActiveUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRequestException("User is not active: " + userId);
        }
        return user;
    }

    private Employee findEmployee(UUID businessId, UUID employeeId) {
        return employeeRepository.findByIdAndBusinessId(employeeId, businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }
}
