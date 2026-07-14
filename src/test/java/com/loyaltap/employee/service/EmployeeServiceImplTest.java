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
import com.loyaltap.employee.model.EmployeeRole;
import com.loyaltap.employee.model.EmployeeStatus;
import com.loyaltap.employee.repository.EmployeeRepository;
import com.loyaltap.user.model.User;
import com.loyaltap.user.model.UserStatus;
import com.loyaltap.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BusinessRepository businessRepository;
    @Mock
    private UserRepository userRepository;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(
                employeeRepository,
                businessRepository,
                userRepository,
                new EmployeeMapper()
        );
    }

    @Test
    void createsActiveEmployee() {
        UUID businessId = UUID.randomUUID();
        Business business = business(businessId, BusinessStatus.ACTIVE);
        User user = user("user-1", UserStatus.ACTIVE);
        when(businessRepository.findById(businessId)).thenReturn(Optional.of(business));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeResponse response = employeeService.createEmployee(
                businessId,
                new CreateEmployeeRequest(user.getId(), EmployeeRole.MANAGER)
        );

        assertEquals(businessId, response.businessId());
        assertEquals(user.getId(), response.userId());
        assertEquals(EmployeeRole.MANAGER, response.role());
        assertEquals(EmployeeStatus.ACTIVE, response.status());
    }

    @Test
    void createRejectsMissingOrInactiveBusiness() {
        UUID missingId = UUID.randomUUID();
        when(businessRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(
                missingId, new CreateEmployeeRequest("user-1", EmployeeRole.EMPLOYEE)));

        UUID inactiveId = UUID.randomUUID();
        when(businessRepository.findById(inactiveId)).thenReturn(
                Optional.of(business(inactiveId, BusinessStatus.INACTIVE)));
        assertThrows(InvalidRequestException.class, () -> employeeService.createEmployee(
                inactiveId, new CreateEmployeeRequest("user-1", EmployeeRole.EMPLOYEE)));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createRejectsMissingOrInactiveUserWithoutSaving() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        when(userRepository.findById("disabled")).thenReturn(Optional.of(user("disabled", UserStatus.DISABLED)));

        assertThrows(ResourceNotFoundException.class, () -> employeeService.createEmployee(
                businessId, new CreateEmployeeRequest("missing", EmployeeRole.EMPLOYEE)));
        assertThrows(InvalidRequestException.class, () -> employeeService.createEmployee(
                businessId, new CreateEmployeeRequest("disabled", EmployeeRole.EMPLOYEE)));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateAssignmentIncludingRemovedRecords() {
        UUID businessId = UUID.randomUUID();
        User user = user("user-1", UserStatus.ACTIVE);
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(employeeRepository.existsByBusinessIdAndUserId(businessId, user.getId())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> employeeService.createEmployee(
                businessId, new CreateEmployeeRequest(user.getId(), EmployeeRole.EMPLOYEE)));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void listsAllStatusesForInactiveBusiness() {
        UUID businessId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.INACTIVE)));
        when(employeeRepository.findAllByBusinessIdOrderByCreatedAtAsc(businessId)).thenReturn(List.of(
                employee(businessId, "user-1", EmployeeStatus.ACTIVE),
                employee(businessId, "user-2", EmployeeStatus.REMOVED)
        ));

        List<EmployeeResponse> responses = employeeService.listEmployees(businessId);

        assertEquals(List.of(EmployeeStatus.ACTIVE, EmployeeStatus.REMOVED),
                responses.stream().map(EmployeeResponse::status).toList());
    }

    @Test
    void getEmployeeIsScopedToBusiness() {
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(employeeRepository.findByIdAndBusinessId(employeeId, businessId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> employeeService.getEmployee(businessId, employeeId));
    }

    @Test
    void updateReactivatesRemovedEmployee() {
        UUID businessId = UUID.randomUUID();
        Employee employee = employee(businessId, "user-1", EmployeeStatus.REMOVED);
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.ACTIVE)));
        when(employeeRepository.findByIdAndBusinessId(employee.getId(), businessId)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.updateEmployee(
                businessId,
                employee.getId(),
                new UpdateEmployeeRequest(EmployeeRole.OWNER, EmployeeStatus.ACTIVE)
        );

        assertEquals(EmployeeRole.OWNER, response.role());
        assertEquals(EmployeeStatus.ACTIVE, response.status());
    }

    @Test
    void updateRejectsInactiveBusinessBeforeEmployeeLookup() {
        UUID businessId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.INACTIVE)));

        assertThrows(InvalidRequestException.class, () -> employeeService.updateEmployee(
                businessId,
                employeeId,
                new UpdateEmployeeRequest(EmployeeRole.EMPLOYEE, EmployeeStatus.ACTIVE)
        ));
        verify(employeeRepository, never()).findByIdAndBusinessId(any(), any());
    }

    @Test
    void removeEmployeeIsIdempotentAndAllowedForInactiveBusiness() {
        UUID businessId = UUID.randomUUID();
        Employee employee = employee(businessId, "user-1", EmployeeStatus.ACTIVE);
        when(businessRepository.findById(businessId)).thenReturn(
                Optional.of(business(businessId, BusinessStatus.INACTIVE)));
        when(employeeRepository.findByIdAndBusinessId(employee.getId(), businessId)).thenReturn(Optional.of(employee));

        employeeService.removeEmployee(businessId, employee.getId());
        employeeService.removeEmployee(businessId, employee.getId());

        assertEquals(EmployeeStatus.REMOVED, employee.getStatus());
    }

    private Business business(UUID id, BusinessStatus status) {
        Business business = new Business();
        business.setId(id);
        business.setStatus(status);
        return business;
    }

    private User user(String id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private Employee employee(UUID businessId, String userId, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(UUID.randomUUID());
        employee.setBusiness(business(businessId, BusinessStatus.ACTIVE));
        employee.setUser(user(userId, UserStatus.ACTIVE));
        employee.setRole(EmployeeRole.EMPLOYEE);
        employee.setStatus(status);
        return employee;
    }
}
